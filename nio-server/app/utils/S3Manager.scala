package utils

import java.util.Base64

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.PutObjectResult
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
                        content: String)(
      implicit s3ExecutionContext: S3ExecutionContext): Future[PutObjectResult]
}

class S3Manager(env: Env) extends FSManager with FSUserExtractManager {

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
          amazonS3.putObject(env.config.s3Config.bucketName,
                             Base64.getEncoder.encodeToString(key.getBytes),
                             content)
        }
      }
  }

  override def userExtractUpload(
      tenant: String,
      orgKey: String,
      userId: String,
      extractTaskId: String,
      content: String)(implicit s3ExecutionContext: S3ExecutionContext)
    : Future[PutObjectResult] = {
    val key: String = s"/$tenant/$orgKey/$userId/extract/$extractTaskId"

    addFile(key, content)
  }
}

case class S3ExecutionContext(ec: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = ec.execute(runnable)

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)
}
