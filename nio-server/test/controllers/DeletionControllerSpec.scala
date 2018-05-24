package controllers

import models.{AppDeletionState, DeletionTaskStatus}
import net.manub.embeddedkafka.EmbeddedKafka
import play.api.libs.json.{JsArray, JsNull, Json}
import utils.TestUtils
import play.api.test.Helpers._

class DeletionControllerSpec extends TestUtils {

  val tenant: String = "sandbox"
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

      val msg1 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg1AsJson = Json.parse(msg1)
      (msg1AsJson \ "type").as[String] mustBe "DeletionTaskStarted"
      (msg1AsJson \ "payload" \ "appId").as[String] mustBe "app1"

      val msg2 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg2AsJson = Json.parse(msg2)
      (msg2AsJson \ "type").as[String] mustBe "DeletionTaskStarted"
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

      println("tasks -> " + resp.json)

      (resp.json \ "count").as[Int] mustBe 1

      val value: JsArray = (resp.json \ "items").as[JsArray]
      value.value.size mustBe 1
    }

    "update deletion task with a appId done" in {
      val app1DoneResp = postJson(
        s"/$tenant/organisations/$orgKey/users/deletions/$deletionTaskId/apps/app1/_done",
        JsNull)
      app1DoneResp.status mustBe OK

      val msg1 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg1AsJson = Json.parse(msg1)
      (msg1AsJson \ "type").as[String] mustBe "DeletionTaskUpdated"
      val appStates1 = (msg1AsJson \ "payload" \ "states")
        .as[JsArray]
        .value
        .flatMap(el => AppDeletionState.appDeletionStateFormats.reads(el).asOpt)

      appStates1.exists(state =>
        state.appId == "app1" && state.status == DeletionTaskStatus.Done) mustBe true
      appStates1.exists(state =>
        state.appId == "app2" && state.status == DeletionTaskStatus.Running) mustBe true

      val app2DoneResp = postJson(
        s"/$tenant/organisations/$orgKey/users/deletions/$deletionTaskId/apps/app2/_done",
        JsNull)
      app2DoneResp.status mustBe OK

      val msg2 = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msg2AsJson = Json.parse(msg2)
      (msg2AsJson \ "type").as[String] mustBe "DeletionTaskDone"
      val appStates2 = (msg2AsJson \ "payload" \ "states")
        .as[JsArray]
        .value
        .flatMap(el => AppDeletionState.appDeletionStateFormats.reads(el).asOpt)

      appStates2.exists(state =>
        state.appId == "app1" && state.status == DeletionTaskStatus.Done) mustBe true
      appStates2.exists(state =>
        state.appId == "app2" && state.status == DeletionTaskStatus.Done) mustBe true
    }
  }
}
