package s3

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, AnonymousAWSCredentials}
import javax.inject.{Inject, Singleton}
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.alpakka.s3.{MemoryBufferType, Proxy, S3Settings}

import scala.concurrent.ExecutionContext

@Singleton
class S3FileDataStore @Inject()(actorSystem: ActorSystem, s3Conf: S3Configuration)(implicit ec: ExecutionContext) {

  lazy val s3Client = {
    val awsCredentials = new AWSStaticCredentialsProvider(
      new AnonymousAWSCredentials()
    )
    val proxy = Option(Proxy("localhost", 8001, "http"))
    val settings = new S3Settings(MemoryBufferType, proxy, awsCredentials, "us-west-2", true)
    implicit val mat = ActorMaterializer()(actorSystem)
    new S3Client(settings)(actorSystem, mat)
  }

  def store(tenant: String, orgKey: String, userId: String, taskId: String, appId: String, name: String) = {
    val key = s"$tenant/$orgKey/$userId/$taskId/$appId/$name"
    s3Client.multipartUpload(s3Conf.bucketName, key)
  }

}
