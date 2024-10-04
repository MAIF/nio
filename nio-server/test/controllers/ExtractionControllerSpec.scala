package controllers

import models._
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import utils.TestUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import org.apache.pekko.stream.scaladsl.FileIO
import com.amazonaws.services.s3.AmazonS3
import play.api.libs.ws.SourceBody
import s3.{S3, S3Configuration}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ExtractionControllerSpec extends TestUtils {

  val orgKey = "orgTest1"
  val appId1 = "app1"
  val appId2 = "app2"
  val userId = "toto"

  "ExtractionController" should {
    var extractionTaskId = ""

    "start extraction task and check for kafka event" in {
      val inputJson = Json.obj("appIds" -> Seq(appId1, "app2"))
      val startResp =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/extraction/_start", inputJson)

      startResp.status mustBe CREATED

      extractionTaskId = (startResp.json \ "id").as[String]

      val messages = readLastNKafkaEvents(2)
      messages.length mustBe 2

      val msg1AsJson = messages(0)
      (msg1AsJson \ "type").as[String] mustBe EventType.ExtractionStarted.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe appId1

      val msg2AsJson = messages(1)
      (msg2AsJson \ "type").as[String] mustBe EventType.ExtractionStarted.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe "app2"
    }

    "find an extraction task by id" in {
      val resp = getJson(s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId")
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
      val inputJson = FilesMetadata(files = Seq(FileMetadata(fileApp1.getName, "json", fileApp1.length))).asJson()
      val resp      = postJson(
        s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId1/_setFilesMetadata",
        inputJson
      )

      resp.status mustBe OK

      val appStates = (resp.json \ "states")
        .as[JsArray]
        .value
        .flatMap(el => AppState.appStateFormats.reads(el).asOpt)

      appStates
        .find(_.appId == appId1)
        .get
        .files
        .exists(fm =>
          fm.name == fileApp1.getName &&
          fm.contentType == "json" &&
          fm.size == fileApp1.length()
        ) mustBe true

      val msg1AsJson = readLastKafkaEvent()
      (msg1AsJson \ "type")
        .as[String] mustBe EventType.ExtractionAppFilesMetadataReceived.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe appId1

      val inputJson2 = FilesMetadata(files = Seq(FileMetadata(fileApp2.getName, "json", fileApp2.length))).asJson()
      val resp2      = postJson(
        s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId2/_setFilesMetadata",
        inputJson2
      )

      resp2.status mustBe OK

      val msg2AsJson = readLastKafkaEvent()
      (msg2AsJson \ "type")
        .as[String] mustBe EventType.ExtractionAppFilesMetadataReceived.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe appId2
    }

    "upload a file during an extraction task" in {
      val s3Client = nioComponents.s3
      val s3Conf   = nioComponents.s3Configuration

      createBucketIfNotExist(s3Client.client, s3Conf.bucketName)

      val chunks = FileIO.fromPath(fileApp1.toPath)

      val resp = Await.result(
        ws.url(
          s"$serverHost/api/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId1/files/${fileApp1.getName}"
        ).withBody(SourceBody(chunks))
          .withMethod("POST")
          .execute(),
        Duration(60, TimeUnit.SECONDS)
      )

      resp.status mustBe OK

      val msg1AsJson = readLastKafkaEvent()
      (msg1AsJson \ "type").as[String] mustBe EventType.ExtractionAppDone.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe appId1

      val respBad = Await.result(
        ws.url(
          s"$serverHost/api/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/app3/files/${fileApp1.getName}"
        ).withBody(SourceBody(chunks))
          .withMethod("POST")
          .execute(),
        Duration(60, TimeUnit.SECONDS)
      )

      respBad.status mustBe NOT_FOUND

      val respTask = getJson(s"/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId")
      respTask.status mustBe OK

      val resp2 = Await.result(
        ws.url(
          s"$serverHost/api/$tenant/organisations/$orgKey/users/extractions/$extractionTaskId/apps/$appId2/files/${fileApp2.getName}"
        ).withBody(SourceBody(chunks))
          .withMethod("POST")
          .execute(),
        Duration(60, TimeUnit.SECONDS)
      )
      resp2.status mustBe OK

      val messages = readLastNKafkaEvents(2)
      messages.length mustBe 2

      val msg2AsJson = messages(0)
      (msg2AsJson \ "type").as[String] mustBe EventType.ExtractionAppDone.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe appId2

      val msg3AsJson = messages(1)
      (msg3AsJson \ "type")
        .as[String] mustBe EventType.ExtractionFinished.toString
    }
  }

  def createBucketIfNotExist(client: AmazonS3, bucketName: String) =
    if (!client.doesBucketExistV2(bucketName)) {
      client.createBucket(bucketName)

      client.putObject(bucketName, "bar", "baz")
    }
}
