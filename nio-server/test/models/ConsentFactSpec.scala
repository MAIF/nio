package models

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.PlaySpec

class ConsentFactSpec extends PlaySpec with WordSpecLike with MustMatchers {

  "ConsentFact" should {

    val mdKey1 = "toto"
    val mdVal1 = "blabla"

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
      metaData = Some(Map(mdKey1 -> mdVal1, "tata" -> "val2"))
    )

    val consentFactWithOffers = consentFact
      .copy(
        offers = Some(Seq(
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
        )))
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
          )))

    val consentFactWithoutMetaData = consentFact.copy(metaData = None)

    "serialize/deserialize from XML" in {
      val xml = consentFact.asXml

      val fromXml = ConsentFact.fromXml(xml)

      fromXml.isRight mustBe true
      val cf = fromXml.right.get
      cf.userId mustBe consentFact.userId
      cf.metaData.isDefined mustBe true

      val md = cf.metaData.get
      md(mdKey1) mustBe mdVal1

      val xml2 = consentFactWithoutMetaData.asXml
      xml2.contains("metaData") mustBe false
    }

    "serialize/deserialize from JSON" in {
      val json = consentFact.asJson

      val fromJson = ConsentFact.fromJson(json)

      fromJson.isRight mustBe true
      val cf = fromJson.right.get
      cf.userId mustBe consentFact.userId
      cf.metaData.isDefined mustBe true

      val md = cf.metaData.get
      md(mdKey1) mustBe mdVal1

      val consentFact2 = consentFactWithoutMetaData
      val asStr = consentFact2.asJson.toString()
      asStr.contains("metaData") mustBe false
    }

    "restricted with pattern" in {
      ConsentFact
        .withRestriction(consentFactWithOffers, Some(Seq(".*")))
        .asJson mustBe consentFactWithOffers.asJson
      ConsentFact
        .withRestriction(consentFactWithOffers, Some(Seq("offer1")))
        .asJson mustBe consentFactWithOffer1.asJson
      ConsentFact
        .withRestriction(consentFactWithOffers, Some(Seq("otherOffer")))
        .offers
        .getOrElse(Seq())
        .length mustBe 0
    }
  }

}
