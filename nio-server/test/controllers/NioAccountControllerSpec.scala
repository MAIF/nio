package controllers

import java.util.concurrent.TimeUnit

import db.NioAccountMongoDataStore
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TestUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class NioAccountControllerSpec extends TestUtils {

  private val nioAccount: JsValue = Json.obj(
    "email" -> "admin@test.fr",
    "password" -> "admin",
    "isAdmin" -> true
  )

  private val nioAccount2: JsValue = Json.obj(
    "email" -> "admin2@test.fr",
    "password" -> "admin",
    "isAdmin" -> true
  )

  "NioAccountController" should {

    "clean" in {
      val store: NioAccountMongoDataStore =
        nioComponents.nioAccountMongoDataStore
      Await.result(store.deleteByQuery("", Json.obj()),
                   Duration(10, TimeUnit.SECONDS))
    }

    "create nio account" in {
      val response: WSResponse =
        postJson("/nio/accounts", nioAccount)

      response.status mustBe CREATED

      val value: JsValue = response.json

      (value \ "email").as[String] mustBe "admin@test.fr"
      (value \ "password").asOpt[String] mustBe None
      (value \ "isAdmin").as[Boolean] mustBe true

      val id: String = (value \ "_id").as[String]

      val getResponse: WSResponse = getJson(s"/nio/accounts/$id")

      getResponse.status mustBe OK

      val getValue: JsValue = getResponse.json

      (getValue \ "email").as[String] mustBe "admin@test.fr"
      (getValue \ "password").asOpt[String] mustBe None
      (getValue \ "isAdmin").as[Boolean] mustBe true
    }

    "create nio account with email conflict" in {
      val response: WSResponse =
        postJson("/nio/accounts", nioAccount)

      response.status mustBe CONFLICT

      (response.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "error.account.email.already.used"
    }

    "get nio accounts" in {
      postJson("/nio/accounts", nioAccount2).status mustBe CREATED

      val getResponse: WSResponse = getJson("/nio/accounts")

      getResponse.status mustBe OK

      val value: JsValue = getResponse.json

      (value \ "count").as[Int] mustBe 2
      (value \ "page").as[Int] mustBe 0
      (value \ "pageSize").as[Int] mustBe 10

      val items: JsArray = (value \ "items").as[JsArray]

      items.value.size mustBe 2

      (items \ 0 \ "email").as[String] mustBe "admin2@test.fr"
      (items \ 0 \ "password").asOpt[String] mustBe None
      (items \ 0 \ "isAdmin").as[Boolean] mustBe true

      (items \ 1 \ "email").as[String] mustBe "admin@test.fr"
      (items \ 1 \ "password").asOpt[String] mustBe None
      (items \ 1 \ "isAdmin").as[Boolean] mustBe true
    }

    "update nio account" in {

      val nioAccount: JsValue = Json.obj(
        "email" -> "admin3@test.fr",
        "password" -> "admin",
        "isAdmin" -> true
      )

      val createResponse: WSResponse = postJson("/nio/accounts", nioAccount)
      createResponse.status mustBe CREATED

      val id: String = (createResponse.json \ "_id").as[String]

      val nioAccountUpdated: JsValue = Json.obj(
        "_id" -> id,
        "email" -> "admin3@test.fr",
        "password" -> "admin123",
        "isAdmin" -> false
      )

      val updateResponse: WSResponse =
        putJson(s"/nio/accounts/$id", nioAccountUpdated)

      updateResponse.status mustBe OK

      val value: JsValue = updateResponse.json

      (value \ "email").as[String] mustBe "admin3@test.fr"
      (value \ "password").asOpt[String] mustBe None
      (value \ "isAdmin").as[Boolean] mustBe false

      val getResponse: WSResponse = getJson(s"/nio/accounts/$id")

      getResponse.status mustBe OK

      val getValue: JsValue = getResponse.json

      (getValue \ "email").as[String] mustBe "admin3@test.fr"
      (getValue \ "password").asOpt[String] mustBe None
      (getValue \ "isAdmin").as[Boolean] mustBe false
    }

    "delete nio account" in {
      val getResponse: WSResponse = getJson("/nio/accounts")

      getResponse.status mustBe OK

      val value: JsValue = getResponse.json

      (value \ "count").as[Int] mustBe 3
      (value \ "page").as[Int] mustBe 0
      (value \ "pageSize").as[Int] mustBe 10

      val items: JsArray = (value \ "items").as[JsArray]

      val id: String = (items \ 0 \ "_id").as[String]

      val deleteResponse: WSResponse = delete(s"/nio/accounts/$id")
      deleteResponse.status mustBe OK

      getJson(s"/nio/accounts/$id").status mustBe NOT_FOUND
    }
  }
}
