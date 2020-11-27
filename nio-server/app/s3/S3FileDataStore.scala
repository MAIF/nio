package s3

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{AccessStyle, MemoryBufferType, S3Attributes, S3Ext, S3Settings}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.AwsRegionProvider

import scala.concurrent.ExecutionContext

class S3FileDataStore(actorSystem: ActorSystem, conf: S3Configuration)(implicit ec: ExecutionContext) {

  lazy val s3Settings: S3Settings =
    S3Ext(actorSystem).settings
      .withBufferType(MemoryBufferType)
      .withEndpointUrl(conf.endpoint)
      .withS3RegionProvider(new AwsRegionProvider {
        override def getRegion: Region = Region.of(conf.region)
      })
      .withCredentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(conf.access, conf.secret)
        )
      )
      .withAccessStyle(AccessStyle.pathAccessStyle)

  def store(tenant: String, orgKey: String, userId: String, taskId: String, appId: String, name: String) = {
    val key = s"$tenant/$orgKey/$userId/$taskId/$appId/$name"
    S3.multipartUpload(conf.bucketName, key)
      .withAttributes(S3Attributes.settings(s3Settings))
  }

}
