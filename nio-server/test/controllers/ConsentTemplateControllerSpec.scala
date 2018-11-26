package controllers

import models._
import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import utils.TestUtils
import play.api.test.Helpers._

class ConsentTemplateControllerSpec extends TestUtils {
  val org1Key = "orgTest1"

  val org1 = Organisation(
    key = org1Key,
    label = s"$org1Key",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "First group label",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val org1ToUpdate = Organisation(
    key = org1Key,
    label = s"$org1Key",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "First group label",
                      permissions =
                        Seq(Permission("sms", "Please accept sms"),
                            Permission("email", "Please accept email"))),
      PermissionGroup(key = "group2",
                      label = "Second group label",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val org1ToUpdate2 = Organisation(
    key = org1Key,
    label = s"$org1Key",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "First group label",
                      permissions =
                        Seq(Permission("email", "Please accept email"))),
      PermissionGroup(key = "group2",
                      label = "Second group label",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val userId1: String = "userId1"

  val consentFactUserId1 = ConsentFact(
    userId = userId1,
    version = 1,
    doneBy = DoneBy(userId = userId1, role = "user"),
    groups = Seq(
      ConsentGroup(
        key = "group1",
        label = "First group label",
        consents = Seq(
          Consent(
            key = "sms",
            label = "Please accept sms",
            checked = true
          )
        )
      )
    )
  )
  val consentFactUserId1Update = ConsentFact(
    userId = userId1,
    version = 2,
    doneBy = DoneBy(userId = userId1, role = "user"),
    groups = Seq(
      ConsentGroup(
        key = "group1",
        label = "First group label",
        consents = Seq(
          Consent(
            key = "sms",
            label = "Please accept sms",
            checked = false
          ),
          Consent(
            key = "email",
            label = "Please accept email",
            checked = true
          )
        )
      ),
      ConsentGroup(
        key = "group2",
        label = "Second group label",
        consents = Seq(
          Consent(
            key = "sms",
            label = "Please accept sms",
            checked = true
          )
        )
      )
    )
  )

  "ConsentController" should {
    "Update organisation released after first user consent approuved" in {
      // Create an organisation
      postJson(s"/$tenant/organisations", org1.asJson).status must be(CREATED)

      // released an organisation
      postJson(s"/$tenant/organisations/$org1Key/draft/_release", org1.asJson).status must be(
        OK)

      // get consent fact template
      val initialTemplateV1: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/users/_template")
      val templateV1Value: JsValue = initialTemplateV1.json

      (templateV1Value \ "userId").as[String] must be("fill")
      (templateV1Value \ "version").as[Int] must be(1)
      (templateV1Value \ "groups").as[JsArray].value.size must be(1)
      (templateV1Value \ "groups" \ 0 \ "key").as[String] must be("group1")
      (templateV1Value \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (templateV1Value \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV1Value \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV1Value \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV1Value \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)

      // Create consent fact user for this released
      putJson(s"/$tenant/organisations/$org1Key/users/$userId1",
              consentFactUserId1.asJson).status must be(OK)

      // Control consent fact user after first update
      val updateConsentUser1: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/users/$userId1")
      updateConsentUser1.status must be(OK)

      val updateConsentUser1Value: JsValue = updateConsentUser1.json

      (updateConsentUser1Value \ "userId").as[String] must be(userId1)
      (updateConsentUser1Value \ "version").as[Int] must be(1)
      (updateConsentUser1Value \ "groups").as[JsArray].value.size must be(1)
      (updateConsentUser1Value \ "groups" \ 0 \ "key").as[String] must be(
        "group1")
      (updateConsentUser1Value \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (updateConsentUser1Value \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (updateConsentUser1Value \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (updateConsentUser1Value \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (updateConsentUser1Value \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(true)

      putJson(s"/$tenant/organisations/$org1Key/draft", org1ToUpdate.asJson).status mustBe OK

      // released a new organisation
      postJson(s"/$tenant/organisations/$org1Key/draft/_release",
               org1ToUpdate.asJson).status must be(OK)

      // Get consent fact template  without userId
      val initialTemplateV2: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/users/_template")
      val templateV2Value: JsValue = initialTemplateV2.json

      (templateV2Value \ "userId").as[String] must be("fill")
      (templateV2Value \ "version").as[Int] must be(2)
      (templateV2Value \ "groups").as[JsArray].value.size must be(2)
      (templateV2Value \ "groups" \ 0 \ "key").as[String] must be("group1")
      (templateV2Value \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (templateV2Value \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(2)
      (templateV2Value \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV2Value \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV2Value \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)
      (templateV2Value \ "groups" \ 0 \ "consents" \ 1 \ "key")
        .as[String] must be("email")
      (templateV2Value \ "groups" \ 0 \ "consents" \ 1 \ "label")
        .as[String] must be("Please accept email")
      (templateV2Value \ "groups" \ 0 \ "consents" \ 1 \ "checked")
        .as[Boolean] must be(false)
      (templateV2Value \ "groups" \ 1 \ "key").as[String] must be("group2")
      (templateV2Value \ "groups" \ 1 \ "label").as[String] must be(
        "Second group label")
      (templateV2Value \ "groups" \ 1 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV2Value \ "groups" \ 1 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV2Value \ "groups" \ 1 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV2Value \ "groups" \ 1 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)

      // get consent fact template with userId
      val initialTemplateV2WithUserId: WSResponse = getJson(
        s"/$tenant/organisations/$org1Key/users/_template?userId=$userId1")
      val templateV2WithUserIdValue: JsValue = initialTemplateV2WithUserId.json

      (templateV2WithUserIdValue \ "userId").as[String] must be(userId1)
      (templateV2WithUserIdValue \ "version").as[Int] must be(2)
      (templateV2WithUserIdValue \ "groups").as[JsArray].value.size must be(2)
      (templateV2WithUserIdValue \ "groups" \ 0 \ "key").as[String] must be(
        "group1")
      (templateV2WithUserIdValue \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(2)
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(true)
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents" \ 1 \ "key")
        .as[String] must be("email")
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents" \ 1 \ "label")
        .as[String] must be("Please accept email")
      (templateV2WithUserIdValue \ "groups" \ 0 \ "consents" \ 1 \ "checked")
        .as[Boolean] must be(false)
      (templateV2WithUserIdValue \ "groups" \ 1 \ "key").as[String] must be(
        "group2")
      (templateV2WithUserIdValue \ "groups" \ 1 \ "label").as[String] must be(
        "Second group label")
      (templateV2WithUserIdValue \ "groups" \ 1 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV2WithUserIdValue \ "groups" \ 1 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV2WithUserIdValue \ "groups" \ 1 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV2WithUserIdValue \ "groups" \ 1 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)

      // Put consent fact for user with the new version
      putJson(s"/$tenant/organisations/$org1Key/users/$userId1",
              consentFactUserId1Update.asJson).status must be(OK)

      // Control consent fact user after second update
      val update2ConsentUser1: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/users/$userId1")
      update2ConsentUser1.status must be(OK)

      val update2ConsentUser1Value: JsValue = update2ConsentUser1.json

      (update2ConsentUser1Value \ "userId").as[String] must be(userId1)
      (update2ConsentUser1Value \ "version").as[Int] must be(2)
      (update2ConsentUser1Value \ "groups").as[JsArray].value.size must be(2)
      (update2ConsentUser1Value \ "groups" \ 0 \ "key").as[String] must be(
        "group1")
      (update2ConsentUser1Value \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(2)
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents" \ 1 \ "key")
        .as[String] must be("email")
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents" \ 1 \ "label")
        .as[String] must be("Please accept email")
      (update2ConsentUser1Value \ "groups" \ 0 \ "consents" \ 1 \ "checked")
        .as[Boolean] must be(true)
      (update2ConsentUser1Value \ "groups" \ 1 \ "key").as[String] must be(
        "group2")
      (update2ConsentUser1Value \ "groups" \ 1 \ "label").as[String] must be(
        "Second group label")
      (update2ConsentUser1Value \ "groups" \ 1 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (update2ConsentUser1Value \ "groups" \ 1 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (update2ConsentUser1Value \ "groups" \ 1 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (update2ConsentUser1Value \ "groups" \ 1 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(true)

      // Release a new organisation version
      putJson(s"/$tenant/organisations/$org1Key/draft", org1ToUpdate2.asJson).status must be(
        OK)

      postJson(s"/$tenant/organisations/$org1Key/draft/_release",
               org1ToUpdate2.asJson).status must be(OK)

      // Get consent fact template  without userId
      val initialTemplateV3: WSResponse =
        getJson(s"/$tenant/organisations/$org1Key/users/_template")
      val templateV3Value: JsValue = initialTemplateV3.json

      (templateV3Value \ "userId").as[String] must be("fill")
      (templateV3Value \ "version").as[Int] must be(3)
      (templateV3Value \ "groups").as[JsArray].value.size must be(2)
      (templateV3Value \ "groups" \ 0 \ "key").as[String] must be("group1")
      (templateV3Value \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (templateV3Value \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV3Value \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("email")
      (templateV3Value \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept email")
      (templateV3Value \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)
      (templateV3Value \ "groups" \ 1 \ "key").as[String] must be("group2")
      (templateV3Value \ "groups" \ 1 \ "label").as[String] must be(
        "Second group label")
      (templateV3Value \ "groups" \ 1 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV3Value \ "groups" \ 1 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV3Value \ "groups" \ 1 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV3Value \ "groups" \ 1 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(false)

      // get consent fact template with userId
      val initialTemplateV3WithUserId: WSResponse = getJson(
        s"/$tenant/organisations/$org1Key/users/_template?userId=$userId1")
      val templateV3WithUserIdValue: JsValue = initialTemplateV3WithUserId.json

      (templateV3WithUserIdValue \ "userId").as[String] must be(userId1)
      (templateV3WithUserIdValue \ "version").as[Int] must be(3)
      (templateV3WithUserIdValue \ "groups").as[JsArray].value.size must be(2)
      (templateV3WithUserIdValue \ "groups" \ 0 \ "key").as[String] must be(
        "group1")
      (templateV3WithUserIdValue \ "groups" \ 0 \ "label").as[String] must be(
        "First group label")
      (templateV3WithUserIdValue \ "groups" \ 0 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV3WithUserIdValue \ "groups" \ 0 \ "consents" \ 0 \ "key")
        .as[String] must be("email")
      (templateV3WithUserIdValue \ "groups" \ 0 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept email")
      (templateV3WithUserIdValue \ "groups" \ 0 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(true)
      (templateV3WithUserIdValue \ "groups" \ 1 \ "key").as[String] must be(
        "group2")
      (templateV3WithUserIdValue \ "groups" \ 1 \ "label").as[String] must be(
        "Second group label")
      (templateV3WithUserIdValue \ "groups" \ 1 \ "consents")
        .as[JsArray]
        .value
        .size must be(1)
      (templateV3WithUserIdValue \ "groups" \ 1 \ "consents" \ 0 \ "key")
        .as[String] must be("sms")
      (templateV3WithUserIdValue \ "groups" \ 1 \ "consents" \ 0 \ "label")
        .as[String] must be("Please accept sms")
      (templateV3WithUserIdValue \ "groups" \ 1 \ "consents" \ 0 \ "checked")
        .as[Boolean] must be(true)
    }
  }

  "ConsentController with offer" should {
    val orgKey: String = "orgWithOffer2"

    "create organisation, offer1, offer2" in {
      val orgWithOffer: Organisation = Organisation(
        key = orgKey,
        label = "organisation with offer",
        groups = Seq(
          PermissionGroup(
            key = "maifNotifs",
            label =
              "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            permissions = Seq(
              Permission(key = "phone", label = "Par contact téléphonique"),
              Permission(key = "mail", label = "Par contact électronique"),
              Permission(key = "sms", label = "Par SMS / MMS / VMS")
            )
          )
        )
      )

      postJson(s"/$tenant/organisations", orgWithOffer.asJson).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", Json.obj()).status mustBe OK

      val offer1: Offer = Offer(
        "offer1",
        "offer 1",
        1,
        Seq(
          PermissionGroup(
            key = "maifNotifs",
            label =
              "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
            permissions = Seq(
              Permission(key = "phone", label = "Par contact téléphonique"),
              Permission(key = "mail", label = "Par contact électronique"),
              Permission(key = "sms", label = "Par SMS / MMS / VMS")
            )
          ))
      )

      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED

      val offer2: Offer =
        Offer(
          "offer2",
          "offer 2",
          1,
          Seq(
            PermissionGroup(
              key = "maifNotifs",
              label =
                "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
              permissions = Seq(
                Permission(key = "phone", label = "Par contact téléphonique"),
                Permission(key = "mail", label = "Par contact électronique"),
                Permission(key = "sms", label = "Par SMS / MMS / VMS")
              )
            ))
        )

      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED
    }

    "generate empty template with offer" in {
      val response: WSResponse =
        getJson(s"/$tenant/organisations/$orgKey/users/_template")

      response.status mustBe OK

      val value: JsValue = response.json

      val offers: JsArray = (value \ "offers").as[JsArray]
      offers.value.length mustBe 2

      (offers \ 0 \ "key").as[String] mustBe "offer1"
      (offers \ 0 \ "label").as[String] mustBe "offer 1"
      (offers \ 0 \ "groups").as[JsArray].value.length mustBe 1
      val offer1group1 = (offers \ 0 \ "groups" \ 0).as[JsValue]

      (offer1group1 \ "key").as[String] mustBe "maifNotifs"
      (offer1group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

      (offer1group1 \ "consents").as[JsArray].value.length mustBe 3

      val offer1group1perm1 = (offer1group1 \ "consents" \ 0).as[JsValue]

      (offer1group1perm1 \ "key").as[String] mustBe "phone"
      (offer1group1perm1 \ "label").as[String] mustBe "Par contact téléphonique"
      (offer1group1perm1 \ "checked").as[Boolean] mustBe false

      val offer1group1perm2 = (offer1group1 \ "consents" \ 1).as[JsValue]

      (offer1group1perm2 \ "key").as[String] mustBe "mail"
      (offer1group1perm2 \ "label").as[String] mustBe "Par contact électronique"
      (offer1group1perm2 \ "checked").as[Boolean] mustBe false

      val offer1group1perm3 = (offer1group1 \ "consents" \ 2).as[JsValue]

      (offer1group1perm3 \ "key").as[String] mustBe "sms"
      (offer1group1perm3 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (offer1group1perm3 \ "checked").as[Boolean] mustBe false

      (offers \ 1 \ "key").as[String] mustBe "offer2"
      (offers \ 1 \ "label").as[String] mustBe "offer 2"
      (offers \ 1 \ "groups").as[JsArray].value.length mustBe 1
      val offer2group1 = (offers \ 1 \ "groups" \ 0).as[JsValue]

      (offer2group1 \ "key").as[String] mustBe "maifNotifs"
      (offer2group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

      (offer2group1 \ "consents").as[JsArray].value.length mustBe 3

      val offer2group1perm1 = (offer2group1 \ "consents" \ 0).as[JsValue]

      (offer2group1perm1 \ "key").as[String] mustBe "phone"
      (offer2group1perm1 \ "label").as[String] mustBe "Par contact téléphonique"
      (offer2group1perm1 \ "checked").as[Boolean] mustBe false

      val offer2group1perm2 = (offer2group1 \ "consents" \ 1).as[JsValue]

      (offer2group1perm2 \ "key").as[String] mustBe "mail"
      (offer2group1perm2 \ "label").as[String] mustBe "Par contact électronique"
      (offer2group1perm2 \ "checked").as[Boolean] mustBe false

      val offer2group1perm3 = (offer2group1 \ "consents" \ 2).as[JsValue]

      (offer2group1perm3 \ "key").as[String] mustBe "sms"
      (offer2group1perm3 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (offer2group1perm3 \ "checked").as[Boolean] mustBe false
    }

    "generate template with just the offer1" in {
      val response: WSResponse =
        getJson(
          s"/$tenant/organisations/$orgKey/users/_template?offerKeys=offer1")

      response.status mustBe OK

      val value: JsValue = response.json

      val offers: JsArray = (value \ "offers").as[JsArray]
      offers.value.length mustBe 1

      (offers \ 0 \ "key").as[String] mustBe "offer1"
      (offers \ 0 \ "label").as[String] mustBe "offer 1"
      (offers \ 0 \ "groups").as[JsArray].value.length mustBe 1
      val offer1group1 = (offers \ 0 \ "groups" \ 0).as[JsValue]

      (offer1group1 \ "key").as[String] mustBe "maifNotifs"
      (offer1group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

      (offer1group1 \ "consents").as[JsArray].value.length mustBe 3

      val offer1group1perm1 = (offer1group1 \ "consents" \ 0).as[JsValue]

      (offer1group1perm1 \ "key").as[String] mustBe "phone"
      (offer1group1perm1 \ "label").as[String] mustBe "Par contact téléphonique"
      (offer1group1perm1 \ "checked").as[Boolean] mustBe false

      val offer1group1perm2 = (offer1group1 \ "consents" \ 1).as[JsValue]

      (offer1group1perm2 \ "key").as[String] mustBe "mail"
      (offer1group1perm2 \ "label").as[String] mustBe "Par contact électronique"
      (offer1group1perm2 \ "checked").as[Boolean] mustBe false

      val offer1group1perm3 = (offer1group1 \ "consents" \ 2).as[JsValue]

      (offer1group1perm3 \ "key").as[String] mustBe "sms"
      (offer1group1perm3 \ "label").as[String] mustBe "Par SMS / MMS / VMS"
      (offer1group1perm3 \ "checked").as[Boolean] mustBe false
    }

    "generate template with just the unknowOfferKey" in {
      val response: WSResponse =
        getJson(
          s"/$tenant/organisations/$orgKey/users/_template?offerKeys=unknowOfferKey")

      response.status mustBe OK

      val value: JsValue = response.json

      (value \ "offers").asOpt[JsArray] mustBe None
    }

    "generate template with user in reference" in {

      val userId = "userIdOffer"
      val consent: ConsentFact = ConsentFact(
        userId = userId,
        doneBy = DoneBy(userId = userId, role = "USER"),
        version = 1,
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
              Consent(key = "sms",
                      label = "Par SMS / MMS / VMS",
                      checked = true)
            )
          )
        ),
        offers = Some(
          Seq(
            ConsentOffer(
              key = "offer1",
              label = "offer 1",
              version = 1,
              Seq(
                ConsentGroup(
                  key = "maifNotifs",
                  label =
                    "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF",
                  consents = Seq(
                    Consent(key = "phone",
                            label = "Par contact téléphonique",
                            checked = false),
                    Consent(key = "mail",
                            label = "Par contact électronique",
                            checked = true),
                    Consent(key = "sms",
                            label = "Par SMS / MMS / VMS",
                            checked = false)
                  )
                )
              )
            ),
            ConsentOffer(
              key = "offer2",
              label = "offer 2",
              version = 1,
              Seq(
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
                            checked = true),
                    Consent(key = "sms",
                            label = "Par SMS / MMS / VMS",
                            checked = false)
                  )
                )
              )
            )
          )
        )
      )

      putJson(s"/$tenant/organisations/$orgKey/users/$userId", consent.asJson).status mustBe OK

      val response: WSResponse =
        getJson(
          s"/$tenant/organisations/$orgKey/users/_template?userId=$userId")

      response.status mustBe OK

      val value: JsValue = response.json

      Logger.info(response.body)
      val offers: JsArray = (value \ "offers").as[JsArray]
      offers.value.length mustBe 2

      (offers \ 0 \ "key").as[String] mustBe "offer1"
      (offers \ 0 \ "label").as[String] mustBe "offer 1"
      (offers \ 0 \ "groups").as[JsArray].value.length mustBe 1
      val offer1group1 = (offers \ 0 \ "groups" \ 0).as[JsValue]

      (offer1group1 \ "key").as[String] mustBe "maifNotifs"
      (offer1group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

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
      (offers \ 1 \ "label").as[String] mustBe "offer 2"
      (offers \ 1 \ "groups").as[JsArray].value.length mustBe 1
      val offer2group1 = (offers \ 1 \ "groups" \ 0).as[JsValue]

      (offer2group1 \ "key").as[String] mustBe "maifNotifs"
      (offer2group1 \ "label")
        .as[String] mustBe "J'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF"

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
  }

}
