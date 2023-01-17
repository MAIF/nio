package models

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

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

      fromXml.map(_.key) mustBe Right(org.key)
    }

    "serialize/deserialize from JSON" in {
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

      val json = org.asJson()

      val fromJson = Organisation.fromJson(json)

      fromJson.map(_.key) mustBe Right(org.key)
    }

    "serialize/deserialize from XML with opt type" in {
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
                label = "a1",
                `type` = OptOut
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
      fromXml.map(_.key) mustBe Right(org.key)
    }

  }

  "serialize/deserialize from JSON with opt type" in {
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
              label = "a1",
              `type` = OptOut
            ),
            Permission(
              key = "a2",
              label = "a2"
            )
          )
        )
      )
    )

    val json = org.asJson()
println(Json.prettyPrint(json))
    val fromJson = Organisation.fromJson(json)

    fromJson.map(_.key) mustBe Right(org.key)
  }

  "parse from JSON with opt type missing" in {
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
              label = "a1",
              `type` = OptOut
            ),
            Permission(
              key = "a2",
              label = "a2"
            )
          )
        )
      )
    )

    val json = Json.parse("""
        {
          "key" : "maif",
          "label" : "test org",
          "version" : {
            "status" : "DRAFT",
            "num" : 1,
            "latest" : false,
            "lastUpdate" : "2023-01-17T09:47:56Z"
          },
          "groups" : [ {
            "key" : "a",
            "label" : "a",
            "permissions" : [ {
              "key" : "a1",
              "label" : "a1",
              "type" : "OptOut"
            }, {
              "key" : "a2",
              "label" : "a2"
            } ]
          } ]
        } """)

    val fromJson = Organisation.fromJson(json)

    fromJson.map(_.key) mustBe Right(org.key)
  }

}
