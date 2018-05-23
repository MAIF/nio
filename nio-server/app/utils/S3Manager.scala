package utils

import java.util.Base64

import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.StreamConverters
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{
  DeleteObjectsRequest,
  ObjectListing,
  PutObjectResult
}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import configuration.{Env, S3Config}
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class S3Manager @Inject()(env: Env) {

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
      new AwsClientBuilder.EndpointConfiguration(s3Config.host,
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
          amazonS3.putObject(env.config.s3Config.bucketName,
                             Base64.getEncoder.encodeToString(key.getBytes),
                             content)
        }
      }

  }

  def reads()(implicit mat: Materializer,
              s3ExecutionContext: S3ExecutionContext): Unit = {
    createBucket(s3Config.bucketName)
      .flatMap { _ =>
        Future {
          amazonS3.listObjects(s3Config.bucketName)
        }.map { listing =>
          Logger.info(
            s"objects in bucket : ${listing.getObjectSummaries.size()}")

          listing.getObjectSummaries.forEach(o => {
            Logger.info(s"key : ${o.getKey}")
          })
        }

      }

  }
}

case class S3ExecutionContext(ec: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = ec.execute(runnable)

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)
}
