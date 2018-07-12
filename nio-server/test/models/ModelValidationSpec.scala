package models

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import utils.DateUtils
import utils.Result.AppErrors

import scala.xml.Elem

class ModelValidationSpec extends PlaySpec with WordSpecLike with MustMatchers {

  "Validation consent fact" should {
    val now: DateTime = DateTime.now(DateTimeZone.UTC)

    val consentFact: ConsentFact = ConsentFact(
      _id = "1",
      userId = "user1",
      doneBy = DoneBy(
        role = "role",
        userId = "user"
      ),
      version = 1,
      groups = Seq(
        ConsentGroup(
          key = "group1",
          label = "group 1",
          consents = Seq(
            Consent(
              key = "g1c1",
              label = "group 1 consent 1",
              checked = false
            ),
            Consent(
              key = "g1c2",
              label = "group 1 consent 2",
              checked = true
            )
          )
        ),
        ConsentGroup(
          key = "group2",
          label = "group 2",
          consents = Seq(
            Consent(
              key = "g2c1",
              label = "group 2 consent 1",
              checked = true
            ),
            Consent(
              key = "g2c2",
              label = "group 2 consent 2",
              checked = false
            )
          )
        )
      ),
      lastUpdate = now,
      lastUpdateSystem = now,
      orgKey = Some("orgKey"),
      metaData = Some(
        Map(
          "key1" -> "value1",
          "key2" -> "value2"
        )
      )
    )


    "xml serialize/deserialize" in {

      val xml: Elem = consentFact.asXml
      val consentFactEither: Either[AppErrors, ConsentFact] = ConsentFact.fromXml(xml)

      consentFactEither.isRight must be (true)

      val consentFactFromXml: ConsentFact = consentFactEither.right.get

      consentFactFromXml.userId must be ("user1")
      consentFactFromXml.doneBy.role must be ("role")
      consentFactFromXml.doneBy.userId must be ("user")
      consentFactFromXml.version must be (1)

      consentFactFromXml.groups.size must be (2)

      consentFactFromXml.groups.head.key must be ("group1")
      consentFactFromXml.groups.head.label must be ("group 1")
      consentFactFromXml.groups.head.consents.size must be (2)
      consentFactFromXml.groups.head.consents.head.key must be ("g1c1")
      consentFactFromXml.groups.head.consents.head.label must be ("group 1 consent 1")
      consentFactFromXml.groups.head.consents.head.checked must be (false)
      consentFactFromXml.groups.head.consents(1).key must be ("g1c2")
      consentFactFromXml.groups.head.consents(1).label must be ("group 1 consent 2")
      consentFactFromXml.groups.head.consents(1).checked must be (true)
      
      consentFactFromXml.groups(1).key must be ("group2")
      consentFactFromXml.groups(1).label must be ("group 2")
      consentFactFromXml.groups(1).consents.size must be (2)
      consentFactFromXml.groups(1).consents.head.key must be ("g2c1")
      consentFactFromXml.groups(1).consents.head.label must be ("group 2 consent 1")
      consentFactFromXml.groups(1).consents.head.checked must be (true)
      consentFactFromXml.groups(1).consents(1).key must be ("g2c2")
      consentFactFromXml.groups(1).consents(1).label must be ("group 2 consent 2")
      consentFactFromXml.groups(1).consents(1).checked must be (false)

      consentFactFromXml.lastUpdate.toString(DateUtils.utcDateFormatter) must be (now.toString(DateUtils.utcDateFormatter))

      consentFactFromXml.orgKey.get must be ("orgKey")

      consentFactFromXml.metaData.get.toSeq.head must be ("key1", "value1")
      consentFactFromXml.metaData.get.toSeq(1) must be ("key2", "value2")

    }

    "json serialize/deserialize" in {
      val json: JsValue = consentFact.asJson
      val consentFactEither: Either[AppErrors, ConsentFact] = ConsentFact.fromJson(json)

      consentFactEither.isRight must be (true)

      val consentFactFromJson: ConsentFact = consentFactEither.right.get

      consentFactFromJson.userId must be ("user1")
      consentFactFromJson.doneBy.role must be ("role")
      consentFactFromJson.doneBy.userId must be ("user")
      consentFactFromJson.version must be (1)

      consentFactFromJson.groups.size must be (2)

      consentFactFromJson.groups.head.key must be ("group1")
      consentFactFromJson.groups.head.label must be ("group 1")
      consentFactFromJson.groups.head.consents.size must be (2)
      consentFactFromJson.groups.head.consents.head.key must be ("g1c1")
      consentFactFromJson.groups.head.consents.head.label must be ("group 1 consent 1")
      consentFactFromJson.groups.head.consents.head.checked must be (false)
      consentFactFromJson.groups.head.consents(1).key must be ("g1c2")
      consentFactFromJson.groups.head.consents(1).label must be ("group 1 consent 2")
      consentFactFromJson.groups.head.consents(1).checked must be (true)

      consentFactFromJson.groups(1).key must be ("group2")
      consentFactFromJson.groups(1).label must be ("group 2")
      consentFactFromJson.groups(1).consents.size must be (2)
      consentFactFromJson.groups(1).consents.head.key must be ("g2c1")
      consentFactFromJson.groups(1).consents.head.label must be ("group 2 consent 1")
      consentFactFromJson.groups(1).consents.head.checked must be (true)
      consentFactFromJson.groups(1).consents(1).key must be ("g2c2")
      consentFactFromJson.groups(1).consents(1).label must be ("group 2 consent 2")
      consentFactFromJson.groups(1).consents(1).checked must be (false)

      consentFactFromJson.lastUpdate.toString(DateUtils.utcDateFormatter) must be (now.toString(DateUtils.utcDateFormatter))

      consentFactFromJson.orgKey.get must be ("orgKey")

      consentFactFromJson.metaData.get.toSeq.head must be ("key1", "value1")
      consentFactFromJson.metaData.get.toSeq(1) must be ("key2", "value2")

    }

  }

}
