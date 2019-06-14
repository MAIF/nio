package controllers.mongo

import java.util.concurrent.TimeUnit

import db.ApiKeyDataStore
import play.api.Logger
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TestUtilsMongo

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApiKeyControllerSpec extends TestUtilsMongo {

  private val apiKey: JsValue = Json.obj(
    "clientId" -> "clientId1",
    "clientSecret" -> "admin",
    "offerRestrictionPatterns" -> Json.arr(JsString("*"))
  )

  private val apiKey2: JsValue = Json.obj(
    "clientId" -> "clientId2",
    "clientSecret" -> "admin",
    "offerRestrictionPatterns" -> Json.arr(JsString("*"))
  )

  "ApiKeyController" should {

    "clean" in {
      val store: ApiKeyDataStore =
        nioComponents.apiKeyDataStore
      Await.result(store.deleteAll(), Duration(10, TimeUnit.SECONDS))
    }

    "create api key" in {
      val response: WSResponse =
        postJson("/apikeys", apiKey)

      Logger.info(response.body)
      response.status mustBe CREATED

      val value: JsValue = response.json

      (value \ "clientId").as[String] mustBe "clientId1"
      (value \ "clientSecret").as[String] mustBe "admin"
      (value \ "offerRestrictionPatterns").as[Seq[String]] mustBe Seq("*")

      val id: String = (value \ "_id").as[String]

      val getResponse: WSResponse = getJson(s"/apikeys/$id")

      getResponse.status mustBe OK

      val getValue: JsValue = getResponse.json

      (getValue \ "clientId").as[String] mustBe "clientId1"
      (getValue \ "clientSecret").as[String] mustBe "admin"
      (getValue \ "offerRestrictionPatterns").as[Seq[String]] mustBe Seq("*")
    }

    "create api key with clientId conflict" in {
      val response: WSResponse =
        postJson("/apikeys", apiKey)

      response.status mustBe CONFLICT

      (response.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "error.api.key.client.id.already.used"
    }

    "get api keys" in {
      postJson("/apikeys", apiKey2).status mustBe CREATED

      val getResponse: WSResponse = getJson("/apikeys")

      getResponse.status mustBe OK

      val value: JsValue = getResponse.json

      (value \ "count").as[Int] mustBe 2
      (value \ "page").as[Int] mustBe 0
      (value \ "pageSize").as[Int] mustBe 10

      val items: JsArray = (value \ "items").as[JsArray]

      items.value.size mustBe 2

      (items \ 0 \ "clientId").as[String] mustBe "clientId2"
      (items \ 0 \ "clientSecret").as[String] mustBe "admin"
      (items \ 0 \ "offerRestrictionPatterns").as[Seq[String]] mustBe Seq("*")

      (items \ 1 \ "clientId").as[String] mustBe "clientId1"
      (items \ 1 \ "clientSecret").as[String] mustBe "admin"
      (items \ 1 \ "offerRestrictionPatterns").as[Seq[String]] mustBe Seq("*")
    }

    "update api key" in {

      val apiKey: JsValue = Json.obj(
        "clientId" -> "clientId3",
        "clientSecret" -> "admin",
        "offerRestrictionPatterns" -> Json.arr(JsString("*"))
      )

      val createResponse: WSResponse = postJson("/apikeys", apiKey)
      createResponse.status mustBe CREATED

      val id: String = (createResponse.json \ "_id").as[String]

      val apiKeyUpdated: JsValue = Json.obj(
        "_id" -> id,
        "clientId" -> "clientId3",
        "clientSecret" -> "admin123",
        "offerRestrictionPatterns" -> Json.arr(JsString("test"))
      )

      val updateResponse: WSResponse =
        putJson(s"/apikeys/$id", apiKeyUpdated)

      updateResponse.status mustBe OK

      val value: JsValue = updateResponse.json

      (value \ "clientId").as[String] mustBe "clientId3"
      (value \ "clientSecret").as[String] mustBe "admin"
      (value \ "offerRestrictionPatterns").as[Seq[String]] mustBe Seq("test")

      val getResponse: WSResponse = getJson(s"/apikeys/$id")

      getResponse.status mustBe OK

      val getValue: JsValue = getResponse.json

      (getValue \ "clientId").as[String] mustBe "clientId3"
      (getValue \ "clientSecret").as[String] mustBe "admin"
      (getValue \ "offerRestrictionPatterns").as[Seq[String]] mustBe Seq("test")
    }

    "delete api key" in {
      val getResponse: WSResponse = getJson("/apikeys")

      getResponse.status mustBe OK

      val value: JsValue = getResponse.json

      (value \ "count").as[Int] mustBe 3
      (value \ "page").as[Int] mustBe 0
      (value \ "pageSize").as[Int] mustBe 10

      val items: JsArray = (value \ "items").as[JsArray]

      val id: String = (items \ 0 \ "_id").as[String]

      val deleteResponse: WSResponse = delete(s"/apikeys/$id")
      deleteResponse.status mustBe OK

      getJson(s"/apikeys/$id").status mustBe NOT_FOUND
    }
  }
}
