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
    "clientId" -> "clientId",
    "clientSecret" -> "clientSecret",
    "isAdmin" -> true
  )

  private val nioAccount2: JsValue = Json.obj(
    "email" -> "admin2@test.fr",
    "password" -> "admin",
    "clientId" -> "clientId2",
    "clientSecret" -> "clientSecret",
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
      (value \ "clientId").as[String] mustBe "clientId"
      (value \ "clientSecret").as[String] mustBe "clientSecret"
      (value \ "isAdmin").as[Boolean] mustBe true

      val id: String = (value \ "_id").as[String]

      val getResponse: WSResponse = getJson(s"/nio/accounts/$id")

      getResponse.status mustBe OK

      val getValue: JsValue = getResponse.json

      (getValue \ "email").as[String] mustBe "admin@test.fr"
      (getValue \ "password").asOpt[String] mustBe None
      (getValue \ "clientId").as[String] mustBe "clientId"
      (getValue \ "clientSecret").as[String] mustBe "clientSecret"
      (getValue \ "isAdmin").as[Boolean] mustBe true
    }

    "create nio account with email conflict" in {
      val response: WSResponse =
        postJson("/nio/accounts", nioAccount)

      response.status mustBe CONFLICT

      (response.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "error.account.email.already.used"
    }

    "create nio account with client conflict" in {
      val nioAccountClientIdExist: JsValue = Json.obj(
        "email" -> "admin-test@test.fr",
        "password" -> "admin",
        "clientId" -> "clientId",
        "clientSecret" -> "clientSecret",
        "isAdmin" -> true
      )

      val response: WSResponse =
        postJson("/nio/accounts", nioAccountClientIdExist)

      response.status mustBe CONFLICT

      (response.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "error.account.client.id.already.used"
    }

    "create nio account with invalid clientId" in {
      val nioAccountClientIdExist: JsValue = Json.obj(
        "email" -> "admin-test@test.fr",
        "password" -> "admin",
        "clientId" -> "client Id",
        "clientSecret" -> "clientSecret",
        "isAdmin" -> true
      )

      val response: WSResponse =
        postJson("/nio/accounts", nioAccountClientIdExist)

      response.status mustBe BAD_REQUEST
    }

    "create nio account with invalid clientSecret" in {
      val nioAccountClientIdExist: JsValue = Json.obj(
        "email" -> "admin-test@test.fr",
        "password" -> "admin",
        "clientId" -> "clientId1234",
        "clientSecret" -> "client Secret",
        "isAdmin" -> true
      )

      val response: WSResponse =
        postJson("/nio/accounts", nioAccountClientIdExist)

      response.status mustBe BAD_REQUEST
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
      (items \ 0 \ "clientId").as[String] mustBe "clientId2"
      (items \ 0 \ "clientSecret").as[String] mustBe "clientSecret"
      (items \ 0 \ "isAdmin").as[Boolean] mustBe true

      (items \ 1 \ "email").as[String] mustBe "admin@test.fr"
      (items \ 1 \ "password").asOpt[String] mustBe None
      (items \ 1 \ "clientId").as[String] mustBe "clientId"
      (items \ 1 \ "clientSecret").as[String] mustBe "clientSecret"
      (items \ 1 \ "isAdmin").as[Boolean] mustBe true
    }

    "update nio account" in {

      val nioAccount: JsValue = Json.obj(
        "email" -> "admin3@test.fr",
        "password" -> "admin",
        "clientId" -> "clientId3",
        "clientSecret" -> "clientSecret",
        "isAdmin" -> true
      )

      val createResponse: WSResponse = postJson("/nio/accounts", nioAccount)
      createResponse.status mustBe CREATED

      val id: String = (createResponse.json \ "_id").as[String]

      val nioAccountUpdated: JsValue = Json.obj(
        "_id" -> id,
        "email" -> "admin3@test.fr",
        "password" -> "admin123",
        "clientId" -> "clientId3",
        "clientSecret" -> "clientSecret123",
        "isAdmin" -> false
      )

      val updateResponse: WSResponse =
        putJson(s"/nio/accounts/$id", nioAccountUpdated)

      updateResponse.status mustBe OK

      val value: JsValue = updateResponse.json

      (value \ "email").as[String] mustBe "admin3@test.fr"
      (value \ "password").asOpt[String] mustBe None
      (value \ "clientId").as[String] mustBe "clientId3"
      (value \ "clientSecret").as[String] mustBe "clientSecret"
      (value \ "isAdmin").as[Boolean] mustBe false

      val getResponse: WSResponse = getJson(s"/nio/accounts/$id")

      getResponse.status mustBe OK

      val getValue: JsValue = getResponse.json

      (getValue \ "email").as[String] mustBe "admin3@test.fr"
      (getValue \ "password").asOpt[String] mustBe None
      (getValue \ "clientId").as[String] mustBe "clientId3"
      (getValue \ "clientSecret").as[String] mustBe "clientSecret"
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
