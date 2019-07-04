package controllers

import java.io.{BufferedWriter, File, FileWriter}

import models._
import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TestUtils

class OrganisationOfferControllerSpec extends TestUtils {

  private val orgKey: String = "orgOffer1"
  private val org: Organisation = Organisation(
    key = orgKey,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1",
                      label = "blalba",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  private val offerKey1 = "offer1"
  private val offer: Offer = Offer(
    key = offerKey1,
    label = "offer one",
    groups = Seq(
      PermissionGroup(key = "offerGrp1",
                      label = "offer group one",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )
  private val offerUpdated: Offer = Offer(
    key = offerKey1,
    label = "offer 1",
    groups = Seq(
      PermissionGroup(
        key = "offerGrp1",
        label = "offer group one",
        permissions = Seq(Permission("sms", "Please accept sms"))),
      PermissionGroup(key = "offerGrp2",
                      label = "offer group two",
                      permissions =
                        Seq(Permission("sms2", "Please accept sms 2")))
    )
  )

  private val offerKey2 = "offer2"
  private val offer2: Offer = Offer(
    key = offerKey2,
    label = "offer two",
    groups = Seq(
      PermissionGroup(key = "offerGrp2",
                      label = "offer group two",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )
  private val offerKeyUnauthorized2 = "offerKeyUnauthorized2"
  private val offerUnauthorized2: Offer = Offer(
    key = offerKeyUnauthorized2,
    label = "offer two",
    groups = Seq(
      PermissionGroup(key = "offerGrp2",
                      label = "offer group two",
                      permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  "OrganisationOfferController" should {
    "create organisation" in {
      postJson(s"/$tenant/organisations", org.asJson).status mustBe CREATED
    }

    "create a first offer on not release organisation" in {
      val createResponse: WSResponse =
        postJson(s"/$tenant/organisations/$orgKey/offers", offer.asJson())

      Logger.info(s"${createResponse.json}")

      createResponse.status mustBe NOT_FOUND
    }

    "release organisation" in {
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", org.asJson).status mustBe OK

      val get1Response = getJson(s"/$tenant/organisations/$orgKey/offers")
      get1Response.status mustBe OK

      val get1Offers: JsArray = get1Response.json.as[JsArray]
      get1Offers.value.length mustBe 0
    }

    "create a first offer" in {
      val createOfferResponse =
        postJson(s"/$tenant/organisations/$orgKey/offers", offer.asJson())
      createOfferResponse.status mustBe CREATED

      val get2Response = getJson(s"/$tenant/organisations/$orgKey/offers")
      get2Response.status mustBe OK
      val get2Offers: JsArray = get2Response.json.as[JsArray]
      get2Offers.value.length mustBe 1

      val value: JsValue = get2Offers.value.head

      (value \ "key").as[String] mustBe offer.key
      (value \ "label").as[String] mustBe offer.label
      (value \ "version").as[Int] mustBe 1

      val groups: JsArray = (value \ "groups").as[JsArray]
      groups.value.length mustBe 1

      (groups \ 0 \ "key").as[String] mustBe offer.groups.head.key
      (groups \ 0 \ "label").as[String] mustBe offer.groups.head.label
      (groups \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer.groups.head.permissions.head.key
      (groups \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer.groups.head.permissions.head.label
    }

    "create a second offer" in {
      val createOfferResponse =
        postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson())
      createOfferResponse.status mustBe CREATED

      val get2Response = getJson(s"/$tenant/organisations/$orgKey/offers")
      get2Response.status mustBe OK
      val get2Offers: JsArray = get2Response.json.as[JsArray]
      get2Offers.value.length mustBe 2

      val value: JsValue = get2Offers.value.head

      (value \ "key").as[String] mustBe offer.key
      (value \ "label").as[String] mustBe offer.label
      (value \ "version").as[Int] mustBe 1

      val groups: JsArray = (value \ "groups").as[JsArray]
      groups.value.length mustBe 1

      (groups \ 0 \ "key").as[String] mustBe offer.groups.head.key
      (groups \ 0 \ "label").as[String] mustBe offer.groups.head.label
      (groups \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer.groups.head.permissions.head.key
      (groups \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer.groups.head.permissions.head.label

      val value2: JsValue = (get2Offers \ 1).as[JsValue]

      (value2 \ "key").as[String] mustBe offer2.key
      (value2 \ "label").as[String] mustBe offer2.label
      (value2 \ "version").as[Int] mustBe 1

      val groups2: JsArray = (value2 \ "groups").as[JsArray]
      groups2.value.length mustBe 1

      (groups2 \ 0 \ "key").as[String] mustBe offer2.groups.head.key
      (groups2 \ 0 \ "label").as[String] mustBe offer2.groups.head.label
      (groups2 \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer2.groups.head.permissions.head.key
      (groups2 \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer2.groups.head.permissions.head.label
    }

    "try to create an offer with an unauthorized key" in {
      val createOfferResponse =
        postJson(s"/$tenant/organisations/$orgKey/offers",
                 offerUnauthorized2.asJson())
      createOfferResponse.status mustBe UNAUTHORIZED

      (createOfferResponse.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "offer.offerKeyUnauthorized2.not.accessible"
    }

    "create offer with an already exist offer key" in {
      val createOfferResponse =
        postJson(s"/$tenant/organisations/$orgKey/offers", offer.asJson())
      createOfferResponse.status mustBe CONFLICT

      (createOfferResponse.json \ "errors" \ 0 \ "message")
        .as[String] mustBe "offer.with.key.offer1.on.organisation.orgOffer1.already.exist"
    }

    "update an offer" in {
      val updateResponse =
        putJson(s"/$tenant/organisations/$orgKey/offers/$offerKey1",
                offerUpdated.asJson())
      updateResponse.status mustBe OK

      val get2Response = getJson(s"/$tenant/organisations/$orgKey/offers")
      get2Response.status mustBe OK
      val get2Offers: JsArray = get2Response.json.as[JsArray]
      get2Offers.value.length mustBe 2

      val value: JsValue = get2Offers.value.head

      (value \ "key").as[String] mustBe offerUpdated.key
      (value \ "label").as[String] mustBe offerUpdated.label
      (value \ "version").as[Int] mustBe 2

      val groups: JsArray = (value \ "groups").as[JsArray]
      groups.value.length mustBe 2

      (groups \ 0 \ "key").as[String] mustBe offerUpdated.groups.head.key
      (groups \ 0 \ "label").as[String] mustBe offerUpdated.groups.head.label
      (groups \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offerUpdated.groups.head.permissions.head.key
      (groups \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offerUpdated.groups.head.permissions.head.label

      (groups \ 1 \ "key").as[String] mustBe offerUpdated.groups(1).key
      (groups \ 1 \ "label").as[String] mustBe offerUpdated.groups(1).label
      (groups \ 1 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offerUpdated.groups(1).permissions.head.key
      (groups \ 1 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offerUpdated.groups(1).permissions.head.label

      val value2: JsValue = (get2Offers \ 1).as[JsValue]

      (value2 \ "key").as[String] mustBe offer2.key
      (value2 \ "label").as[String] mustBe offer2.label
      (value2 \ "version").as[Int] mustBe 1

      val groups2: JsArray = (value2 \ "groups").as[JsArray]
      groups2.value.length mustBe 1

      (groups2 \ 0 \ "key").as[String] mustBe offer2.groups.head.key
      (groups2 \ 0 \ "label").as[String] mustBe offer2.groups.head.label
      (groups2 \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer2.groups.head.permissions.head.key
      (groups2 \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer2.groups.head.permissions.head.label
    }

    "update offer with another offer version" in {
      val updateResponse =
        putJson(s"/$tenant/organisations/$orgKey/offers/$offerKey1",
                offerUpdated.copy(version = 10).asJson())

      updateResponse.status mustBe OK
      (updateResponse.json \ "version").as[Int] mustBe 3
    }

    "delete an offer" in {
      val deleteResponse =
        delete(s"/$tenant/organisations/$orgKey/offers/$offerKey1")
      deleteResponse.status mustBe OK

      val get2Response = getJson(s"/$tenant/organisations/$orgKey/offers")
      get2Response.status mustBe OK
      val get2Offers: JsArray = get2Response.json.as[JsArray]
      get2Offers.value.length mustBe 1

      val value2: JsValue = (get2Offers \ 0).as[JsValue]

      (value2 \ "key").as[String] mustBe offer2.key
      (value2 \ "label").as[String] mustBe offer2.label
      (value2 \ "version").as[Int] mustBe 1

      val groups2: JsArray = (value2 \ "groups").as[JsArray]
      groups2.value.length mustBe 1

      (groups2 \ 0 \ "key").as[String] mustBe offer2.groups.head.key
      (groups2 \ 0 \ "label").as[String] mustBe offer2.groups.head.label
      (groups2 \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer2.groups.head.permissions.head.key
      (groups2 \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer2.groups.head.permissions.head.label
    }

    "get offers after organisation release" in {

      val releaseOrganisation =
        postJson(s"/$tenant/organisations/$orgKey/draft/_release", Json.obj())

      releaseOrganisation.status mustBe OK
      val releasedOrg = releaseOrganisation.json

      val offers = (releasedOrg \ "offers").as[JsArray]

      offers.value.length mustBe 1

      val offer1: JsValue = (offers \ 0).as[JsValue]

      (offer1 \ "key").as[String] mustBe offer2.key
      (offer1 \ "label").as[String] mustBe offer2.label
      (offer1 \ "version").as[Int] mustBe 1

      val groupsOffer1: JsArray = (offer1 \ "groups").as[JsArray]
      groupsOffer1.value.length mustBe 1

      (groupsOffer1 \ 0 \ "key").as[String] mustBe offer2.groups.head.key
      (groupsOffer1 \ 0 \ "label").as[String] mustBe offer2.groups.head.label
      (groupsOffer1 \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer2.groups.head.permissions.head.key
      (groupsOffer1 \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer2.groups.head.permissions.head.label

      val get2Response = getJson(s"/$tenant/organisations/$orgKey/offers")
      get2Response.status mustBe OK
      val get2Offers: JsArray = get2Response.json.as[JsArray]
      get2Offers.value.length mustBe 1

      val value2: JsValue = (get2Offers \ 0).as[JsValue]

      (value2 \ "key").as[String] mustBe offer2.key
      (value2 \ "label").as[String] mustBe offer2.label
      (value2 \ "version").as[Int] mustBe 1

      val groups2: JsArray = (value2 \ "groups").as[JsArray]
      groups2.value.length mustBe 1

      (groups2 \ 0 \ "key").as[String] mustBe offer2.groups.head.key
      (groups2 \ 0 \ "label").as[String] mustBe offer2.groups.head.label
      (groups2 \ 0 \ "permissions" \ 0 \ "key")
        .as[String] mustBe offer2.groups.head.permissions.head.key
      (groups2 \ 0 \ "permissions" \ 0 \ "label")
        .as[String] mustBe offer2.groups.head.permissions.head.label
    }

    "remove offer on organisation and consent already use this offer" in {
      val orgKey: String = "orgRemoveOffer"

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

      val offerKey1 = "offer1"
      val offer1: Offer = Offer(
        key = offerKey1,
        label = "offer one",
        groups = Seq(
          PermissionGroup(
            key = "maifNotifs",
            label =
              "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF",
            permissions = Seq(
              Permission(key = "phone", label = "Par contact téléphonique"),
              Permission(key = "mail", label = "Par contact électronique"),
              Permission(key = "sms", label = "Par SMS / MMS / VMS")
            )
          )
        )
      )

      val offerKey2 = "offer2"
      val offer2: Offer = Offer(
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

      postJson(s"/$tenant/organisations", orgWithOffer.asJson).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", Json.obj()).status mustBe OK

      // offer 1 with version 1
      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED

      // offer 2 with version 2
      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED

      val userId = "userRemoveOffer"
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
              label = "offer one",
              version = 1,
              groups = Seq(
                ConsentGroup(
                  key = "maifNotifs",
                  label =
                    "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF",
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
              label = "offer two",
              version = 1,
              groups = Seq(
                ConsentGroup(
                  key = "maifPartnerNotifs",
                  label =
                    "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales des partenaires du groupe MAIF",
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

      delete(s"/$tenant/organisations/$orgKey/offers/$offerKey1").status mustBe OK

      val response: WSResponse =
        getJson(s"/$tenant/organisations/$orgKey/users/$userId")
      response.status mustBe OK

      val value = response.json

      (value \ "offers").as[JsArray].value.length mustBe 1
    }

    "initialize offer with ndJSon binary file" in {
      val orgKey: String = "orgInit"

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

      val offerKey1 = "offer1"
      val offer1: Offer = Offer(
        key = offerKey1,
        label = "offer one",
        groups = Seq(
          PermissionGroup(
            key = "maifNotifs",
            label =
              "J'accepte de recevoir par téléphone, mail et SMS des offres commerciales du groupe MAIF",
            permissions = Seq(
              Permission(key = "phone", label = "Par contact téléphonique"),
              Permission(key = "mail", label = "Par contact électronique"),
              Permission(key = "sms", label = "Par SMS / MMS / VMS")
            )
          )
        )
      )

      val offerKey2 = "offer2"
      val offer2: Offer = Offer(
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

      postJson(s"/$tenant/organisations", orgWithOffer.asJson).status mustBe CREATED
      postJson(s"/$tenant/organisations/$orgKey/draft/_release", Json.obj()).status mustBe OK

      // offer 1 with version 1
      postJson(s"/$tenant/organisations/$orgKey/offers", offer1.asJson()).status mustBe CREATED

      // offer 2 with version 2
      postJson(s"/$tenant/organisations/$orgKey/offers", offer2.asJson()).status mustBe CREATED

      val userId = "userByFile"

      //ERROR 415

      postJson(
        s"/$tenant/organisations/$orgKey/offers/$offerKey1/_init",
        Json.obj(),
        Seq(
          ACCEPT -> "application/xml",
          CONTENT_TYPE -> "application/xml"
        )
      ).status mustBe UNSUPPORTED_MEDIA_TYPE

      //INIT BY JSON
      val tempJson = File.createTempFile("init", "ndjson")

      var bw = new BufferedWriter(new FileWriter(tempJson))
      bw.write(
        "{\"userId\": \"JSON_1_11_42\", \"date\": \"2012-12-21T06:06:06Z\"}\n")
      bw.write(
        "{\"userId\": \"JSON_2_11_42\", \"date\": \"2012-12-21T06:06:06Z\"}\n")
      bw.write(
        "{\"userId\": \"JSON_3_11_42\", \"date\": \"2012-12-21T06:06:06Z\"}\n")
      bw.close()

      val jsonResponse = postBinaryFile(
        s"/$tenant/organisations/$orgKey/offers/$offerKey1/_init",
        tempJson,
        true,
        Seq(
          ACCEPT -> "application/json",
          CONTENT_TYPE -> "application/json"
        )
      )
      jsonResponse.status mustBe OK
      tempJson.delete()

      val jsonResponseValue = jsonResponse.json
      jsonResponseValue.as[JsArray].value.length mustBe 3
      (jsonResponseValue \ 0 \ "status").as[Boolean] mustBe true
      (jsonResponseValue \ 1 \ "status").as[Boolean] mustBe true
      (jsonResponseValue \ 2 \ "status").as[Boolean] mustBe true

      var listResponse = getJson(s"/$tenant/organisations/$orgKey/users")
      listResponse.status mustBe OK
      (listResponse.json \ "items").as[JsArray].value.length mustBe 3

      //INIT BY CSV

      val tempCsv = File.createTempFile("init", "csv")

      bw = new BufferedWriter(new FileWriter(tempCsv))
      bw.write("CSV_1_11_42;2012-12-21T06:06:06Z\n")
      bw.write("CSV_2_11_42;2012-12-21T06:06:06Z\n")
      bw.write("CSV_3_11_42;2012-12-21T06:06:06Z\n")
      bw.close()

      val csvResponse = postBinaryFile(
        s"/$tenant/organisations/$orgKey/offers/$offerKey1/_init",
        tempCsv,
        true,
        Seq(
          ACCEPT -> "application/csv",
          CONTENT_TYPE -> "application/csv"
        )
      )
      csvResponse.status mustBe OK
      tempCsv.delete()

      val csvResponseValue = jsonResponse.json
      csvResponseValue.as[JsArray].value.length mustBe 3
      (csvResponseValue \ 0 \ "status").as[Boolean] mustBe true
      (csvResponseValue \ 1 \ "status").as[Boolean] mustBe true
      (csvResponseValue \ 2 \ "status").as[Boolean] mustBe true

      listResponse = getJson(s"/$tenant/organisations/$orgKey/users")
      listResponse.status mustBe OK
      (listResponse.json \ "items").as[JsArray].value.length mustBe 6

      //ERROR WITH USER ALREADY OFFER AND SAME DATE
      val tempError = File.createTempFile("init_error", "ndjson")

      bw = new BufferedWriter(new FileWriter(tempError))
      bw.write(
        "{\"userId\": \"JSON_1_11_42\", \"date\": \"2012-12-20T06:06:06Z\"}\n")
      bw.close()

      val userExistResponse = postBinaryFile(
        s"/$tenant/organisations/$orgKey/offers/$offerKey1/_init",
        tempError,
        true,
        Seq(
          ACCEPT -> "application/json",
          CONTENT_TYPE -> "application/json"
        )
      )
      userExistResponse.status mustBe OK
      (userExistResponse.json \ 0 \ "status").as[Boolean] mustBe false
      tempError.delete()

      //USER ALREaDY EXIST WOITH OTHER OFFER
      val tempExist = File.createTempFile("exist", "ndjson")
      bw = new BufferedWriter(new FileWriter(tempExist))
      bw.write(
        "{\"userId\": \"JSON_1_11_42\", \"date\": \"2012-12-20T06:06:06Z\"}\n")
      bw.close()

      val existResponse = postBinaryFile(
        s"/$tenant/organisations/$orgKey/offers/$offerKey2/_init",
        tempExist,
        true,
        Seq(
          ACCEPT -> "application/json",
          CONTENT_TYPE -> "application/json"
        )
      )
      existResponse.status mustBe OK
      (existResponse.json.as[JsArray] \ 0 \ "status").as[Boolean] mustBe true

      var userResponse =
        getJson(s"/$tenant/organisations/$orgKey/users/JSON_1_11_42")

      userResponse.status mustBe OK
      (userResponse.json \ "offers").as[JsArray].value.length mustBe 2

    }
  }
}
