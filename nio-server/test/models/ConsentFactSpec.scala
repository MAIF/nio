package models

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.PlaySpec

class ConsentFactSpec extends PlaySpec with WordSpecLike with MustMatchers {

  "ConsentFact" should {

    "serialize/deserialize from XML" in {
      val consentFact = ConsentFact(
        _id = "cf",
        userId = "userId4",
        doneBy = DoneBy("a1", "admin"),
        version = 1,
        groups = Seq(
          ConsentGroup("a", "a", Seq(Consent("a", "a", false))),
          ConsentGroup("b", "b", Seq(Consent("b", "b", false)))
        ),
        lastUpdate = DateTime.now(DateTimeZone.UTC)
      )

      val xml = consentFact.asXml

      val fromXml = ConsentFact.fromXml(xml)

      fromXml.isRight mustBe true
      fromXml.right.get.userId mustBe consentFact.userId
    }

  }

}
