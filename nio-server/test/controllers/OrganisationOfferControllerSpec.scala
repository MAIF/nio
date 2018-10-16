package controllers

import models._
import org.joda.time.{DateTime, DateTimeZone}
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
  }
}
