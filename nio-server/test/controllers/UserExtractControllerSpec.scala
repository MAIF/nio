package controllers

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.model.Multipart.BodyPart
import org.apache.pekko.stream.scaladsl.{FileIO, Source}
import models.{Organisation, Permission, PermissionGroup, UserExtract}
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws.SourceBody
import utils.NioLogger
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import play.api.test.Helpers._
import play.mvc.BodyParser.MultipartFormData
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.StringPart
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.part.StringMultipartPart
import utils.TestUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UserExtractControllerSpec extends TestUtils {

  val orgKey: String    = "org1"
  val org2Key: String   = "org2"
  val userId: String    = "user1"
  val userIdXml: String = "user1Xml"

  val org1 = Organisation(
    key = orgKey,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val org2 = Organisation(
    key = org2Key,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val userExtract = UserExtract("user@nio.fr")

  "UserExtractController JSON" should {

    "ask an extraction" in {
      val path: String   = s"/$tenant/organisations"
      val createResponse = postJson(path, org1.asJson())
      createResponse.status mustBe CREATED

      val userExtractStatus =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract", userExtract.asJson())
      userExtractStatus.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskAsked"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId
    }

    "ask an extraction with an existing orgKey/userId" in {
      val userExtractStatusConflict =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract", userExtract.asJson())
      userExtractStatusConflict.status mustBe CONFLICT
    }

    "ask an extraction for an unknow organisation" in {
      val userExtractStatus =
        postJson(s"/$tenant/organisations/org2/users/$userId/_extract", userExtract.asJson())
      userExtractStatus.status mustBe NOT_FOUND
    }

    "list all extracted data for an organisation" in {

      val path: String   = s"/$tenant/organisations"
      val createResponse = postJson(path, org2.asJson())
      createResponse.status mustBe CREATED

      postJson(s"/$tenant/organisations/$org2Key/users/$userId/_extract", userExtract.asJson()).status mustBe OK

      val userExtractStatus =
        postJson(s"/$tenant/organisations/$orgKey/users/userId2/_extract", userExtract.asJson())
      userExtractStatus.status mustBe OK

      val userExtractStatus1 =
        postJson(s"/$tenant/organisations/$orgKey/users/userId3/_extract", userExtract.asJson())
      userExtractStatus1.status mustBe OK

      val userExtracted = getJson(s"/$tenant/organisations/$orgKey/_extracted")
      userExtracted.status mustBe OK

      val extracted: JsValue = userExtracted.json

      (extracted \ "page").as[Int] mustBe 0
      (extracted \ "pageSize").as[Int] mustBe 10
      (extracted \ "count").as[Int] mustBe 3
      val items: JsArray = (extracted \ "items").as[JsArray]
      items.value.size mustBe 3
      (items \ 0 \ "userId").as[String] mustBe "user1"
      (items \ 0 \ "uploadStartedAt").asOpt[String] mustBe None
      (items \ 0 \ "endedAt").asOpt[String] mustBe None

      (items \ 1 \ "userId").as[String] mustBe "userId2"
      (items \ 2 \ "userId").as[String] mustBe "userId3"
    }

    "list all extracted data for a given user" in {
      val userExtracted =
        getJson(s"/$tenant/organisations/$orgKey/users/user1/_extracted")
      userExtracted.status mustBe OK

      val extracted: JsValue = userExtracted.json

      (extracted \ "page").as[Int] mustBe 0
      (extracted \ "pageSize").as[Int] mustBe 10
      (extracted \ "count").as[Int] mustBe 1
      val items: JsArray = (extracted \ "items").as[JsArray]
      items.value.size mustBe 1
      (items \ 0 \ "userId").as[String] mustBe "user1"
    }

    "list all extracted data for an unknow organisation" in {
      val userExtracted =
        getJson(s"/$tenant/organisations/orgUnknow/_extracted")
      userExtracted.status mustBe NOT_FOUND
    }

    "upload file" in {

      val file = File.createTempFile("file", ".json")
      Files.write(file.toPath, """{ "key": "value" }""".getBytes(StandardCharsets.UTF_8))

      val resp = Await.result(
        ws.url(s"$serverHost/api/$tenant/organisations/$orgKey/users/$userId/_files/${file.getName}")
          .withBody(
            Source.single(FilePart("file", file.getName, Some("application/json"), FileIO.fromPath(file.toPath)))
          )
          .execute("POST"),
        Duration(60, TimeUnit.SECONDS)
      )

      resp.status mustBe OK

      Await
        .result(
          ws.url(s"$serverHost/api/$tenant/organisations/$orgKey/users/$userId/_files/${file.getName}")
            .withBody(
              Source.single(FilePart("file", file.getName, Some("application/json"), FileIO.fromPath(file.toPath)))
            )
            .execute("POST"),
          Duration(60, TimeUnit.SECONDS)
        )
        .status mustBe NOT_FOUND

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskCompleted"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId

      // ask extract after file upload must be OK
      val userExtractStatus =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract", userExtract.asJson())
      userExtractStatus.status mustBe OK

      val file2 = File.createTempFile("file2", ".csv")
      Files.write(file2.toPath, """val1; val2; val3 \n val4; val5; val6 \n""".getBytes(StandardCharsets.UTF_8))

      val chunks2 = FileIO.fromPath(file2.toPath)

      Await
        .result(
          ws.url(s"$serverHost/api/$tenant/organisations/$orgKey/users/$userId/_files/${file2.getName}")
            .withBody(
              Source.single(FilePart("file", file2.getName, Some("application/json"), FileIO.fromPath(file2.toPath)))
            )
            .execute("POST"),
          Duration(60, TimeUnit.SECONDS)
        )
        .status mustBe OK

      val userExtracted =
        getJson(s"/$tenant/organisations/$orgKey/users/$userId/_extracted")
      userExtracted.status mustBe OK

      val extracted: JsValue = userExtracted.json

      (extracted \ "page").as[Int] mustBe 0
      (extracted \ "pageSize").as[Int] mustBe 10
      (extracted \ "count").as[Int] mustBe 2
      val items: JsArray = (extracted \ "items").as[JsArray]
      items.value.size mustBe 2

      (items \ 0 \ "userId").as[String] mustBe "user1"
      (items \ 0 \ "orgKey").as[String] mustBe orgKey

      (items \ 1 \ "userId").as[String] mustBe "user1"
      (items \ 1 \ "orgKey").as[String] mustBe orgKey
    }
  }

  "UserExtractController XML" should {

    "ask an extraction" in {
      val userExtractStatus =
        postXml(s"/$tenant/organisations/$orgKey/users/$userIdXml/_extract", userExtract.asXml())
      userExtractStatus.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskAsked"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userIdXml
    }

    "ask an extraction with an existing orgKey/userId" in {
      val userExtractStatusConflict =
        postXml(s"/$tenant/organisations/$orgKey/users/$userIdXml/_extract", userExtract.asXml())
      userExtractStatusConflict.status mustBe CONFLICT
    }

    "ask an extraction for an unknow organisation" in {
      val userExtractStatus =
        postXml(s"/$tenant/organisations/orgUnknow/users/$userIdXml/_extract", userExtract.asXml())
      userExtractStatus.status mustBe NOT_FOUND
    }
  }

}
