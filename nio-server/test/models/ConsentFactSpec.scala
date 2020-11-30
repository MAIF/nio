package models

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec

class ConsentFactSpec extends PlaySpec with AnyWordSpecLike with Matchers {

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

    val consentFactWithoutMetaData = consentFact.copy(metaData = None)

    "serialize/deserialize from XML" in {
      val xml = consentFact.asXml()

      val fromXml = ConsentFact.fromXml(xml)

      fromXml.isRight mustBe true
      fromXml.map(_.userId) mustBe Right(consentFact.userId)
      fromXml.map(_.metaData.isDefined) mustBe Right(true)

      fromXml.map(_.metaData.map(md => md(mdKey1))) mustBe Right(Some(mdVal1))

      val xml2 = consentFactWithoutMetaData.asXml()
      xml2.contains("metaData") mustBe false
    }

    "serialize/deserialize from JSON" in {
      val json = consentFact.asJson()

      val fromJson = ConsentFact.fromJson(json)

      fromJson.isRight mustBe true
      fromJson.map(_.userId) mustBe Right(consentFact.userId)
      fromJson.map(_.metaData.isDefined) mustBe Right(true)
      fromJson.map(_.metaData.map(md => md(mdKey1))) mustBe Right(Some(mdVal1))

      val consentFact2 = consentFactWithoutMetaData
      val asStr        = consentFact2.asJson().toString()
      asStr.contains("metaData") mustBe false
    }
  }

}
