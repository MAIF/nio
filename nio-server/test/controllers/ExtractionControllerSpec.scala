package controllers

import models._
import net.manub.embeddedkafka.EmbeddedKafka
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import utils.TestUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.FileIO
import com.amazonaws.auth.{AWSStaticCredentialsProvider, AnonymousAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import io.findify.s3mock.S3Mock
import play.api.libs.ws.SourceBody

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ExtractionControllerSpec extends TestUtils {

  val tenant: String = "sandbox"
  val orgKey = "orgTest1"
  val appId1 = "app1"
  val appId2 = "app2"
  val userId = "toto"

  "ExtractionController" should {
    var extractionTaskId = ""

    "start deletion task and check for kafka event" in {
      val inputJson = Json.obj("appIds" -> Seq(appId1, "app2"))
      val startResp =
        postJson(
          s"/$tenant/organisations/$orgKey/users/$userId/extraction/_start",
          inputJson)

      startResp.status mustBe CREATED

      extractionTaskId = (startResp.json \ "id").as[String]

      val msg1 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg1AsJson = Json.parse(msg1)
      (msg1AsJson \ "type").as[String] mustBe EventType.ExtractionStarted.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe appId1

      val msg2 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg2AsJson = Json.parse(msg2)
      (msg2AsJson \ "type").as[String] mustBe EventType.ExtractionStarted.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe "app2"
    }

    "find an extraction task by id" in {
      val resp = getJson(
        s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId")
      resp.status mustBe OK

      (resp.json \ "id").as[String] mustBe extractionTaskId
    }

    "show all extraction tasks" in {
      val resp = getJson(s"/$tenant/organisations/$orgKey/users/extractions")
      resp.status mustBe OK

      println("tasks -> " + resp.json)

      (resp.json \ "count").as[Int] mustBe 1

      val value: JsArray = (resp.json \ "items").as[JsArray]
      value.value.size mustBe 1
    }

    val fileApp1 = File.createTempFile("file_app1_", ".json")
    Files.write(fileApp1.toPath, """{ "key": "value" }""".getBytes(StandardCharsets.UTF_8))

    val fileApp2 = File.createTempFile("file_app2_", ".json")
    Files.write(fileApp2.toPath, """{ "key": "value" }""".getBytes(StandardCharsets.UTF_8))

    "set files metadata for a task" in {
      val inputJson = FilesMetadata(files = Seq(FileMetadata(fileApp1.getName,"json",fileApp1.length))).asJson
      val resp = postJson(s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId1/_setFilesMetadata", inputJson)

      resp.status mustBe OK

      val appStates = (resp.json \ "states")
        .as[JsArray]
        .value
        .flatMap(el => AppState.appStateFormats.reads(el).asOpt)

      appStates.find(_.appId == appId1).get.files.exists(fm =>
        fm.name == fileApp1.getName &&
        fm.contentType == "json" &&
        fm.size == fileApp1.length()) mustBe true

      val msg1 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg1AsJson = Json.parse(msg1)
      (msg1AsJson \ "type").as[String] mustBe EventType.ExtractionAppFilesMetadataReceived.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe appId1

      val inputJson2 = FilesMetadata(files = Seq(FileMetadata(fileApp2.getName,"json",fileApp2.length))).asJson
      val resp2 = postJson(s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId2/_setFilesMetadata", inputJson2)

      resp2.status mustBe OK

      val msg2 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg2AsJson = Json.parse(msg2)
      (msg2AsJson \ "type").as[String] mustBe EventType.ExtractionAppFilesMetadataReceived.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe appId2
    }

    "upload a file during an extraction task" in {

      /** Create and start S3 API mock. */
      val api = S3Mock(port = 8001, dir = "tmp")
      api.start

      /* AWS S3 client setup.
       *  withPathStyleAccessEnabled(true) trick is required to overcome S3 default
       *  DNS-based bucket access scheme
       *  resulting in attempts to connect to addresses like "bucketname.localhost"
       *  which requires specific DNS setup.
       */
      val endpoint =
        new EndpointConfiguration("http://localhost:8001", "us-west-2")
      val client = AmazonS3ClientBuilder.standard
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(endpoint)
        .withCredentials(
          new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
        .build

      createBucketIfNotExist(client, "nioevents")


      val chunks = FileIO.fromPath(fileApp1.toPath)

      val resp = Await.result(
        ws.url(s"$serverHost/api/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId1/files/${fileApp1.getName}")
        .withBody(SourceBody(chunks))
        .withMethod("POST")
        .execute(),
        Duration(60, TimeUnit.SECONDS)
      )

      resp.status mustBe OK

      val msg1 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg1AsJson = Json.parse(msg1)
      (msg1AsJson \ "type").as[String] mustBe EventType.ExtractionAppDone.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe appId1

      val respBad = Await.result(
        ws.url(s"$serverHost/api/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/app3/files/${fileApp1.getName}")
          .withBody(SourceBody(chunks))
          .withMethod("POST")
          .execute(),
        Duration(60, TimeUnit.SECONDS)
      )

      respBad.status mustBe NOT_FOUND

      val respTask = getJson(
        s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId")
      respTask.status mustBe OK
      println("-----> " + respTask.body)

      val resp2 = Await.result(
        ws.url(s"$serverHost/api/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId2/files/${fileApp2.getName}")
          .withBody(SourceBody(chunks))
          .withMethod("POST")
          .execute(),
        Duration(60, TimeUnit.SECONDS)
      )
      resp2.status mustBe OK

      println("---- > "+ resp2.body)

      val msg2 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg2AsJson = Json.parse(msg2)
      (msg2AsJson \ "type").as[String] mustBe EventType.ExtractionAppDone.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe appId2
      println("----> " + (msg2AsJson \ "payload"))

      val msg3 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg3AsJson = Json.parse(msg3)
      (msg3AsJson \ "type").as[String] mustBe EventType.ExtractionFinished.toString
      println("----> " + (msg3AsJson \ "payload"))
    }
  }

  def createBucketIfNotExist(client: AmazonS3, bucketName: String) = {
    if (!client.doesBucketExistV2(bucketName)) {
      client.createBucket(bucketName)

      client.putObject(bucketName, "bar", "baz")
    }
  }
}
