package models

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec

class OrganisationSpec extends PlaySpec with AnyWordSpecLike {

  "Organisation" should {

    "serialize/deserialize from XML" in {
      val org = Organisation(
        _id = "cf",
        key = "maif",
        label = "test org",
        version = VersionInfo(),
        groups = Seq(
          PermissionGroup(
            key = "a",
            label = "a",
            permissions = Seq(
              Permission(
                key = "a1",
                label = "a1"
              ),
              Permission(
                key = "a2",
                label = "a2"
              )
            )
          )
        )
      )

      val xml = org.asXml()

      val fromXml = Organisation.fromXml(xml)

      fromXml.isRight mustBe true
      fromXml.map(_.key) mustBe Right(org.key)
    }

  }

}
