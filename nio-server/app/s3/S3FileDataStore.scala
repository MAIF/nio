package s3

import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import javax.inject.{Inject, Singleton}
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, Proxy, S3Settings}

import scala.concurrent.ExecutionContext

@Singleton
class S3FileDataStore @Inject()(actorSystem: ActorSystem, conf: S3Configuration)(
    implicit ec: ExecutionContext) {

  lazy val s3Client = {
    val awsCredentials = new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(conf.access, conf.secret))
    val url = new URL(conf.endpoint)
    val proxy = Option(Proxy(url.getHost, url.getPort, url.getProtocol))
    val settings =
      new S3Settings(MemoryBufferType, proxy, awsCredentials, conf.region, true)
    implicit val mat = ActorMaterializer()(actorSystem)
    new S3Client(settings)(actorSystem, mat)
  }

  def store(tenant: String,
            orgKey: String,
            userId: String,
            taskId: String,
            appId: String,
            name: String) = {
    val key = s"$tenant/$orgKey/$userId/$taskId/$appId/$name"
    s3Client.multipartUpload(conf.bucketName, key)
  }

}
