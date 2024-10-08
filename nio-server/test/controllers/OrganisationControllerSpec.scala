package controllers

import models._

import java.time.{Clock, LocalDateTime}
import utils.{DateUtils, NioLogger, TestUtils}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import java.time.format.DateTimeFormatter

class OrganisationControllerSpec extends TestUtils {

  "OrganisationController" should {
    val org1Key = "orgTest1"

    val org1       = Organisation(
      key = org1Key,
      label = "lbl",
      groups = Seq(
        PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
      )
    )
    val org1AsJson = org1.asJson()

    val org2Key    = "orgTest2"
    val org2       = Organisation(
      key = org2Key,
      label = "lbl",
      groups = Seq(
        PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
      )
    )
    val org2AsJson = org2.asJson()

    val org2Modified       = org2.copy(label = "modified")
    val org2AsJsonModified = org2Modified.asJson()

    val org3Key    = "orgTest3"
    val org3       = Organisation(
      key = org3Key,
      label = "lbl",
      groups = Seq(
        PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
      ),
      offers = Some(
        Seq(
          Offer(
            "offer1",
            "offer 1",
            1,
            Seq(
              PermissionGroup(
                key = "group2",
                label = "groupe 2",
                permissions = Seq(Permission("email", "Please accept email"))
              )
            )
          )
        )
      )
    )
    val org3Update = Organisation(
      key = org3Key,
      label = "lbl",
      groups = Seq(
        PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
      ),
      offers = Some(
        Seq(
          Offer(
            "offer1",
            "offer 1",
            1,
            Seq(PermissionGroup(key = "group1", label = "groupe 1", permissions = Seq(Permission("other", "other"))))
          )
        )
      )
    )

    "create new organisation" in {
      val beforeNewOrgCreation = LocalDateTime.now(Clock.systemUTC).minusSeconds(1)

      val path: String   = s"/$tenant/organisations"
      val createResponse = postJson(path, org1AsJson)

      createResponse.status mustBe CREATED
      createResponse.contentType mustBe JSON

      val msgAsJson = readLastKafkaEvent()
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
        val lastUpdate = LocalDateTime.parse(s, DateUtils.utcDateFormatter)
        lastUpdate.isAfter(beforeNewOrgCreation) && lastUpdate.isBefore(LocalDateTime.now(Clock.systemUTC).plusSeconds(1))
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
        val lastUpdate = LocalDateTime.parse(s, DateUtils.utcDateFormatter)
        lastUpdate.isAfter(beforeNewOrgCreation) && lastUpdate.isBefore(LocalDateTime.now(Clock.systemUTC).plusSeconds(1))
      } mustBe Some(true)
    }

    "create organisation with an already existing key" in {
      postJson(s"/$tenant/organisations", org1AsJson).status mustBe CONFLICT
    }

    "find draft by key" in {
      val path: String         = s"/$tenant/organisations/$org1Key/draft"
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe org1.version.status
    }

    "release draft by key" in {
      val path: String         = s"/$tenant/organisations/$org1Key/draft/_release"
      val response: WSResponse = postJson(path, org1AsJson)

      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe 1
      (value \ "version" \ "latest").as[Boolean] mustBe true

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "OrganisationReleased"
      (msgAsJson \ "payload" \ "key").as[String] mustBe org1Key
      (msgAsJson \ "payload" \ "version" \ "num")
        .as[Int] mustBe org1.version.num
    }

    "find last release by key" in {
      val path: String         = s"/$tenant/organisations/$org1Key/last"
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsValue = response.json
      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe org1.version.num
      (value \ "version" \ "lastUpdate").asOpt[String].map { s =>
        val lastUpdate = LocalDateTime.parse(s, DateUtils.utcDateFormatter)
        lastUpdate.isBefore(LocalDateTime.now(Clock.systemUTC).plusSeconds(1))
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
      val path: String         = s"/$tenant/organisations/$org1Key/1"
      val response: WSResponse = getJson(path)

      response.status mustBe OK

      val value: JsValue = response.json
      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe 1
    }

    "release draft by key again" in {
      val path: String         = s"/$tenant/organisations/$org1Key/draft/_release"
      val response: WSResponse = postJson(path, org1AsJson)

      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "key").as[String] mustBe org1Key
      (value \ "version" \ "status").as[String] mustBe "RELEASED"
      (value \ "version" \ "num").as[Int] mustBe 2
      (value \ "version" \ "latest").as[Boolean] mustBe true

      val orgRelease1: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/1")

      orgRelease1.status mustBe OK

      val value1: JsValue = orgRelease1.json

      (value1 \ "key").as[String] mustBe org1Key
      (value1 \ "version" \ "status").as[String] mustBe "RELEASED"
      (value1 \ "version" \ "num").as[Int] mustBe 1
      (value1 \ "version" \ "latest").as[Boolean] mustBe false

      val msgAsJson = readLastKafkaEvent()
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
      val path: String         = s"/$tenant/organisations"
      val response: WSResponse = postJson(path, org2AsJson)

      response.status mustBe CREATED
      response.contentType mustBe JSON

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "OrganisationCreated"
      (msgAsJson \ "payload" \ "key").as[String] mustBe org2Key
    }

    "modify something in the created draft" in {
      val path: String         = s"/$tenant/organisations/$org2Key/draft"
      val response: WSResponse = putJson(path, org2AsJsonModified)

      response.status mustBe OK

      val putValue: JsValue = response.json

      (putValue \ "label").as[String] mustBe org2Modified.label

      val msgAsJson = readLastKafkaEvent()
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
        "key"    -> "org key 3",
        "label"  -> "modified",
        "groups" -> Json.arr(
          Json.obj(
            "key"         -> "group1",
            "label"       -> "blalba",
            "permissions" -> Json.arr(
              Json.obj(
                "key"   -> "sms",
                "label" -> "blabla"
              )
            )
          ),
          Json.obj(
            "key"         -> "group2",
            "label"       -> "blalba",
            "permissions" -> Json.arr(
              Json.obj(
                "key"   -> "sms",
                "label" -> "blabla"
              )
            )
          )
        )
      )

      val path: String         = s"/$tenant/organisations"
      val response: WSResponse = postJson(path, org3AsJsonInvalidKeyName)

      response.status mustBe BAD_REQUEST
    }

    "create invalid organisation groups empty" in {
      val org3AsJsonInvalidGroupsEmpty = Json.obj(
        "key"    -> "orgkey3",
        "label"  -> "modified",
        "groups" -> Json.arr(
        )
      )

      val path: String         = s"/$tenant/organisations"
      val response: WSResponse =
        postJson(path, org3AsJsonInvalidGroupsEmpty)

      response.status mustBe BAD_REQUEST
    }

    "create invalid organisation groups invalid key" in {
      val org3AsJsonInvalidGroupsEmpty = Json.obj(
        "key"    -> "orgkey3",
        "label"  -> "modified",
        "groups" -> Json.arr(
          Json.obj(
            "key"         -> "a a  a a",
            "label"       -> "what do you want",
            "permissions" -> Json.arr()
          )
        )
      )

      val path: String         = s"/$tenant/organisations"
      val response: WSResponse =
        postJson(path, org3AsJsonInvalidGroupsEmpty)

      response.status mustBe BAD_REQUEST
    }

    "create invalid organisation permissions invalid key" in {
      val org3AsJsonInvalidGroupsEmpty = Json.obj(
        "key"    -> "orgkey3",
        "label"  -> "modified",
        "groups" -> Json.arr(
          Json.obj(
            "key"         -> "group1",
            "label"       -> "my first group",
            "permissions" -> Json.arr(
              Json.obj(
                "key"   -> "in va lid",
                "label" -> "what do you want"
              )
            )
          ),
          Json.obj(
            "key"         -> "group2",
            "label"       -> "my first group",
            "permissions" -> Json.arr(
              Json.obj(
                "key"   -> "in va lid",
                "label" -> "what do you want"
              )
            )
          )
        )
      )

      val path: String         = s"/$tenant/organisations"
      val response: WSResponse =
        postJson(path, org3AsJsonInvalidGroupsEmpty)

      response.status mustBe BAD_REQUEST
    }

    "delete organisation" in {

      val orgKey = "organisationToDelete"

      val orgAsJson = Json.obj(
        "key"    -> orgKey,
        "label"  -> "lbl",
        "groups" -> Json.arr(
          Json.obj(
            "key"         -> "group1",
            "label"       -> "blalba",
            "permissions" -> Json.arr(
              Json.obj(
                "key"   -> "sms",
                "label" -> "Please accept sms"
              )
            )
          )
        )
      )

      val respOrgaCreated: WSResponse =
        postJson(s"/$tenant/organisations", orgAsJson)
      respOrgaCreated.status mustBe CREATED

      val respRelease =
        postJson(s"/$tenant/organisations/$orgKey/draft/_release", respOrgaCreated.json)
      respRelease.status mustBe OK

      val userId: String = "userToDelete"

      val consentFactAsJson = Json.obj(
        "userId"  -> userId,
        "doneBy"  -> Json.obj(
          "userId" -> userId,
          "role"   -> "USER"
        ),
        "version" -> 1,
        "groups"  -> Json.arr(
          Json.obj(
            "key"      -> "group1",
            "label"    -> "blalba",
            "consents" -> Json.arr(
              Json.obj(
                "key"     -> "sms",
                "label"   -> "Please accept sms",
                "checked" -> true
              )
            )
          )
        )
      )

      putJson(s"/$tenant/organisations/$orgKey/users/$userId", consentFactAsJson).status mustBe OK

      delete(s"/$tenant/organisations/$orgKey").status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "OrganisationDeleted"
      (msgAsJson \ "payload" \ "key").as[String] mustBe orgKey

      getJson(s"/$tenant/organisations/$orgKey").status mustBe NOT_FOUND
      getJson(s"/$tenant/organisations/$orgKey/users/$userId").status mustBe NOT_FOUND
    }

    "create organisation with offer" in {
      val respOrgaCreated: WSResponse =
        postJson(s"/$tenant/organisations", org3.asJson())
      respOrgaCreated.status mustBe CREATED

      val value: JsValue = respOrgaCreated.json

      (value \ "key").as[String] mustBe org3.key
      (value \ "label").as[String] mustBe org3.label

      (value \ "version" \ "status").as[String] mustBe org3.version.status
      (value \ "version" \ "num").as[Int] mustBe org3.version.num
      (value \ "version" \ "latest").as[Boolean] mustBe org3.version.latest
      (value \ "version" \ "neverReleased").asOpt[Boolean] mustBe None

      val groups = (value \ "groups").as[JsArray]
      groups.value.size mustBe org3.groups.size

      (groups \ 0 \ "key").as[String] mustBe org3.groups.head.key
      (groups \ 0 \ "label").as[String] mustBe org3.groups.head.label

      val permissions = (groups \ 0 \ "permissions").as[JsArray]
      (permissions \ 0 \ "key")
        .as[String] mustBe org3.groups.head.permissions.head.key
      (permissions \ 0 \ "label")
        .as[String] mustBe org3.groups.head.permissions.head.label

      (value \ "offers").asOpt[String] mustBe None

      val respOrgaUpdated: WSResponse =
        putJson(s"/$tenant/organisations/$org3Key/draft", org3Update.asJson())
      respOrgaUpdated.status mustBe OK

      val valuePut: JsValue = respOrgaUpdated.json
      (valuePut \ "key").as[String] mustBe org3Update.key
      (valuePut \ "label").as[String] mustBe org3Update.label

      (valuePut \ "version" \ "status").as[String] mustBe org3Update.version.status
      (valuePut \ "version" \ "num").as[Int] mustBe org3Update.version.num
      (valuePut \ "version" \ "latest").as[Boolean] mustBe org3Update.version.latest
      (valuePut \ "version" \ "neverReleased").asOpt[Boolean] mustBe None

      val groupsPut = (valuePut \ "groups").as[JsArray]
      groupsPut.value.size mustBe org3Update.groups.size

      (groupsPut \ 0 \ "key").as[String] mustBe org3Update.groups.head.key
      (groupsPut \ 0 \ "label").as[String] mustBe org3Update.groups.head.label

      val permissionsPut = (groupsPut \ 0 \ "permissions").as[JsArray]
      (permissionsPut \ 0 \ "key")
        .as[String] mustBe org3Update.groups.head.permissions.head.key
      (permissionsPut \ 0 \ "label")
        .as[String] mustBe org3Update.groups.head.permissions.head.label

      (valuePut \ "offers").asOpt[String] mustBe None
    }
  }

  "validate release management" should {
    "create released without organisation creation" in {
      val orgKey = "orgTest5"
      val org    = Organisation(
        key = orgKey,
        label = "lbl",
        groups = Seq(
          PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
        )
      )

      val releaseErrorResponse: WSResponse =
        postJson(s"/$tenant/organisations/$orgKey/draft/_release", org.asJson())
      releaseErrorResponse.status mustBe NOT_FOUND

      val creationResponse: WSResponse =
        postJson(s"/$tenant/organisations", org.asJson())
      creationResponse.status mustBe CREATED

      val releaseResponse: WSResponse =
        postJson(s"/$tenant/organisations/$orgKey/draft/_release", org.asJson())
      releaseResponse.status mustBe OK
    }
  }

  "check version number cannot be change by client" should {

    "validate version" in {
      val orgKey = "orgTest6"
      val org    = Organisation(
        key = orgKey,
        label = "lbl",
        version = VersionInfo(
          num = 5,
          status = "RELEASED"
        ),
        groups = Seq(
          PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
        )
      )

      // Create organisation with wrong version num/ version status
      val createResponse: WSResponse =
        postJson(s"/$tenant/organisations", org.asJson())
      createResponse.status mustBe CREATED

      val createOrganisationJson: JsValue = createResponse.json

      (createOrganisationJson \ "version" \ "num").as[Int] mustBe 1
      (createOrganisationJson \ "version" \ "status").as[String] mustBe "DRAFT"

      val get1Response: WSResponse =
        getJson(s"/$tenant/organisations/$orgKey/draft")
      get1Response.status mustBe OK
      (get1Response.json \ "version" \ "num").as[Int] mustBe 1
      (get1Response.json \ "version" \ "status").as[String] mustBe "DRAFT"

      // Update organisation with wrong version num/ version status
      val updateResponse: WSResponse =
        putJson(s"/$tenant/organisations/$orgKey/draft", org.asJson())
      updateResponse.status mustBe OK

      val updateOrganisationJson: JsValue = updateResponse.json

      (updateOrganisationJson \ "version" \ "num").as[Int] mustBe 1
      (updateOrganisationJson \ "version" \ "status").as[String] mustBe "DRAFT"

      val get2Response: WSResponse =
        getJson(s"/$tenant/organisations/$orgKey/draft")
      get2Response.status mustBe OK
      (get2Response.json \ "version" \ "num").as[Int] mustBe 1
      (get2Response.json \ "version" \ "status").as[String] mustBe "DRAFT"
    }
  }

  "tenant must be existed to create a new organisation" should {

    "if we create a new organisation on a inexisting tenant" in {

      val orgKey = "orgTest6"
      val org    = Organisation(
        key = orgKey,
        label = "lbl",
        version = VersionInfo(
          num = 5,
          status = "RELEASED"
        ),
        groups = Seq(
          PermissionGroup(key = "group1", label = "blalba", permissions = Seq(Permission("sms", "Please accept sms")))
        )
      )

      // Create organisation with wrong version num/ version status
      val createResponse: WSResponse =
        postJson(s"/tenantunknow/organisations", org.asJson())
      createResponse.status mustBe NOT_FOUND

    }

  }

  "organisation with error" should {
    "error" in {

      val orgKey            = "error"
      val org: Organisation = Organisation(
        key = orgKey,
        label = "lbl",
        version = VersionInfo(
          num = 5,
          status = "RELEASED"
        ),
        groups = Seq(
          PermissionGroup(
            key = "bla-qlkfqlj _lk",
            label = "label",
            permissions = Seq(Permission("fjslkjf sjklfl", "Please accept sms"))
          )
        ),
        offers = Some(
          Seq(
            Offer(
              key = "toto",
              label = "toto",
              groups = Seq(
                PermissionGroup(key = "group1", label = "bla-qlkfqlj _lk", permissions = Seq.empty)
              )
            )
          )
        )
      )

      val response: WSResponse = postJson(s"/$tenant/organisations", org.asJson())

      response.status mustBe BAD_REQUEST

      val value: JsValue = response.json

      (value \ "errors").as[JsArray].value.length mustBe 2

      (value \ "errors" \ 0 \ "message")
        .as[String] mustBe "error.organisation.groups.0.key"
      (value \ "errors" \ 1 \ "message")
        .as[String] mustBe "error.organisation.groups.0.permissions.0.key"
    }
  }
}
