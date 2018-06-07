package controllers

import models.{Consent, ConsentFact, ConsentGroup, DoneBy}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsArray, JsValue, Json}
import utils.{DateUtils, TestUtils}
import play.api.test.Helpers._

class ConsentControllerSpec extends TestUtils {

  val userId1: String = "userId1"
  val userId2: String = "userId2"
  val userId3: String = "userId3"
  val userId4: String = "userId4"

  val user1 = ConsentFact(
    userId = userId1,
    doneBy = DoneBy(userId = userId1, role = "USER"),
    version = 2,
    groups = Seq(
      ConsentGroup(
        key = "maifNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        consents = Seq(
          Consent(key = "phone",
                  label = "Par contact téléphonique",
                  checked = true),
          Consent(key = "mail",
                  label = "Par contact électronique",
                  checked = false),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
        )
      ),
      ConsentGroup(
        key = "partenaireNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        consents = Seq(
          Consent(key = "phone",
                  label = "Par contact téléphonique",
                  checked = false),
          Consent(key = "mail",
                  label = "Par contact électronique",
                  checked = true),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
        )
      )
    )
  )
  val user1AsJson = user1.asJson

  val user1Modified = user1.copy(doneBy = user1.doneBy.copy(role = "ADMIN"))
  val user1ModifiedAsJson = user1Modified.asJson

  val user3 = ConsentFact(
    userId = userId3,
    doneBy = DoneBy(userId = userId3, role = "USER"),
    version = 2,
    groups = Seq(
      ConsentGroup(
        key = "maifNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        consents = Seq(
          Consent(key = "phone",
                  label = "Par contact téléphonique",
                  checked = true),
          Consent(key = "mail",
                  label = "Par contact électronique",
                  checked = false),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = true)
        )
      ),
      ConsentGroup(
        key = "partenaireNotifs",
        label =
          "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        consents = Seq(
          Consent(key = "phone",
                  label = "Par contact téléphonique",
                  checked = false),
          Consent(key = "mail",
                  label = "Par contact électronique",
                  checked = true),
          Consent(key = "sms", label = "Par SMS / MMS / VMS", checked = false)
        )
      )
    )
  )
  val user3AsJson = user3.asJson

  val user2InvalidFormatAsJson = Json.obj(
    "userId" -> userId2,
    "doneBy" -> Json.obj(
      "userId" -> userId2,
      "role" -> "USER"
    ),
    "version" -> 2,
    "groups" -> Json.arr(
      Json.obj(
        "key" -> "maifNotifs",
        "label" -> "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
        "consents" -> Json.arr(
          Json.obj(
            "key" -> "sms",
            "label" -> "Par SMS / MMS / VMS",
            "checked" -> false
          )
        )
      ),
      Json.obj(
        "key" -> "partenaireNotifs",
        "label" -> "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
        "consents" -> Json.arr(
          Json.obj(
            "key" -> "mail",
            "label" -> "Par contact électronique",
            "checked" -> false
          ),
          Json.obj(
            "key" -> "sms",
            "label" -> "Par SMS / MMS / VMS",
            "checked" -> false
          )
        )
      )
    )
  )

  "ConsentController" should {
    val organisationKey: String = "maif"

    "user not exist" in {
      val path: String =
        s"/$tenant/organisations/$organisationKey/users/$userId1"
      val response = getJson(path)

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
      val putResponse = putJson(path, user1AsJson)

      putResponse.status mustBe OK

      val putValue: JsValue = putResponse.json

      (putValue \ "userId").as[String] mustBe user1.userId
      (putValue \ "doneBy" \ "userId").as[String] mustBe user1.doneBy.userId
      (putValue \ "doneBy" \ "role").as[String] mustBe user1.doneBy.role
      (putValue \ "version").as[Int] mustBe user1.version

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "ConsentFactCreated"
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId1

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
      val putResponse = putJson(path, user1ModifiedAsJson)

      putResponse.status mustBe OK

      val putValue: JsValue = putResponse.json

      (putValue \ "userId").as[String] mustBe userId1
      (putValue \ "orgKey").as[String] mustBe organisationKey
      (putValue \ "doneBy" \ "userId").as[String] mustBe user1Modified.doneBy
        .userId
      (putValue \ "doneBy" \ "role").as[String] mustBe user1Modified.doneBy.role
      (putValue \ "version").as[Int] mustBe user1Modified.version

      val msgAsJson = readLastKafkaEvent()

      (msgAsJson \ "type").as[String] mustBe "ConsentFactUpdated"
      (msgAsJson \ "oldValue" \ "doneBy" \ "role")
        .as[String] mustBe user1.doneBy.role
      (msgAsJson \ "payload" \ "doneBy" \ "role")
        .as[String] mustBe user1Modified.doneBy.role

      val response = getJson(path)

      val value: JsValue = response.json

      (value \ "userId").as[String] mustBe userId1
      (value \ "orgKey").as[String] mustBe organisationKey
      (value \ "doneBy" \ "userId").as[String] mustBe user1Modified.doneBy
        .userId
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

    "update user with invalid consents compare to organisation version" in {
      putJson(s"/$tenant/organisations/$organisationKey/users/$userId2",
              user2InvalidFormatAsJson).status mustBe BAD_REQUEST
    }

    "get consents history" in {
      val path: String =
        s"/$tenant/organisations/$organisationKey/users/$userId3"
      putJson(path, user3AsJson).status mustBe OK
      val msg1 = readLastKafkaEvent()
      (msg1 \ "type").as[String] mustBe "ConsentFactCreated"
      putJson(path, user3AsJson).status mustBe OK
      val msg2 = readLastKafkaEvent()
      (msg2 \ "type").as[String] mustBe "ConsentFactUpdated"
      putJson(path, user3AsJson).status mustBe OK
      val msg3 = readLastKafkaEvent()
      (msg3 \ "type").as[String] mustBe "ConsentFactUpdated"
      putJson(path, user3AsJson).status mustBe OK
      val msg4 = readLastKafkaEvent()
      (msg4 \ "type").as[String] mustBe "ConsentFactUpdated"
      putJson(path, user3AsJson).status mustBe OK
      val msg5 = readLastKafkaEvent()
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
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          )
        ),
        lastUpdate = DateTime.now(DateTimeZone.UTC)
      )

      val consentFactAsXml = consentFact.asXml

      val resp =
        putXml(s"/$tenant/organisations/$organisationKey/users/$userId4",
               consentFactAsXml)

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
      val yesterday: DateTime = DateTime.now(DateTimeZone.UTC).minusDays(1)

      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId4,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          )
        ),
        lastUpdate = yesterday
      )

      val resp =
        putJson(s"/$tenant/organisations/$organisationKey/users/$userId4",
                consentFact.asJson)

      resp.status mustBe OK

      val json: JsValue = resp.json

      (json \ "lastUpdate").as[String] mustBe yesterday.toString(
        DateUtils.utcDateFormatter)

      val respGet =
        getJson(s"/$tenant/organisations/$organisationKey/users/$userId4")

      respGet.status mustBe OK

      val jsonGet: JsValue = respGet.json

      (jsonGet \ "lastUpdate").as[String] mustBe yesterday.toString(
        DateUtils.utcDateFormatter)
    }

    "not force update date" in {
      val userId5 = "userId5"
      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId5,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          )
        )
      )

      val resp =
        putJson(s"/$tenant/organisations/$organisationKey/users/$userId5",
                consentFact.asJson)

      resp.status mustBe OK

      val json: JsValue = resp.json

      (json \ "lastUpdate").as[String] must not be null
    }

    "force lastUpdate date xml" in {
      val yesterday: DateTime = DateTime.now(DateTimeZone.UTC).minusDays(1)

      val userId6 = "userId6"

      val consentFact = ConsentFact(
        _id = "cf",
        userId = userId6,
        doneBy = DoneBy("a1", "admin"),
        version = 2,
        groups = Seq(
          ConsentGroup(
            "maifNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          ),
          ConsentGroup(
            "partenaireNotifs",
            "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF",
            Seq(Consent("phone", "Par contact téléphonique", false),
                Consent("mail", "Par contact électronique", false),
                Consent("sms", "Par SMS / MMS / VMS", false))
          )
        ),
        lastUpdate = yesterday
      )

      val resp =
        putXml(s"/$tenant/organisations/$organisationKey/users/$userId6",
               consentFact.asXml)

      resp.status mustBe OK

      val xml = resp.xml

      (xml \ "lastUpdate").head.text mustBe yesterday.toString(
        DateUtils.utcDateFormatter)

      val respGet =
        getXml(s"/$tenant/organisations/$organisationKey/users/$userId6")

      respGet.status mustBe OK

      val xmlGet = respGet.xml

      (xmlGet \ "lastUpdate").head.text mustBe yesterday.toString(
        DateUtils.utcDateFormatter)
    }

  }
}
