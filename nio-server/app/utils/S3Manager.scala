package utils

import java.io.{ByteArrayInputStream, File, FileInputStream, InputStream}
import java.nio.file.Files
import java.util
import java.util.Base64

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{IOResult, Materializer}
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.model.lifecycle.{
  LifecycleAndOperator,
  LifecycleFilter,
  LifecyclePrefixPredicate,
  LifecycleTagPredicate
}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import configuration.{Env, S3Config}

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

trait FSManager {
  def addFile(key: String, content: String)(implicit s3ExecutionContext: S3ExecutionContext): Future[PutObjectResult]
}

trait FSUserExtractManager {
  def userExtractUpload(
      tenant: String,
      orgKey: String,
      userId: String,
      extractTaskId: String,
      name: String,
      src: Source[ByteString, _]
  )(implicit s3ExecutionContext: S3ExecutionContext): Future[String]

  def getUploadedFile(tenant: String, orgKey: String, userId: String, extractTaskId: String, name: String)(implicit
      s3ExecutionContext: S3ExecutionContext
  ): Future[Source[ByteString, Future[IOResult]]]
}

class S3Manager(env: Env, actorSystem: ActorSystem) extends FSManager with FSUserExtractManager {

  implicit val mat                    = Materializer(actorSystem)
  private lazy val s3Config: S3Config = env.config.s3Config

  private lazy val amazonS3: AmazonS3 = AmazonS3ClientBuilder
    .standard()
    .withClientConfiguration(
      new ClientConfiguration()
        .withMaxErrorRetry(0)
        .withConnectionTimeout(1000)
        .withSignerOverride("S3SignerType")
    )
    .withCredentials(
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(s3Config.accessKey, s3Config.secretKey)
      )
    )
    .withPathStyleAccessEnabled(true)
    .withEndpointConfiguration(
      new AwsClientBuilder.EndpointConfiguration(s3Config.endpoint, s3Config.region)
    )
    .build()

  private def isBucketExist(bucketName: String)(implicit s3ExecutionContext: S3ExecutionContext): Future[Boolean] =
    Future {
      amazonS3.doesBucketExistV2(bucketName)
    }

  private def createBucket(bucketName: String, public: Boolean = false)(implicit
      s3ExecutionContext: S3ExecutionContext
  ): Future[Boolean] =
    isBucketExist(bucketName)
      .flatMap {
        case e if !e =>
          Future {
            amazonS3.createBucket(bucketName)
          }.map(_ => e)
        case e       =>
          Future {
            e
          }
      }

  def addFile(key: String, content: String)(implicit s3ExecutionContext: S3ExecutionContext): Future[PutObjectResult] =
    createBucket(s3Config.bucketName)
      .flatMap { _ =>
        Future {
          amazonS3.putObject(s3Config.bucketName, Base64.getEncoder.encodeToString(key.getBytes), content)
        }
      }

  def getKey(tenant: String, orgKey: String, userId: String, extractTaskId: String, name: String): String =
    s"$tenant/$orgKey/$userId/extract/$extractTaskId/${name.replaceAll("-", "_").replaceAll(" ", "_")}"

  def getUploadedFile(tenant: String, orgKey: String, userId: String, extractTaskId: String, name: String)(implicit
      s3ExecutionContext: S3ExecutionContext
  ): Future[Source[ByteString, Future[IOResult]]] = {

    val key: String = getKey(tenant, orgKey, userId, extractTaskId, name)

    Future {
      amazonS3.getObject(s3Config.uploadBucketName, key)
    }.map { s3Object =>
      val in: InputStream = s3Object.getObjectContent
      StreamConverters.fromInputStream(() => in, s3Config.chunkSizeInMb)

    }
  }

  def bucketExpiration(bucketName: String, prefixKey: String)(implicit
      s3ExecutionContext: S3ExecutionContext
  ): Future[Boolean] = {
    // https://docs.aws.amazon.com/fr_fr/AmazonS3/latest/dev/manage-lifecycle-using-java.html
    val rule: BucketLifecycleConfiguration.Rule =
      new BucketLifecycleConfiguration.Rule()
        .withId(s"$bucketName-ttl")
        .withFilter(
          new LifecycleFilter(
            new LifecyclePrefixPredicate(prefixKey)
          )
        )
        .withExpirationInDays(s3Config.expireAtInDay)
        .withStatus(BucketLifecycleConfiguration.ENABLED)

    Future {
      val configuration = amazonS3.getBucketLifecycleConfiguration(bucketName)
      configuration.getRules.add(rule)
      amazonS3.setBucketLifecycleConfiguration(bucketName, configuration)
    }(s3ExecutionContext).map(_ => true)
  }

  override def userExtractUpload(
      tenant: String,
      orgKey: String,
      userId: String,
      extractTaskId: String,
      name: String,
      src: Source[ByteString, _]
  )(implicit s3ExecutionContext: S3ExecutionContext): Future[String] = {
    val bucketName = s3Config.uploadBucketName

    val key: String = getKey(tenant, orgKey, userId, extractTaskId, name)

    // Create bucket if not exist
    createBucket(bucketName)
      .map { _ =>
        val prefixKey = s"$tenant/$orgKey/$userId/extract/$extractTaskId/"
        bucketExpiration(bucketName, prefixKey)
      }
      .flatMap { _ =>
        val s3Request: InitiateMultipartUploadRequest =
          new InitiateMultipartUploadRequest(bucketName, key)
        val result: InitiateMultipartUploadResult     =
          amazonS3.initiateMultipartUpload(s3Request)
        val uploadId: String                          = result.getUploadId

        src
          .via(loadFile(key, uploadId))
          .runFold(Seq[UploadPartResult]()) { (results, uploadResult) =>
            results :+ uploadResult
          }
          .map { results =>
            val tags            = results.map(_.getPartETag).asJava
            val completeRequest =
              new CompleteMultipartUploadRequest(bucketName, key, uploadId, tags)
            val result          = amazonS3.completeMultipartUpload(completeRequest)

            result.getLocation
          }
      }
      .flatMap { _ =>
        Future {
          amazonS3.getUrl(bucketName, key)
        }.map(_.toExternalForm)
      }
  }

  def loadFile(key: String, uploadId: String)(implicit
      s3ExecutionContext: S3ExecutionContext
  ): Flow[ByteString, UploadPartResult, NotUsed] =
    Flow[ByteString]
      .grouped(s3Config.chunkSizeInMb)
      .statefulMapConcat { () =>
        var partNumber = 1
        chunks => {
          val groupedChunk = chunks.reduce(_ ++ _)
          val uploadPart   = new UploadPartRequest()
            .withBucketName(s3Config.uploadBucketName)
            .withKey(key)
            .withUploadId(uploadId)
            .withPartNumber(partNumber)
            .withPartSize(groupedChunk.size)
            .withInputStream(new ByteArrayInputStream(groupedChunk.toArray[Byte]))

          partNumber += 1
          List(amazonS3.uploadPart(uploadPart))
        }
      }
}

case class S3ExecutionContext(ec: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = ec.execute(runnable)

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)
}
