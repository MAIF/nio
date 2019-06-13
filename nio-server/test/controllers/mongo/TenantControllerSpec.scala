package controllers.mongo

import models.Tenant
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TestUtilsMongo

class TenantControllerSpec extends TestUtilsMongo {

  val secret = "tenant-admin-secret" -> "secret"

  val jsonHeaders = Seq(ACCEPT -> JSON, CONTENT_TYPE -> JSON, secret)
  val xmlHeaders = Seq(ACCEPT -> XML, CONTENT_TYPE -> XML, secret)

  val tenant1AsJson = Json.obj(
    "key" -> "newTenant",
    "description" -> "a new tenant"
  )

  "TenantController" should {
    val path: String = "/tenants"

    "get" in {
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsArray = response.json.as[JsArray]

      value.value.size mustBe 1

      (value \ 0 \ "key").as[String] mustBe s"$tenant"
      (value \ 0 \ "description")
        .as[String] mustBe "Default tenant from config file"
    }

    "create a new tenant" in {
      val response: WSResponse =
        callJson(path, POST, tenant1AsJson, headers = jsonHeaders)

      response.status mustBe CREATED

      response.contentType.contains("json") mustBe true

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "TenantCreated"
      (msgAsJson \ "payload" \ "key").as[String] mustBe "newTenant"
    }

    "after create a new tenant" in {
      val response = getJson(path)

      response.status mustBe OK

      val value: JsArray = response.json.as[JsArray]

      value.value.size mustBe 2

      (value \ 0 \ "key").as[String] mustBe s"$tenant"
      (value \ 0 \ "description")
        .as[String] mustBe "Default tenant from config file"

      (value \ 1 \ "key").as[String] mustBe "newTenant"
      (value \ 1 \ "description").as[String] mustBe "a new tenant"
    }

    "create a new tenant as XML" in {
      val tenantXml = Tenant("testTenant", "test tenant").asXml

      val resp = postXml(path, tenantXml, headers = xmlHeaders)

      resp.status mustBe CREATED

      resp.contentType.contains("xml") mustBe true

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "TenantCreated"
      (msgAsJson \ "payload" \ "key").as[String] mustBe "testTenant"
    }

    "get as XML" in {
      val resp = getXml(path, headers = xmlHeaders)

      resp.status mustBe OK

      resp.contentType.contains("xml") mustBe true

      val xmlValue = resp.xml

      (xmlValue \\ "key")(0).text mustBe s"$tenant"
      (xmlValue \\ "key")(1).text mustBe "newTenant"
      (xmlValue \\ "key")(2).text mustBe "testTenant"
    }

    "create with content-type xml" in {
      val tenantXml = Tenant("testTenantXml", "test tenant").asXml

      val resp =
        postXml(path, tenantXml, headers = Seq(CONTENT_TYPE -> XML, secret))

      resp.status mustBe CREATED
      resp.contentType mustBe s"$XML; charset=UTF-8"
    }

    "create with content-type json" in {
      val tenantJson = Tenant("testTenantJson", "test tenant").asJson

      val resp =
        postJson(path, tenantJson, headers = Seq(CONTENT_TYPE -> JSON, secret))

      resp.status mustBe CREATED
      resp.contentType mustBe JSON
    }

    "create tenant with an already exist key" in {
      val tenantAsJson = Json.obj(
        "key" -> "newTenantAlreadyExist",
        "description" -> "a new tenant"
      )

      val resp = postJson(path, tenantAsJson, headers = jsonHeaders)

      resp.status mustBe CREATED

      val alreadyExistResp = postJson(path, tenantAsJson, headers = jsonHeaders)

      alreadyExistResp.status mustBe CONFLICT
    }

    "delete tenant" in {

      val tenantToDelete: String = "tenanttodelete"
      val tenantAsJson = Json.obj(
        "key" -> tenantToDelete,
        "description" -> "a new tenant"
      )

      val respCreate = postJson("/tenants", tenantAsJson, headers = jsonHeaders)

      respCreate.status mustBe CREATED

      val respGet1 = getJson("/tenants", headers = jsonHeaders)

      respGet1.status mustBe OK

      respGet1.json.as[JsArray].value.size mustBe 7

      val orgKey = "organisationToDelete"

      val orgAsJson = Json.obj(
        "key" -> orgKey,
        "label" -> "lbl",
        "groups" -> Json.arr(
          Json.obj(
            "key" -> "group1",
            "label" -> "blalba",
            "permissions" -> Json.arr(
              Json.obj(
                "key" -> "sms",
                "label" -> "Please accept sms"
              )
            )
          )
        )
      )

      val respOrgaCreated: WSResponse =
        postJson(s"/$tenantToDelete/organisations", orgAsJson)
      respOrgaCreated.status mustBe CREATED

      postJson(s"/$tenantToDelete/organisations/$orgKey/draft/_release",
               respOrgaCreated.json).status mustBe OK

      val userId: String = "userToDelete"

      val consentFactAsJson = Json.obj(
        "userId" -> userId,
        "doneBy" -> Json.obj(
          "userId" -> userId,
          "role" -> "USER"
        ),
        "version" -> 1,
        "groups" -> Json.arr(
          Json.obj(
            "key" -> "group1",
            "label" -> "blalba",
            "consents" -> Json.arr(
              Json.obj(
                "key" -> "sms",
                "label" -> "Please accept sms",
                "checked" -> true
              )
            )
          )
        )
      )

      putJson(s"/$tenantToDelete/organisations/$orgKey/users/$userId",
              consentFactAsJson).status mustBe OK

      delete(s"/tenants/$tenantToDelete", headers = jsonHeaders).status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "TenantDeleted"
      (msgAsJson \ "payload" \ "key").as[String] mustBe tenantToDelete

      val respGet = getJson("/tenants", headers = jsonHeaders)

      respGet.status mustBe OK

      respGet.json.as[JsArray].value.size mustBe 6

      getJson(s"/$tenantToDelete/organisations/$orgKey", headers = jsonHeaders).status mustBe NOT_FOUND
      getJson(s"/$tenantToDelete/organisations/$orgKey/users/$userId",
              headers = jsonHeaders).status mustBe NOT_FOUND
    }
  }
}
