package utils

import java.io.{ByteArrayInputStream, FileInputStream}
import java.util.Base64

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import configuration.{Env, S3Config}

import scala.concurrent.{ExecutionContext, Future}

trait FSManager {
  def addFile(key: String, content: String)(
      implicit s3ExecutionContext: S3ExecutionContext): Future[PutObjectResult]
}

trait FSUserExtractManager {
  def userExtractUpload(tenant: String,
                        orgKey: String,
                        userId: String,
                        extractTaskId: String,
                        name: String,
                        src: Source[ByteString, _])(
      implicit s3ExecutionContext: S3ExecutionContext): Future[String]
}

class S3Manager(env: Env, actorSystem: ActorSystem)
    extends FSManager
    with FSUserExtractManager {

  implicit val mat = ActorMaterializer()(actorSystem)
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
      new AwsClientBuilder.EndpointConfiguration(s3Config.endpoint,
                                                 Regions.DEFAULT_REGION.getName)
    )
    .build()

  private def isBucketExist(bucketName: String)(
      implicit s3ExecutionContext: S3ExecutionContext): Future[Boolean] = {
    Future {
      amazonS3.doesBucketExistV2(bucketName)
    }
  }

  private def createBucket(bucketName: String)(
      implicit s3ExecutionContext: S3ExecutionContext): Future[Boolean] = {
    isBucketExist(bucketName)
      .flatMap {
        case e if !e =>
          Future {
            amazonS3.createBucket(bucketName)
          }.map(
            _ => e
          )
        case e =>
          Future {
            e
          }
      }
  }

  def addFile(key: String, content: String)(
      implicit s3ExecutionContext: S3ExecutionContext)
    : Future[PutObjectResult] = {
    createBucket(env.config.s3Config.bucketName)
      .flatMap { _ =>
        Future {
          amazonS3.putObject(s3Config.bucketName,
                             Base64.getEncoder.encodeToString(key.getBytes),
                             content)
        }
      }
  }

  override def userExtractUpload(tenant: String,
                                 orgKey: String,
                                 userId: String,
                                 extractTaskId: String,
                                 name: String,
                                 src: Source[ByteString, _])(
      implicit s3ExecutionContext: S3ExecutionContext): Future[String] = {
    val bucketName = s3Config.bucketName

    // Create bucket if not exist
    createBucket(bucketName)
      .flatMap { _ =>
        val key: String =
          s"$tenant/$orgKey/$userId/extract/$extractTaskId/$name"

        val s3Request: InitiateMultipartUploadRequest =
          new InitiateMultipartUploadRequest(bucketName, key)
        val result: InitiateMultipartUploadResult =
          amazonS3.initiateMultipartUpload(s3Request)
        val uploadId: String = result.getUploadId

        src
          .via(loadFile(key, uploadId))
          .runFold(Seq[UploadPartResult]()) { (results, uploadResult) =>
            results :+ uploadResult
          }
          .map { results =>
            val tags = scala.collection.JavaConverters
              .seqAsJavaList(results.map(_.getPartETag))
            val completeRequest =
              new CompleteMultipartUploadRequest(bucketName,
                                                 key,
                                                 uploadId,
                                                 tags)
            amazonS3.completeMultipartUpload(completeRequest).getLocation
          }
      }
  }

  def loadFile(key: String, uploadId: String)(
      implicit s3ExecutionContext: S3ExecutionContext)
    : Flow[ByteString, UploadPartResult, NotUsed] =
    Flow[ByteString]
      .grouped(5)
      .statefulMapConcat { () =>
        var partNumber = 1
        chunks =>
          {
            val groupedChunk = chunks.reduce(_ ++ _)
            val uploadPart = new UploadPartRequest()
              .withBucketName(s3Config.bucketName)
              .withKey(key)
              .withUploadId(uploadId)
              .withPartNumber(partNumber)
              .withPartSize(groupedChunk.size)
              .withInputStream(new ByteArrayInputStream(
                groupedChunk.toArray[Byte]))

            partNumber += 1
            List(amazonS3.uploadPart(uploadPart))
          }
      }
}

case class S3ExecutionContext(ec: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = ec.execute(runnable)

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)
}
