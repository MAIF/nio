package controllers.postgres

import models.{DeletionTaskStatus, EventType}
import play.api.libs.json.{JsArray, JsNull, Json}
import play.api.test.Helpers._
import utils.{TestUtilsMongo, TestUtilsPostgres}

class DeletionControllerSpec extends TestUtilsPostgres {
  val orgKey = "orgTest1"
  val userId = "toto"

  "DeletionController" should {
    var deletionTaskId = ""

    "start deletion task and check for kafka event" in {
      val inputJson = Json.obj("appIds" -> Seq("app1", "app2"))
      val startResp =
        postJson(
          s"/$tenant/organisations/$orgKey/users/$userId/deletion/_start",
          inputJson)

      startResp.status mustBe CREATED

      deletionTaskId = (startResp.json \ "id").as[String]

      val messages = readLastNKafkaEvents(2)
      messages.length mustBe 2

      val msg1AsJson = messages(0)
      (msg1AsJson \ "type").as[String] mustBe EventType.DeletionStarted.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe "app1"

      val msg2AsJson = messages(1)
      (msg2AsJson \ "type").as[String] mustBe EventType.DeletionStarted.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe "app2"
    }

    "find a deletion task by id" in {
      val resp = getJson(
        s"/$tenant/organisations/$orgKey/users/deletions/$deletionTaskId")
      resp.status mustBe OK

      (resp.json \ "id").as[String] mustBe deletionTaskId
    }

    "show all deletion tasks" in {
      val resp = getJson(s"/$tenant/organisations/$orgKey/users/deletions")
      resp.status mustBe OK

      (resp.json \ "count").as[Int] mustBe 1

      val value: JsArray = (resp.json \ "items").as[JsArray]
      value.value.size mustBe 1
    }

    "update deletion task with a appId done" in {
      val app1DoneResp = postJson(
        s"/$tenant/organisations/$orgKey/users/deletions/$deletionTaskId/apps/app1/_done",
        JsNull)
      app1DoneResp.status mustBe OK

      val msg1AsJson = readLastKafkaEvent()
      (msg1AsJson \ "type").as[String] mustBe EventType.DeletionAppDone.toString
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe "app1"

      val app2DoneResp = postJson(
        s"/$tenant/organisations/$orgKey/users/deletions/$deletionTaskId/apps/app2/_done",
        JsNull)
      app2DoneResp.status mustBe OK

      val messages = readLastNKafkaEvents(2)
      messages.length mustBe 2

      val msg2AsJson = messages(0)
      (msg2AsJson \ "type").as[String] mustBe EventType.DeletionAppDone.toString
      (msg2AsJson \ "payload" \ "appId").as[String] mustBe "app2"

      val msg3AsJson = messages(1)
      (msg3AsJson \ "type").as[String] mustBe EventType.DeletionFinished
        .toString
      (msg3AsJson \ "payload" \ "status")
        .as[String] mustBe DeletionTaskStatus.Done.toString
    }
  }
}
