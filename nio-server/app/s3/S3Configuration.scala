package s3

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class S3Configuration @Inject()(val configuration: Configuration) {

  lazy val bucketName = configuration.get[String]("s3.bucket")

  lazy val chunkSizeInMb = 1024 * 1024 * configuration.get[Int]("s3.chunkSizeInMb")

  lazy val expirationInDays = configuration.get[Int]("s3.expirationInDays")

  lazy val endpoint = configuration.get[String]("s3.endpoint")

  lazy val region = configuration.get[String]("s3.region")

  lazy val access = configuration.get[String]("s3.access")

  lazy val secret = configuration.get[String]("s3.secret")

  lazy val v4auth = configuration.get[Boolean]("s3.v4auth")
}
