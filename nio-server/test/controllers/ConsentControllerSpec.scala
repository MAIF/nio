package controllers

import akka.japi.Option
import models._
import org.joda.time.{DateTime, DateTimeZone}
import utils.NioLogger
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import utils.{DateUtils, TestUtils}
import play.api.test.Helpers._

class ConsentControllerSpec extends TestUtils {

  val userId1: String = "userId1"
  val userId2: String = "userId2"
  val userId3: String = "userId3"
  val userId4: String = "userId4"
  val userId5: String = "userId5"
  val userId6: String = "userId6"

  val user1       = ConsentFact(
    userId = userId1,
    doneBy = DoneBy(userId = userId1, role = "USER"),
    version = 2,
    groups = Seq(
      ConsentGroup(
        key = "maifNotifs",
        label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        consents = Seq(
          Consent(key = "phone", label = "Par contact téléphonique", checked = true),
          Consent(key = "mail", label = "Par contact électronique", checked = false),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
        )
      ),
      ConsentGroup(
        key = "partenaireNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        consents = Seq(
          Consent(key = "phone", label = "Par contact téléphonique", checked = false),
          Consent(key = "mail", label = "Par contact électronique", checked = true),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
        )
      )
    )
  )
  val user1AsJson = user1.asJson()

  val user1Modified       = user1.copy(doneBy = user1.doneBy.copy(role = "ADMIN"))
  val user1ModifiedAsJson = user1Modified.asJson()

  val user3       = ConsentFact(
    userId = userId3,
    doneBy = DoneBy(userId = userId3, role = "USER"),
    version = 2,
    groups = Seq(
      ConsentGroup(
        key = "maifNotifs",
        label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        consents = Seq(
          Consent(key = "phone", label = "Par contact téléphonique", checked = true),
          Consent(key = "mail", label = "Par contact électronique", checked = false),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
        )
      ),
      ConsentGroup(
        key = "partenaireNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        consents = Seq(
          Consent(key = "phone", label = "Par contact téléphonique", checked = false),
          Consent(key = "mail", label = "Par contact électronique", checked = true),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
        )
      )
    )
  )
  val user3AsJson = user3.asJson()

  val user2SubsetOfContentAsJson = Json.obj(
    "userId"  -> userId2,
    "doneBy"  -> Json.obj(
      "userId" -> userId2,
      "role"   -> "USER"
    ),
    "version" -> 2,
    "groups"  -> Json.arr(
      Json.obj(
        "key"      -> "maifNotifs",
        "label"    -> "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        "consents" -> Json.arr(
          Json.obj("key" -> "sms", "label" -> "Par SMS / MMS / VMS", "checked" -> false)
        )
      ),
      Json.obj(
        "key"      -> "partenaireNotifs",
        "label"    -> "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        "consents" -> Json.arr(
          Json.obj("key" -> "mail", "label" -> "Par contact électronique", "checked" -> false),
          Json.obj("key" -> "sms", "label" -> "Par SMS / MMS / VMS", "checked" -> false)
        )
      )
    )
  )

  val user5InvalidContentWithUnknownKeyAsJson = Json.obj(
    "userId"  -> userId5,
    "doneBy"  -> Json.obj(
      "userId" -> userId5,
      "role"   -> "USER"
    ),
    "version" -> 2,
    "groups"  -> Json.arr(
      Json.obj(
        "key"      -> "maifNotifs",
        "label"    -> "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        "consents" -> Json.arr(
          Json.obj("key" -> "sms", "label" -> "Par SMS / MMS / VMS", "checked" -> false),
          Json.obj("key" -> "fax", "label" -> "Par FAX", "checked" -> false)
        )
      ),
      Json.obj(
        "key"      -> "partenaireNotifs",
        "label"    -> "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        "consents" -> Json.arr(
          Json.obj("key" -> "mail", "label" -> "Par contact électronique", "checked" -> false),
          Json.obj("key" -> "sms", "label" -> "Par SMS / MMS / VMS", "checked" -> false)
        )
      )
    )
  )


  val user6 = ConsentFact(
    userId = userId6,
    doneBy = DoneBy(userId = userId6, role = "USER"),
    version = 2,
    groups = Seq(
      ConsentGroup(
        key = "maifNotifs",
        label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        consents = Seq(
          Consent(key = "phone", label = "Par contact téléphonique", checked = true),
          Consent(key = "mail", label = "Par contact électronique", checked = false),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
        )
      ),
      ConsentGroup(
        key = "partenaireNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        consents = Seq(
          Consent(key = "phone", label = "Par contact téléphonique", checked = false),
          Consent(key = "mail", label = "Par contact électronique", checked = true),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
        )
      )
    )
  )
  val user6AsJson = user6.asJson()

  val user6Modified = user6.copy(groups = user6.groups.take(1).map(g => g.copy(consents = g.consents.take(1))))
  val user6ModifiedAsJson = user6Modified.asJson()

  private def offerToConsentOffer(offer: Offer): ConsentOffer =
    ConsentOffer(
      key = offer.key,
      label = offer.label,
      version = offer.version,
      groups = offer.groups.map(group =>
        ConsentGroup(group.key, group.label, group.permissions.map(perm => Consent(perm.key, perm.label, false)))
      )
    )

  private val offerKey1     = "offer1"
  private val offer1: Offer = Offer(
    key = offerKey1,
    label = "offer one",
    groups = Seq(
      PermissionGroup(
        key = "maifNotifs",
        label = "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF",
        permissions = Seq(
          Permission(key = "phone", label = "Par contact téléphonique"),
          Permission(key = "mail", label = "Par contact électronique"),
          Permission(key = "sms", label = "Par SMS / MMS / VMS")
        )
      )
    )
  )

  private val offerKey2              = "offer2"
  private val offer2: Offer          = Offer(
    key = offerKey2,
    label = "offer two",
    groups = Seq(
      PermissionGroup(
        key = "maifPartnerNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales des partenaires du groupe MAIF",
        permissions = Seq(
          Permission(key = "phone", label = "Par contact téléphonique"),
          Permission(key = "mail", label = "Par contact électronique"),
          Permission(key = "sms", label = "Par SMS / MMS / VMS")
        )
      )
    )
  )
  private val offer2V2OnError: Offer =
    offer2.copy(version = 2, label = "offer 2")
  private val offer2V3OnError: Offer =
    offer2.copy(version = 3, label = "offer 2")

  private val offerKeyNotAuthorized     = "offerTwo"
  private val offerNotAuthorized: Offer = Offer(
    key = offerKeyNotAuthorized,
    label = "offer not authorized",
    groups = Seq(
      PermissionGroup(
        key = "offerGrp1",
        label = "offer group one",
        permissions = Seq(Permission("sms", "Please accept sms"))
      )
    )
  )

  "ConsentController" should {
    val organisationKey: String = "maif"

    "user not exist" in {
      val path: String =
        s"/$tenant/organisations/$organisationKey/users/$userId1"
      val response     = getJson(path)

      response.status mustBe NOT_FOUND
    }

    "get user consents template" in {
      val response =
        getJson(s"/$tenant/organisations/$organisationKey/users/_template")

      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "userId").as[String] mustBe "fill"
      (value \ "doneBy" \ "userId").as[String] mustBe "fill"
      (value \ "doneBy" \ "role").as[String] mustBe "fill"
      (value \ "version").as[Int] mustBe 2

      val groups: JsArray = (value \ "groups").as[JsArray]

      groups.value.size mustBe 2

      (groups \ 0 \ "key").as[String] mustBe "maifNotifs"
      (groups \ 0 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

      val consents1: JsArray = (groups \ 0 \ "consents").as[JsArray]

      consents1.value.size mustBe 3

      (consents1 \ 0 \ "key").as[String] mustBe "phone"
      (consents1 \ 0 \ "label").as[String] mustBe "Par contact téléphonique"
      (consents1 \ 0 \ "checked").as[Boolean] mustBe false

      (consents1 \ 1 \ "key").as[String] mustBe "mail"
      (consents1 \ 1 \ "label").as[String] mustBe "Par contact électronique"
      (consents1 \ 1 \ "checked").as[Boolean] mustBe false

      (consents1 \ 2 \ "key").as[String] mustBe "sms"
      (consents1 \ 2 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (consents1 \ 2 \ "checked").as[Boolean] mustBe false

      (groups \ 1 \ "key").as[String] mustBe "partenaireNotifs"
      (groups \ 1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF"

      val consents2: JsArray = (groups \ 1 \ "consents").as[JsArray]

      consents2.value.size mustBe 3

      (consents2 \ 0 \ "key").as[String] mustBe "phone"
      (consents2 \ 0 \ "label").as[String] mustBe "Par contact téléphonique"
      (consents2 \ 0 \ "checked").as[Boolean] mustBe false

      (consents2 \ 1 \ "key").as[String] mustBe "mail"
      (consents2 \ 1 \ "label").as[String] mustBe "Par contact électronique"
      (consents2 \ 1 \ "checked").as[Boolean] mustBe false

      (consents2 \ 2 \ "key").as[String] mustBe "sms"
      (consents2 \ 2 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (consents2 \ 2 \ "checked").as[Boolean] mustBe false
    }

    "create user consents" in {
      val path: String =
        s"/$tenant/organisations/$organisationKey/users/$userId1"
      val putResponse  = putJson(path, user1AsJson)

      putResponse.status mustBe OK

      val putValue: JsValue = putResponse.json

      (putValue \ "userId").as[String] mustBe user1.userId
      (putValue \ "doneBy" \ "userId").as[String] mustBe user1.doneBy.userId
      (putValue \ "doneBy" \ "role").as[String] mustBe user1.doneBy.role
      (putValue \ "version").as[Int] mustBe user1.version

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "ConsentFactCreated"
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId1
      (msgAsJson \ "metadata" \ "foo").as[String] mustBe "bar"
      (msgAsJson \ "metadata" \ "foo2").as[String] mustBe "bar2"

      val response = getJson(path)

      val value: JsValue = response.json

      (value \ "userId").as[String] mustBe user1.userId
      (value \ "doneBy" \ "userId").as[String] mustBe user1.doneBy.userId
      (value \ "doneBy" \ "role").as[String] mustBe user1.doneBy.role
      (value \ "version").as[Int] mustBe user1.version

      val groups: JsArray = (value \ "groups").as[JsArray]

      groups.value.size mustBe 2

      (groups \ 0 \ "key").as[String] mustBe user1.groups.head.key
      (groups \ 0 \ "label").as[String] mustBe user1.groups.head.label

      val consents1: JsArray = (groups \ 0 \ "consents").as[JsArray]

      consents1.value.size mustBe 3

      (consents1 \ 0 \ "key").as[String] mustBe "phone"
      (consents1 \ 0 \ "label").as[String] mustBe "Par contact téléphonique"
      (consents1 \ 0 \ "checked").as[Boolean] mustBe true

      (consents1 \ 1 \ "key").as[String] mustBe "mail"
      (consents1 \ 1 \ "label").as[String] mustBe "Par contact électronique"
      (consents1 \ 1 \ "checked").as[Boolean] mustBe false

      (consents1 \ 2 \ "key").as[String] mustBe "sms"
      (consents1 \ 2 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (consents1 \ 2 \ "checked").as[Boolean] mustBe true

      (groups \ 1 \ "key").as[String] mustBe user1.groups(1).key
      (groups \ 1 \ "label").as[String] mustBe user1.groups(1).label

      val consents2: JsArray = (groups \ 1 \ "consents").as[JsArray]

      consents2.value.size mustBe 3

      (consents2 \ 0 \ "key").as[String] mustBe "phone"
      (consents2 \ 0 \ "label").as[String] mustBe "Par contact téléphonique"
      (consents2 \ 0 \ "checked").as[Boolean] mustBe false

      (consents2 \ 1 \ "key").as[String] mustBe "mail"
      (consents2 \ 1 \ "label").as[String] mustBe "Par contact électronique"
      (consents2 \ 1 \ "checked").as[Boolean] mustBe true

      (consents2 \ 2 \ "key").as[String] mustBe "sms"
      (consents2 \ 2 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (consents2 \ 2 \ "checked").as[Boolean] mustBe false
    }

    "update user consents" in {
      val path: String =
        s"/$tenant/organisations/$organisationKey/users/$userId1"
      val putResponse  = putJson(path, user1ModifiedAsJson)

      putResponse.status mustBe OK

      val putValue: JsValue = putResponse.json

      (putValue \ "userId").as[String] mustBe userId1
      (putValue \ "orgKey").as[String] mustBe organisationKey
      (putValue \ "doneBy" \ "userId").as[String] mustBe user1Modified.doneBy.userId
      (putValue \ "doneBy" \ "role").as[String] mustBe user1Modified.doneBy.role
      (putValue \ "version").as[Int] mustBe user1Modified.version

      val msgAsJson = readLastKafkaEvent()

      (msgAsJson \ "type").as[String] mustBe "ConsentFactUpdated"
      (msgAsJson \ "oldValue" \ "doneBy" \ "role")
        .as[String] mustBe user1.doneBy.role
      (msgAsJson \ "payload" \ "doneBy" \ "role")
        .as[String] mustBe user1Modified.doneBy.role
      (msgAsJson \ "metadata" \ "foo").as[String] mustBe "bar"
      (msgAsJson \ "metadata" \ "foo2").as[String] mustBe "bar2"

      val response = getJson(path)

      val value: JsValue = response.json

      (value \ "userId").as[String] mustBe userId1
      (value \ "orgKey").as[String] mustBe organisationKey
      (value \ "doneBy" \ "userId").as[String] mustBe user1Modified.doneBy.userId
      (value \ "doneBy" \ "role").as[String] mustBe user1Modified.doneBy.role
      (value \ "version").as[Int] mustBe user1Modified.version

      val groups: JsArray = (value \ "groups").as[JsArray]

      groups.value.size mustBe user1Modified.groups.size

      (groups \ 0 \ "key").as[String] mustBe "maifNotifs"
      (groups \ 0 \ "label").as[String] mustBe
      "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

      val consents1: JsArray = (groups \ 0 \ "consents").as[JsArray]

      consents1.value.size mustBe user1Modified.groups.head.consents.size

      (consents1 \ 0 \ "key")
        .as[String] mustBe user1Modified.groups.head.consents.head.key
      (consents1 \ 0 \ "label")
        .as[String] mustBe user1Modified.groups.head.consents.head.label
      (consents1 \ 0 \ "checked")
        .as[Boolean] mustBe user1Modified.groups.head.consents.head.checked

      (consents1 \ 1 \ "key")
        .as[String] mustBe user1Modified.groups.head.consents(1).key
      (consents1 \ 1 \ "label")
        .as[String] mustBe user1Modified.groups.head.consents(1).label
      (consents1 \ 1 \ "checked")
        .as[Boolean] mustBe user1Modified.groups.head.consents(1).checked

      (consents1 \ 2 \ "key")
        .as[String] mustBe user1Modified.groups.head.consents(2).key
      (consents1 \ 2 \ "label")
        .as[String] mustBe user1Modified.groups.head.consents(2).label
      (consents1 \ 2 \ "checked")
        .as[Boolean] mustBe user1Modified.groups.head.consents(2).checked

      (groups \ 1 \ "key").as[String] mustBe user1Modified.groups(1).key
      (groups \ 1 \ "label").as[String] mustBe user1Modified.groups(1).label

      val consents2: JsArray = (groups \ 1 \ "consents").as[JsArray]

      consents2.value.size mustBe user1Modified.groups(1).consents.size

      (consents2 \ 0 \ "key")
        .as[String] mustBe user1Modified.groups(1).consents.head.key
      (consents2 \ 0 \ "label")
        .as[String] mustBe user1Modified.groups(1).consents.head.label
      (consents2 \ 0 \ "checked")
        .as[Boolean] mustBe user1Modified.groups(1).consents.head.checked

      (consents2 \ 1 \ "key")
        .as[String] mustBe user1Modified.groups(1).consents(1).key
      (consents2 \ 1 \ "label")
        .as[String] mustBe user1Modified.groups(1).consents(1).label
      (consents2 \ 1 \ "checked")
        .as[Boolean] mustBe user1Modified.groups(1).consents(1).checked

      (consents2 \ 2 \ "key")
        .as[String] mustBe user1Modified.groups(1).consents(2).key
      (consents2 \ 2 \ "label")
        .as[String] mustBe user1Modified.groups(1).consents(2).label
      (consents2 \ 2 \ "checked")
        .as[Boolean] mustBe user1Modified.groups(1).consents(2).checked
    }

    "update user with a subset of consents compare to organisation version" in {
      val response = putJson(
        s"/$tenant/organisations/$organisationKey/users/$userId2",
        user2SubsetOfContentAsJson
      )
      response.status mustBe OK
    }

    "update user removing data should fail" in {
      putJson(
        s"/$tenant/organisations/$organisationKey/users/$userId6",
        user6AsJson
      ).status mustBe OK

      val response = putJson(
        s"/$tenant/organisations/$organisationKey/users/$userId6",
        user6ModifiedAsJson
      )
      response.status mustBe BAD_REQUEST
      response.json mustBe Json.parse("""
       {
          "errors":[
            {
              "message":"error.invalid.consent.missing",
              "args":["mail"]
            },{
              "message":"error.invalid.consent.missing",
              "args":["sms"]
            },
            {
              "message":"error.invalid.group.missing",
              "args":["partenaireNotifs"]}],
              "fieldErrors":{}
            }
      """)
      println(response.json)

    }

    "update user with invalid consents compare to organisation version" in {
      val response = putJson(
        s"/$tenant/organisations/$organisationKey/users/$userId5",
        user5InvalidContentWithUnknownKeyAsJson
      )
      response.status mustBe BAD_REQUEST

      println(response.json)
      response.json mustBe Json.parse("""{"errors":[{"message":"error.invalid.consent.key","args":["fax"]}],"fieldErrors":{}}""")
    }

    "get consents history" in {
      val path: String =
        s"/$tenant/organisations/$organisationKey/users/$userId3"
      putJson(path, user3AsJson).status mustBe OK
      val msg1         = readLastKafkaEvent()
      (msg1 \ "type").as[String] mustBe "ConsentFactCreated"
      putJson(path, user3AsJson).status mustBe OK
      val msg2         = readLastKafkaEvent()
      (msg2 \ "type").as[String] mustBe "ConsentFactUpdated"
      putJson(path, user3AsJson).status mustBe OK
      val msg3         = readLastKafkaEvent()
      (msg3 \ "type").as[String] mustBe "ConsentFactUpdated"
      putJson(path, user3AsJson).status mustBe OK
      val msg4         = readLastKafkaEvent()
      (msg4 \ "type").as[String] mustBe "ConsentFactUpdated"
      putJson(path, user3AsJson).status mustBe OK
      val msg5         = readLastKafkaEvent()
      (msg5 \ "type").as[String] mustBe "ConsentFactUpdated"

      val historyPath: String = s"$path/logs?page=0&pageSize=10"

      val response = getJson(historyPath)

      response.status mustBe OK

      val value: JsValue = response.json

      val items: JsArray = (value \ "items").as[JsArray]
      items.value.size mustBe 5
      (value \ "count").as[Int] mustBe 5
      (value \ "page").as[Int] mustBe 0
      (value \ "pageSize").as[Int] mustBe 10

      // as xml
      val resp = getXml(historyPath)
      resp.status mustBe OK

      val xmlValue = resp.xml
      (xmlValue \ "count").head.text mustBe "5"
      (xmlValue \ "page").head.text mustBe "0"
      (xmlValue \ "pageSize").head.text mustBe "10"
      val itemsXml = (xmlValue \ "items").head
      itemsXml.child.size mustBe 5
    }

    "get template as XML" in {
      val resp =
        getXml(s"/$tenant/organisations/$organisationKey/users/_template")

      resp.status mustBe OK

      resp.contentType.contains("xml") mustBe true

      val xmlValue = resp.xml

      (xmlValue \ "userId").head.text mustBe "fill"
      (xmlValue \ "doneBy" \ "userId").head.text mustBe "fill"
      (xmlValue \ "doneBy" \ "role").head.text mustBe "fill"
    }

    "create user consents as XML" in {
      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId4,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          )
        ),
        lastUpdate = DateTime.now(DateTimeZone.UTC)
      )

      val consentFactasXml = consentFact.asXml()

      val resp =
        putXml(s"/$tenant/organisations/$organisationKey/users/$userId4", consentFactasXml)

      resp.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "ConsentFactCreated"
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId4
    }

    "create user with invalid userId in json" in {
      val path = s"/$tenant/organisations/$organisationKey/users/userIdInvalid"

      val response = putJson(path, user1AsJson)

      response.status mustBe BAD_REQUEST
    }

    "force lastUpdate date" in {
      val tomorrow: DateTime = DateTime.now(DateTimeZone.UTC).plusDays(1)

      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId4,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          )
        ),
        lastUpdate = tomorrow
      )

      val resp =
        putJson(s"/$tenant/organisations/$organisationKey/users/$userId4", consentFact.asJson())

      resp.status mustBe OK

      val json: JsValue = resp.json

      (json \ "lastUpdate").as[String] mustBe tomorrow.toString(DateUtils.utcDateFormatter)

      val respGet =
        getJson(s"/$tenant/organisations/$organisationKey/users/$userId4")

      respGet.status mustBe OK

      val jsonGet: JsValue = respGet.json

      (jsonGet \ "lastUpdate").as[String] mustBe tomorrow.toString(DateUtils.utcDateFormatter)
    }

    "not force update date" in {
      val userId5     = "userId5"
      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId5,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          )
        )
      )

      val resp =
        putJson(s"/$tenant/organisations/$organisationKey/users/$userId5", consentFact.asJson())

      resp.status mustBe OK

      val json: JsValue = resp.json

      (json \ "lastUpdate").as[String] must not be null
    }

    "force lastUpdate date xml" in {
      val yesterday: DateTime = DateTime.now(DateTimeZone.UTC).minusDays(1)

      val userId6 = "userId6Xml"

      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId6,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(
              Consent("phone", "Par contact téléphonique", false),
              Consent("mail", "Par contact électronique", false),
              Consent("sms", "Par SMS / MMS / VMS", false)
            )
          )
        ),
        lastUpdate = yesterday
      )

      val resp =
        putXml(s"/$tenant/organisations/$organisationKey/users/$userId6", consentFact.asXml())

      resp.status mustBe OK

      val xml = resp.xml

      (xml \ "lastUpdate").head.text mustBe yesterday.toString(DateUtils.utcDateFormatter)

      val respGet =
        getXml(s"/$tenant/organisations/$organisationKey/users/$userId6")

      respGet.status mustBe OK

      val xmlGet = respGet.xml

      (xmlGet \ "lastUpdate").head.text mustBe yesterday.toString(DateUtils.utcDateFormatter)
    }

  }

  "Consent with offer" should {
    val orgKey: String = "orgWithOffer"

    val orgWithOffer: Organisation = Organisation(
      key = orgKey,
      label = "organisation with offer",
      groups = Seq(
        PermissionGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          permissions = Seq(
            Permission(key = "phone", label = "Par contact téléphonique"),
            Permission(key = "mail", label = "Par contact électronique"),
            Permission(key = "sms", label = "Par SMS / MMS / VMS")
          )
        )
      )
    )

    val userId               = "userIdOffer"
    val consent: ConsentFact = ConsentFact(
      userId = userId,
      doneBy = DoneBy(userId = userId, role = "USER"),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          consents = Seq(
            Consent(key = "phone", label = "Par contact téléphonique", checked = true),
            Consent(key = "mail", label = "Par contact électronique", checked = false),
            Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
          )
        )
      ),
      offers = Some(
        Seq(
          ConsentOffer(
            key = "offer1",
            label = "offer one",
            version = 1,
            groups = Seq(
              ConsentGroup(
                key = "maifNotifs",
                label = "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF",
                consents = Seq(
                  Consent(key = "phone", label = "Par contact téléphonique", checked = false),
                  Consent(key = "mail", label = "Par contact électronique", checked = true),
                  Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
                )
              )
            )
          ),
          ConsentOffer(
            key = "offer2",
            label = "offer two",
            version = 2,
            groups = Seq(
              ConsentGroup(
                key = "maifPartnerNotifs",
                label =
                  "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales des partenaires du groupe MAIF",
                consents = Seq(
                  Consent(key = "phone", label = "Par contact téléphonique", checked = true),
                  Consent(key = "mail", label = "Par contact électronique", checked = true),
                  Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
                )
              )
            )
          )
        )
      )
    )

    val basicConsentFact: ConsentFact = ConsentFact(
      userId = userId,
      doneBy = DoneBy(userId = userId, role = "USER"),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          consents = Seq(
            Consent(key = "phone", label = "Par contact téléphonique", checked = true),
            Consent(key = "mail", label = "Par contact électronique", checked = false),
            Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
          )
        )
      )
    )

    "create organisation, released, add offer" in {
      postJson(s"/$tenant/organisations", orgWithOffer.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", Json.obj()).status mustBe OK

      // offer 1 with version 1
      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED

      // offer 2 with version 2
      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED
      putJson(s"/$tenant/organisations/$orgKey/offers/${offer2.key}", offer2.asJson()).status mustBe OK
    }

    "add offer" in {
      putJson(s"/$tenant/organisations/$orgKey/users/$userId", consent.asJson()).status mustBe OK

      val response: WSResponse =
        getJson(s"/$tenant/organisations/$orgKey/users/$userId")

      response.status mustBe OK

      val value: JsValue = response.json

      val offers: JsArray = (value \ "offers").as[JsArray]
      offers.value.length mustBe 2

      (offers \ 0 \ "key").as[String] mustBe "offer1"
      (offers \ 0 \ "label").as[String] mustBe "offer one"
      (offers \ 0 \ "version").as[Int] mustBe 1
      (offers \ 0 \ "groups").as[JsArray].value.length mustBe 1
      val offer1group1    = (offers \ 0 \ "groups" \ 0).as[JsValue]

      (offer1group1 \ "key").as[String] mustBe "maifNotifs"
      (offer1group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF"

      (offer1group1 \ "consents").as[JsArray].value.length mustBe 3

      val offer1group1perm1 = (offer1group1 \ "consents" \ 0).as[JsValue]

      (offer1group1perm1 \ "key").as[String] mustBe "phone"
      (offer1group1perm1 \ "label").as[String] mustBe "Par contact téléphonique"
      (offer1group1perm1 \ "checked").as[Boolean] mustBe false

      val offer1group1perm2 = (offer1group1 \ "consents" \ 1).as[JsValue]

      (offer1group1perm2 \ "key").as[String] mustBe "mail"
      (offer1group1perm2 \ "label").as[String] mustBe "Par contact électronique"
      (offer1group1perm2 \ "checked").as[Boolean] mustBe true

      val offer1group1perm3 = (offer1group1 \ "consents" \ 2).as[JsValue]

      (offer1group1perm3 \ "key").as[String] mustBe "sms"
      (offer1group1perm3 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (offer1group1perm3 \ "checked").as[Boolean] mustBe false

      (offers \ 1 \ "key").as[String] mustBe "offer2"
      (offers \ 1 \ "label").as[String] mustBe "offer two"
      (offers \ 1 \ "version").as[Int] mustBe 2
      (offers \ 1 \ "groups").as[JsArray].value.length mustBe 1
      val offer2group1 = (offers \ 1 \ "groups" \ 0).as[JsValue]

      (offer2group1 \ "key").as[String] mustBe "maifPartnerNotifs"
      (offer2group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales des partenaires du groupe MAIF"

      (offer2group1 \ "consents").as[JsArray].value.length mustBe 3

      val offer2group1perm1 = (offer2group1 \ "consents" \ 0).as[JsValue]

      (offer2group1perm1 \ "key").as[String] mustBe "phone"
      (offer2group1perm1 \ "label").as[String] mustBe "Par contact téléphonique"
      (offer2group1perm1 \ "checked").as[Boolean] mustBe true

      val offer2group1perm2 = (offer2group1 \ "consents" \ 1).as[JsValue]

      (offer2group1perm2 \ "key").as[String] mustBe "mail"
      (offer2group1perm2 \ "label").as[String] mustBe "Par contact électronique"
      (offer2group1perm2 \ "checked").as[Boolean] mustBe true

      val offer2group1perm3 = (offer2group1 \ "consents" \ 2).as[JsValue]

      (offer2group1perm3 \ "key").as[String] mustBe "sms"
      (offer2group1perm3 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (offer2group1perm3 \ "checked").as[Boolean] mustBe false

    }

    "version consent >  version org on error" in {
      // offer 2 version 3
      putJson(s"/$tenant/organisations/$orgKey/offers/${offer2.key}", offer2.asJson()).status mustBe OK

      val consentWithVersionHighThanOrg: ConsentFact =
        basicConsentFact.copy(offers = Some(Seq(offerToConsentOffer(offer2.copy(version = 10)))))

      val putErrorResponse: WSResponse =
        putJson(s"/$tenant/organisations/$orgKey/users/$userId", consentWithVersionHighThanOrg.asJson())
      putErrorResponse.status mustBe BAD_REQUEST

    }

    "version consent 1 < version org 3 => version consent 1 < version  lastconsent 2" in {
      val consentWithVersionLowThanOrgAndLastCons: ConsentFact =
        basicConsentFact.copy(offers = Some(Seq(offerToConsentOffer(offer2).copy(version = 1))))
      putJson(
        s"/$tenant/organisations/$orgKey/users/$userId",
        consentWithVersionLowThanOrgAndLastCons.asJson()
      ).status mustBe BAD_REQUEST
    }

    "version consent 2 <  version org 3 => version consent 2 = version  lastconsent 2 ==> struct KO" in {
      val consentWithVersionLowThanOrgAndEqualToLastConsAndStrucDif: ConsentFact =
        basicConsentFact.copy(offers = Some(Seq(offerToConsentOffer(offer2V2OnError))))
      val putErrorResponse: WSResponse                                           = putJson(
        s"/$tenant/organisations/$orgKey/users/$userId",
        consentWithVersionLowThanOrgAndEqualToLastConsAndStrucDif.asJson()
      )
      putErrorResponse.status mustBe BAD_REQUEST
    }

    "version consent =  version org ==> 200  si structure OK / 400 si pas OK" in {
      val consentWithVersionEqualToOrgAndStrucEq: ConsentFact =
        basicConsentFact.copy(offers = Some(Seq(offerToConsentOffer(offer2).copy(version = 3))))

      putJson(
        s"/$tenant/organisations/$orgKey/users/$userId",
        consentWithVersionEqualToOrgAndStrucEq.asJson()
      ).status mustBe OK

      val consentWithVersionEqualToOrgAndStrucDif: ConsentFact =
        basicConsentFact.copy(offers = Some(Seq(offerToConsentOffer(offer2V3OnError))))
      val putErrorResponse: WSResponse                         =
        putJson(s"/$tenant/organisations/$orgKey/users/$userId", consentWithVersionEqualToOrgAndStrucDif.asJson())
      putErrorResponse.status mustBe BAD_REQUEST
    }

    "version consent 2 <  version org 3 => version consent 2 = version lastconsent 2 ==> struct OK" in {
      val consentWithVersionLowThanOrgAndEqualToLastConsAndStrucEq: ConsentFact =
        basicConsentFact.copy(offers = Some(Seq(offerToConsentOffer(offer2).copy(version = 3))))

      // Update offer version
      val updateOfferResponse: WSResponse =
        putJson(s"/$tenant/organisations/$orgKey/offers/${offer2.key}", offer2.asJson())
      updateOfferResponse.status mustBe OK

      val putErrorResponse: WSResponse =
        putJson(
          s"/$tenant/organisations/$orgKey/users/$userId",
          consentWithVersionLowThanOrgAndEqualToLastConsAndStrucEq.asJson()
        )
      putErrorResponse.status mustBe OK
    }

    "delete offer" in {
      val orgKey: String = "orgWithOfferToDelete"

      val orgWithOfferToDelete: Organisation = Organisation(
        key = orgKey,
        label = "organisation with offer",
        groups = Seq(
          PermissionGroup(
            key = "maifNotifs",
            label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            permissions = Seq(
              Permission(key = "phone", label = "Par contact téléphonique"),
              Permission(key = "mail", label = "Par contact électronique"),
              Permission(key = "sms", label = "Par SMS / MMS / VMS")
            )
          )
        )
      )

      postJson(s"/$tenant/organisations", orgWithOfferToDelete.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", Json.obj()).status mustBe OK

      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED

      val userId                 = "deleteUser"
      val consent: ConsentFact   = ConsentFact(
        userId = userId,
        doneBy = DoneBy(userId = userId, role = "USER"),
        version = 1,
        groups = Seq(
          ConsentGroup(
            key = "maifNotifs",
            label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            consents = Seq(
              Consent(key = "phone", label = "Par contact téléphonique", checked = true),
              Consent(key = "mail", label = "Par contact électronique", checked = false),
              Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
            )
          )
        ),
        offers = Some(
          Seq(
            ConsentOffer(
              key = "offer1",
              label = "offer one",
              version = 1,
              groups = Seq(
                ConsentGroup(
                  key = "maifNotifs",
                  label = "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF",
                  consents = Seq(
                    Consent(key = "phone", label = "Par contact téléphonique", checked = false),
                    Consent(key = "mail", label = "Par contact électronique", checked = true),
                    Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
                  )
                )
              )
            ),
            ConsentOffer(
              key = "offer2",
              label = "offer two",
              version = 1,
              groups = Seq(
                ConsentGroup(
                  key = "maifPartnerNotifs",
                  label =
                    "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales des partenaires du groupe MAIF",
                  consents = Seq(
                    Consent(key = "phone", label = "Par contact téléphonique", checked = true),
                    Consent(key = "mail", label = "Par contact électronique", checked = true),
                    Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
                  )
                )
              )
            )
          )
        )
      )
      putJson(s"/$tenant/organisations/$orgKey/users/$userId", consent.asJson()).status mustBe OK
      val response: WSResponse   =
        getJson(s"/$tenant/organisations/$orgKey/users/$userId")
      response.status mustBe OK

      (response.json \ "offers").as[JsArray].value.length mustBe 2
      val msgAsJson              = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "ConsentFactCreated"
      (msgAsJson \ "payload" \ "userId").as[String] mustBe consent.userId
      val payloadOffers: JsArray =
        (msgAsJson \ "payload" \ "offers").as[JsArray]
      payloadOffers.value.size mustBe 2
      (payloadOffers \ 0 \ "key")
        .as[String] mustBe "offer1"
      (payloadOffers \ 1 \ "key")
        .as[String] mustBe "offer2"

      delete(s"/$tenant/organisations/$orgKey/users/$userId/offers/$offerKeyNotAuthorized").status mustBe UNAUTHORIZED
      delete(s"/$tenant/organisations/$orgKey/users/$userId/offers/$offerKey1").status mustBe OK

      val deleteResponse =
        getJson(s"/$tenant/organisations/$orgKey/users/$userId")
      deleteResponse.status mustBe OK
      val value: JsValue = deleteResponse.json

      val offers: JsArray = (value \ "offers").as[JsArray]
      offers.value.length mustBe 1

      (offers \ 0 \ "key").as[String] mustBe "offer2"
      (offers \ 0 \ "label").as[String] mustBe "offer two"

      val msgAsJsonAfterDelete    = readLastKafkaEvent()
      (msgAsJsonAfterDelete \ "type").as[String] mustBe "ConsentFactUpdated"
      (msgAsJsonAfterDelete \ "payload" \ "userId")
        .as[String] mustBe consent.userId
      val payloadOffers2: JsArray =
        (msgAsJsonAfterDelete \ "payload" \ "offers").as[JsArray]
      payloadOffers2.value.size mustBe 1
      (payloadOffers2 \ 0 \ "key")
        .as[String] mustBe "offer2"

      delete(s"/$tenant/organisations/$orgKey/users/$userId/offers/$offerKey1").status mustBe NOT_FOUND
    }

    "save consent fact with an offers array empty" in {
      val consent: ConsentFact = ConsentFact(
        userId = userId,
        doneBy = DoneBy(userId = userId, role = "USER"),
        version = 1,
        groups = Seq(
          ConsentGroup(
            key = "maifNotifs",
            label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            consents = Seq(
              Consent(key = "phone", label = "Par contact téléphonique", checked = true),
              Consent(key = "mail", label = "Par contact électronique", checked = false),
              Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
            )
          )
        ),
        offers = Some(
          Seq()
        )
      )
      val response: WSResponse =
        putJson(s"/$tenant/organisations/$orgKey/users/$userId", consent.asJson())

      NioLogger.info(response.body)
      response.status mustBe OK
    }

    "restricted with pattern" in {
      val consentFact = ConsentFact(
        _id = "cf",
        userId = "userId4",
        doneBy = DoneBy("a1", "admin"),
        version = 1,
        groups = Seq(
          ConsentGroup("a", "a", Seq(Consent("a", "a", false))),
          ConsentGroup("b", "b", Seq(Consent("b", "b", false)))
        ),
        lastUpdate = DateTime.now(DateTimeZone.UTC),
        metaData = Some(Map("mdKey1" -> "mdVal1", "tata" -> "val2"))
      )

      val consentFactWithOffers = consentFact
        .copy(
          offers = Some(
            Seq(
              ConsentOffer(
                key = "offer1",
                label = "offer 1",
                version = 1,
                groups = Seq(
                  ConsentGroup("a", "a", Seq(Consent("a", "a", false))),
                  ConsentGroup("b", "b", Seq(Consent("b", "b", false)))
                )
              ),
              ConsentOffer(
                key = "offer2",
                label = "offer 2",
                version = 1,
                groups = Seq(
                  ConsentGroup("a", "a", Seq(Consent("a", "a", false))),
                  ConsentGroup("b", "b", Seq(Consent("b", "b", false)))
                )
              )
            )
          )
        )
      val consentFactWithOffer1 = consentFact
        .copy(
          offers = Some(
            Seq(
              ConsentOffer(
                key = "offer1",
                label = "offer 1",
                version = 1,
                groups = Seq(
                  ConsentGroup("a", "a", Seq(Consent("a", "a", false))),
                  ConsentGroup("b", "b", Seq(Consent("b", "b", false)))
                )
              )
            )
          )
        )

      consentManagerService
        .consentFactWithAccessibleOffers(consentFactWithOffers, Some(Seq(".*")))
        .asJson() mustBe consentFactWithOffers.asJson()
      consentManagerService
        .consentFactWithAccessibleOffers(consentFactWithOffers, Some(Seq("offer1")))
        .asJson() mustBe consentFactWithOffer1.asJson()
      consentManagerService
        .consentFactWithAccessibleOffers(consentFactWithOffers, Some(Seq("otherOffer")))
        .offers
        .getOrElse(Seq())
        .length mustBe 0
    }
  }

  "last update date must be >= last update date in db" should {
    val lastUpdate = DateTime.now(DateTimeZone.UTC)

    val userIdDate  = "userIdWithLastUpdateDate"
    val consentFact = ConsentFact(
      _id = "cf",
      userId = userIdDate,
      doneBy = DoneBy("a1", "admin"),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          consents = Seq(
            Consent(key = "phone", label = "Par contact téléphonique", checked = true),
            Consent(key = "mail", label = "Par contact électronique", checked = false),
            Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
          )
        )
      ),
      lastUpdate = lastUpdate,
      metaData = Some(Map("mdKey1" -> "mdVal1", "tata" -> "val2"))
    )

    val orgKey: String = "organisationForLastUpdate"

    val organisation: Organisation = Organisation(
      key = orgKey,
      label = "organisation",
      groups = Seq(
        PermissionGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          permissions = Seq(
            Permission(key = "phone", label = "Par contact téléphonique"),
            Permission(key = "mail", label = "Par contact électronique"),
            Permission(key = "sms", label = "Par SMS / MMS / VMS")
          )
        )
      )
    )

    "initialize organisation" in {
      postJson(s"/$tenant/organisations", organisation.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", organisation.asJson()).status mustBe OK

      // insert consent fact on db
      putJson(s"/$tenant/organisations/$orgKey/users/$userIdDate", consentFact.asJson()).status mustBe OK
    }

    "case last update date in body < last update date in db" in {
      val response =
        putJson(
          s"/$tenant/organisations/$orgKey/users/$userIdDate",
          consentFact.copy(lastUpdate = lastUpdate.minusDays(1)).asJson()
        )
      response.status mustBe CONFLICT

      (response.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "the.specified.update.date.must.be.greater.than.the.saved.update.date"
    }

    "case last update date in body = last update date in db" in {
      putJson(s"/$tenant/organisations/$orgKey/users/$userIdDate", consentFact.asJson()).status mustBe OK
    }

    "case last update date in body > last update date in db" in {
      putJson(
        s"/$tenant/organisations/$orgKey/users/$userIdDate",
        consentFact
          .copy(lastUpdate = lastUpdate.plusDays(1))
          .asJson()
      ).status mustBe OK
    }
  }

  "last update offer date must be >= last update offer date in db" should {
    val today     = DateTime.now(DateTimeZone.UTC)
    val yesterday = DateTime.now(DateTimeZone.UTC).minusDays(1)
    val tommorrow = DateTime.now(DateTimeZone.UTC).plusDays(1)

    val userIdDate  = "userIdWithLastUpdateDate"
    val consentFact = ConsentFact(
      _id = "cf",
      userId = userIdDate,
      doneBy = DoneBy("a1", "admin"),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          consents = Seq(
            Consent(key = "phone", label = "Par contact téléphonique", checked = true),
            Consent(key = "mail", label = "Par contact électronique", checked = false),
            Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
          )
        )
      ),
      offers = None,
      lastUpdate = today,
      metaData = Some(Map("mdKey1" -> "mdVal1", "tata" -> "val2"))
    )

    val consentOffer1: ConsentOffer = offerToConsentOffer(offer1)

    val orgKey: String = "organisationForLastUpdateOffer"

    val organisation: Organisation = Organisation(
      key = orgKey,
      label = "organisation",
      groups = Seq(
        PermissionGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          permissions = Seq(
            Permission(key = "phone", label = "Par contact téléphonique"),
            Permission(key = "mail", label = "Par contact électronique"),
            Permission(key = "sms", label = "Par SMS / MMS / VMS")
          )
        )
      ),
      offers = Some(Seq(offer1))
    )

    "initialize organisation" in {
      postJson(s"/$tenant/organisations", organisation.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", organisation.asJson()).status mustBe OK

      // offer 1 with version 1
      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED

      // insert consent fact on db
      putJson(s"/$tenant/organisations/$orgKey/users/$userIdDate", consentFact.asJson()).status mustBe OK
    }

    "case no offer in db" in {
      val response =
        putJson(
          s"/$tenant/organisations/$orgKey/users/$userIdDate",
          consentFact.copy(offers = Some(Seq(consentOffer1))).asJson()
        )
      response.status mustBe OK
    }

    "case last update date in offer < last update date in db" in {
      val response =
        putJson(
          s"/$tenant/organisations/$orgKey/users/$userIdDate",
          consentFact
            .copy(offers = Some(Seq(consentOffer1.copy(lastUpdate = yesterday))))
            .asJson()
        )
      response.status mustBe CONFLICT

      (response.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "the.specified.offer.update.date.must.be.greater.than.the.saved.offer.update.date"
    }

    "case last update date in offer = last update date in db" in {
      putJson(
        s"/$tenant/organisations/$orgKey/users/$userIdDate",
        consentFact
          .copy(offers = Some(Seq(consentOffer1)))
          .asJson()
      ).status mustBe OK
    }

    "case last update date in offer > last update date in db" in {
      putJson(
        s"/$tenant/organisations/$orgKey/users/$userIdDate",
        consentFact
          .copy(offers = Some(Seq(consentOffer1.copy(lastUpdate = tommorrow))))
          .asJson()
      ).status mustBe OK
    }
  }

  "put just offer2 with user has 1 & 2" should {
    val today     = DateTime.now(DateTimeZone.UTC)
    val yesterday = DateTime.now(DateTimeZone.UTC).minusDays(1)

    val consentOffer1: ConsentOffer = offerToConsentOffer(offer1).copy(lastUpdate = yesterday)
    val consentOffer2: ConsentOffer = offerToConsentOffer(offer2).copy(lastUpdate = yesterday)

    val userIdDate  = "userIdWithOffer"
    val consentFact = ConsentFact(
      _id = "cf",
      userId = userIdDate,
      doneBy = DoneBy("a1", "admin"),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          consents = Seq(
            Consent(key = "phone", label = "Par contact téléphonique", checked = true),
            Consent(key = "mail", label = "Par contact électronique", checked = false),
            Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
          )
        )
      ),
      offers = Some(Seq(consentOffer1, consentOffer2)),
      lastUpdate = today,
      metaData = Some(Map("mdKey1" -> "mdVal1", "tata" -> "val2"))
    )

    val orgKey: String = "organisationForOffers"

    val organisation: Organisation = Organisation(
      key = orgKey,
      label = "organisation",
      groups = Seq(
        PermissionGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          permissions = Seq(
            Permission(key = "phone", label = "Par contact téléphonique"),
            Permission(key = "mail", label = "Par contact électronique"),
            Permission(key = "sms", label = "Par SMS / MMS / VMS")
          )
        )
      ),
      offers = Some(Seq(offer1))
    )

    "initialize organisation" in {
      postJson(s"/$tenant/organisations", organisation.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", organisation.asJson()).status mustBe OK

      // offer 1 with version 1
      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED

      // insert consent fact on db
      putJson(s"/$tenant/organisations/$orgKey/users/$userIdDate", consentFact.asJson()).status mustBe OK
    }

    "update just offer 2 without change offer 1" in {
      val response =
        putJson(
          s"/$tenant/organisations/$orgKey/users/$userIdDate",
          consentFact
            .copy(
              offers = Some(
                Seq(
                  consentOffer2.copy(
                    lastUpdate = today,
                    groups = consentOffer2.groups.map(g =>
                      ConsentGroup(g.key, g.label, g.consents.map(c => c.copy(checked = true)))
                    )
                  )
                )
              )
            )
            .asJson()
        )
      response.status mustBe OK

      val msgAsJson              = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "ConsentFactUpdated"
      val payloadOffers: JsArray =
        (msgAsJson \ "payload" \ "offers").as[JsArray]

      (payloadOffers \ 0 \ "key")
        .as[String] mustBe "offer2"
      (payloadOffers \ 1 \ "key")
        .as[String] mustBe "offer1"

      (payloadOffers \ 0 \ "lastUpdate")
        .as[String] mustBe today.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
      (payloadOffers \ 1 \ "lastUpdate")
        .as[String] mustBe yesterday.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")

    }
  }

  "put no offers with user has 1 & 2" should {
    val today     = DateTime.now(DateTimeZone.UTC)
    val yesterday = DateTime.now(DateTimeZone.UTC).minusDays(1)

    val consentOffer1: ConsentOffer = offerToConsentOffer(offer1).copy(lastUpdate = yesterday)
    val consentOffer2: ConsentOffer = offerToConsentOffer(offer2).copy(lastUpdate = yesterday)

    val userIdDate  = "userIdWithOffer"
    val consentFact = ConsentFact(
      _id = "cf",
      userId = userIdDate,
      doneBy = DoneBy("a1", "admin"),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          consents = Seq(
            Consent(key = "phone", label = "Par contact téléphonique", checked = true),
            Consent(key = "mail", label = "Par contact électronique", checked = false),
            Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
          )
        )
      ),
      offers = Some(Seq(consentOffer1, consentOffer2)),
      lastUpdate = today,
      metaData = Some(Map("mdKey1" -> "mdVal1", "tata" -> "val2"))
    )

    val orgKey: String = "organisationForNoOffersUpdate"

    val organisation: Organisation = Organisation(
      key = orgKey,
      label = "organisation",
      groups = Seq(
        PermissionGroup(
          key = "maifNotifs",
          label = "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
          permissions = Seq(
            Permission(key = "phone", label = "Par contact téléphonique"),
            Permission(key = "mail", label = "Par contact électronique"),
            Permission(key = "sms", label = "Par SMS / MMS / VMS")
          )
        )
      ),
      offers = Some(Seq(offer1))
    )

    "initialize organisation" in {
      postJson(s"/$tenant/organisations", organisation.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", organisation.asJson()).status mustBe OK

      // offer 1 with version 1
      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED

      // insert consent fact on db
      putJson(s"/$tenant/organisations/$orgKey/users/$userIdDate", consentFact.asJson()).status mustBe OK
    }

    "do not delete subscribed offers" in {
      val response =
        putJson(
          s"/$tenant/organisations/$orgKey/users/$userIdDate",
          consentFact
            .copy(offers = None)
            .asJson()
        )
      response.status mustBe OK

      val msgAsJson              = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "ConsentFactUpdated"
      val payloadOffers: JsArray =
        (msgAsJson \ "payload" \ "offers").as[JsArray]

      (payloadOffers \ 0 \ "key")
        .as[String] mustBe "offer1"
      (payloadOffers \ 1 \ "key")
        .as[String] mustBe "offer2"

    }
  }
}
