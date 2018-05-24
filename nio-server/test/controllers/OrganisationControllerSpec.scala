package controllers

import models.{Organisation, Permission, PermissionGroup}
import net.manub.embeddedkafka.EmbeddedKafka
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import utils.TestUtils
import play.api.test.Helpers._

class OrganisationControllerSpec extends TestUtils {

  "OrganisationController" should {
    val tenant: String = "sandbox"
    val org1Key = "orgTest1"

    val org1 = Organisation(
      key = org1Key,
      label = "lbl",
      groups = Seq(
        PermissionGroup(key = "group1",
                        label = "blalba",
                        permissions =
                          Seq(Permission("sms", "Please accept sms")))
      )
    )
    val org1AsJson = org1.asJson

    val org2Key = "orgTest2"
    val org2 = Organisation(
      key = org2Key,
      label = "lbl",
      groups = Seq(
        PermissionGroup(key = "group1",
                        label = "blalba",
                        permissions =
                          Seq(Permission("sms", "Please accept sms")))
      )
    )
    val org2AsJson = org2.asJson

    val org2Modified = org2.copy(label = "modified")
    val org2AsJsonModified = org2Modified.asJson

    "create new organisation" in {
      val beforeNewOrgCreation = DateTime.now(DateTimeZone.UTC).minusSeconds(1)

      val path: String = s"/$tenant/organisations"
      val createResponse = postJson(path, org1AsJson)

      createResponse.status mustBe CREATED
      createResponse.contentType mustBe JSON

      val msg = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msgAsJson = Json.parse(msg)
      (msgAsJson \ "type").as[String] mustBe "OrganisationCreated"
      (msgAsJson \ "payload" \ "key").as[String] mustBe org1Key

      val response: WSResponse = getJson(s"$path/$org1Key/draft")
      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "key").as[String] mustBe org1.key
      (value \ "label").as[String] mustBe org1.label

      (value \ "version" \ "status").as[String] mustBe org1.version.status
      (value \ "version" \ "num").as[Int] mustBe org1.version.num
      (value \ "version" \ "latest").as[Boolean] mustBe org1.version.latest
      (value \ "version" \ "neverReleased").asOpt[Boolean] mustBe None
      (value \ "version" \ "lastUpdate").asOpt[String].map { s =>
        val lastUpdate = DateTime.parse(s)
        lastUpdate.isAfter(beforeNewOrgCreation) && lastUpdate.isBefore(
          DateTime.now(DateTimeZone.UTC).plusSeconds(1))
      } mustBe Some(true)

      val groups = (value \ "groups").as[JsArray]
      groups.value.size mustBe org1.groups.size

      (groups \ 0 \ "key").as[String] mustBe org1.groups.head.key
      (groups \ 0 \ "label").as[String] mustBe org1.groups.head.label

      val permissions = (groups \ 0 \ "permissions").as[JsArray]
      (permissions \ 0 \ "key")
        .as[String] mustBe org1.groups.head.permissions.head.key
      (permissions \ 0 \ "label")
        .as[String] mustBe org1.groups.head.permissions.head.label

      val getOrganisationsResponse: WSResponse = getJson(path)
      getOrganisationsResponse.status mustBe OK

      val organisations: JsArray = getOrganisationsResponse.json.as[JsArray]

      (organisations \ 0 \ "key").as[String] mustBe "maif"
      (organisations \ 0 \ "label").as[String] mustBe "MAIF"
      (organisations \ 0 \ "version" \ "status").as[String] mustBe "RELEASED"
      (organisations \ 0 \ "version" \ "num").as[Int] mustBe 2

      (organisations \ 1 \ "key").as[String] mustBe org1Key
      (organisations \ 1 \ "label").as[String] mustBe "lbl"
      (organisations \ 1 \ "version" \ "status").as[String] mustBe "DRAFT"
      (organisations \ 1 \ "version" \ "num").as[Int] mustBe 1
      (organisations \ 1 \ "version" \ "lastUpdate").asOpt[String].map { s =>
        val lastUpdate = DateTime.parse(s)
        lastUpdate.isAfter(beforeNewOrgCreation) && lastUpdate.isBefore(
          DateTime.now(DateTimeZone.UTC).plusSeconds(1))
      } mustBe Some(true)
    }

    "create organisation with an already existing key" in {
      postJson(s"/$tenant/organisations", org1AsJson).status mustBe CONFLICT
    }

    "find draft by key" in {
      val path: String = s"/$tenant/organisations/$org1Key/draft"
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe org1.version.status
    }

    "release draft by key" in {
      val path: String = s"/$tenant/organisations/$org1Key/draft/_release"
      val response: WSResponse = postJson(path, org1AsJson)

      response.status mustBe OK
      response.json.toString mustBe "true"

      val msg = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msgAsJson = Json.parse(msg)
      (msgAsJson \ "type").as[String] mustBe "OrganisationReleased"
      (msgAsJson \ "payload" \ "key").as[String] mustBe org1Key
      (msgAsJson \ "payload" \ "version" \ "num")
        .as[Int] mustBe org1.version.num
    }

    "find last release by key" in {
      val path: String = s"/$tenant/organisations/$org1Key/last"
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsValue = response.json
      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe org1.version.num
      (value \ "version" \ "lastUpdate").asOpt[String].map { s =>
        val lastUpdate = DateTime.parse(s)
        lastUpdate.isBefore(DateTime.now(DateTimeZone.UTC).plusSeconds(1))
      } mustBe Some(true)
    }

    "find draft by key after doing release" in {
      val response: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/draft")

      response.status mustBe OK

      val value: JsValue = response.json
      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "DRAFT"
      (value \ "version" \ "num").as[Int] mustBe 2
    }

    "find specific release by key and version num" in {
      val path: String = s"/$tenant/organisations/$org1Key/1"
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsValue = response.json
      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe 1
    }

    "release draft by key again" in {
      val path: String = s"/$tenant/organisations/$org1Key/draft/_release"
      val response: WSResponse = postJson(path, org1AsJson)

      response.status mustBe OK

      response.json.toString mustBe "true"

      val msg = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msgAsJson = Json.parse(msg)
      (msgAsJson \ "type").as[String] mustBe "OrganisationReleased"
      (msgAsJson \ "payload" \ "key").as[String] mustBe org1Key
      (msgAsJson \ "payload" \ "version" \ "num").as[Int] mustBe 2
    }

    "find last release by key again" in {
      val response: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/last")

      response.status mustBe OK

      val value: JsValue = response.json
      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe 2
    }

    "find all releases by key" in {
      val response: WSResponse = getJson(s"/$tenant/organisations/$org1Key")

      val value: JsArray = response.json.as[JsArray]

      (value.last \ "key").as[String] mustBe org1Key
      (value.last \ "version" \ "status").as[String] mustBe "RELEASED"
      (value.last \ "version" \ "num").as[Int] mustBe 2
      (value.last \ "version" \ "latest").as[Boolean] mustBe true
    }

    "create another orga" in {
      val path: String = s"/$tenant/organisations"
      val response: WSResponse = postJson(path, org2AsJson)

      response.status mustBe CREATED
      response.contentType mustBe JSON

      val msg = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msgAsJson = Json.parse(msg)
      (msgAsJson \ "type").as[String] mustBe "OrganisationCreated"
      (msgAsJson \ "payload" \ "key").as[String] mustBe org2Key
    }

    "modify something in the created draft" in {
      val path: String = s"/$tenant/organisations/$org2Key/draft"
      val response: WSResponse = putJson(path, org2AsJsonModified)

      response.status mustBe OK
      response.json.toString mustBe "true"

      val msg = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msgAsJson = Json.parse(msg)
      (msgAsJson \ "type").as[String] mustBe "OrganisationUpdated"
      (msgAsJson \ "oldValue" \ "label").as[String] mustBe "lbl"
      (msgAsJson \ "payload" \ "label").as[String] mustBe "modified"

      val draftResponse: WSResponse = getJson(path)

      draftResponse.status mustBe OK

      val value: JsValue = draftResponse.json
      (value \ "label").as[String] mustBe org2Modified.label
    }

    "list all latest orgas" in {
      val response: WSResponse = getJson(s"/$tenant/organisations")

      response.status mustBe OK
      response.json.as[JsArray].value.size mustBe 3
    }

    "list all latest orgas as XML" in {
      val resp = getXml(s"/$tenant/organisations")

      resp.status mustBe OK
      resp.contentType.contains("xml") mustBe true

      val xmlValue = resp.xml

      (xmlValue \\ "key").size mustBe 3
    }

    "create invalid organisation key name" in {
      val org3AsJsonInvalidKeyName = Json.obj(
        "key" -> "org key 3",
        "label" -> "modified",
        "groups" -> Json.arr(
          Json.obj(
            "key" -> "group1",
            "label" -> "blalba",
            "permissions" -> Json.arr(
              Json.obj(
                "key" -> "sms",
                "label" -> "blabla"
              )
            )
          ),
          Json.obj(
            "key" -> "group2",
            "label" -> "blalba",
            "permissions" -> Json.arr(
              Json.obj(
                "key" -> "sms",
                "label" -> "blabla"
              )
            )
          )
        )
      )

      val path: String = s"/$tenant/organisations"
      val response: WSResponse = postJson(path, org3AsJsonInvalidKeyName)

      response.status mustBe BAD_REQUEST
    }

    "create invalid organisation groups empty" in {
      val org3AsJsonInvalidGroupsEmpty = Json.obj(
        "key" -> "orgkey3",
        "label" -> "modified",
        "groups" -> Json.arr(
          )
      )

      val path: String = s"/$tenant/organisations"
      val response: WSResponse =
        postJson(path, org3AsJsonInvalidGroupsEmpty)

      response.status mustBe BAD_REQUEST
    }

    "create invalid organisation groups invalid key" in {
      val org3AsJsonInvalidGroupsEmpty = Json.obj(
        "key" -> "orgkey3",
        "label" -> "modified",
        "groups" -> Json.arr(
          Json.obj(
            "key" -> "a a  a a",
            "label" -> "what do you want",
            "permissions" -> Json.arr()
          )
        )
      )

      val path: String = s"/$tenant/organisations"
      val response: WSResponse =
        postJson(path, org3AsJsonInvalidGroupsEmpty)

      response.status mustBe BAD_REQUEST
    }

    "create invalid organisation permissions invalid key" in {
      val org3AsJsonInvalidGroupsEmpty = Json.obj(
        "key" -> "orgkey3",
        "label" -> "modified",
        "groups" -> Json.arr(
          Json.obj(
            "key" -> "group1",
            "label" -> "my first group",
            "permissions" -> Json.arr(
              Json.obj(
                "key" -> "in va lid",
                "label" -> "what do you want"
              )
            )
          ),
          Json.obj(
            "key" -> "group2",
            "label" -> "my first group",
            "permissions" -> Json.arr(
              Json.obj(
                "key" -> "in va lid",
                "label" -> "what do you want"
              )
            )
          )
        )
      )

      val path: String = s"/$tenant/organisations"
      val response: WSResponse =
        postJson(path, org3AsJsonInvalidGroupsEmpty)

      response.status mustBe BAD_REQUEST
    }

    "delete organisation" in {

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
        postJson(s"/$tenant/organisations", orgAsJson)
      respOrgaCreated.status mustBe CREATED
      EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)

      val respRelease =
        postJson(s"/$tenant/organisations/$orgKey/draft/_release",
                 respOrgaCreated.json)
      respRelease.status mustBe OK
      EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)

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

      putJson(s"/$tenant/organisations/$orgKey/users/$userId",
              consentFactAsJson).status mustBe OK
      EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)

      delete(s"/$tenant/organisations/$orgKey").status mustBe OK

      val msg = EmbeddedKafka.consumeFirstStringMessageFrom(kafkaTopic)
      val msgAsJson = Json.parse(msg)
      (msgAsJson \ "type").as[String] mustBe "OrganisationDeleted"
      (msgAsJson \ "payload" \ "key").as[String] mustBe orgKey

      getJson(s"/$tenant/organisations/$orgKey").status mustBe NOT_FOUND
      getJson(s"/$tenant/organisations/$orgKey/users/$userId").status mustBe NOT_FOUND
    }
  }
}
