package models

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import utils.DateUtils
import utils.Result.AppErrors

import scala.xml.Elem

class ModelValidationSpec extends PlaySpec with WordSpecLike with MustMatchers {
  val now: DateTime = DateTime.now(DateTimeZone.UTC)

  "Validation ConsentFact" should {
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
      val consentFactEither: Either[AppErrors, ConsentFact] =
        ConsentFact.fromXml(xml)

      consentFactEither.isRight must be(true)

      val consentFactFromXml: ConsentFact = consentFactEither.right.get

      checkConsentFact(consentFactFromXml)
    }

    "json serialize/deserialize" in {
      val json: JsValue = consentFact.asJson
      val consentFactEither: Either[AppErrors, ConsentFact] =
        ConsentFact.fromJson(json)

      consentFactEither.isRight must be(true)

      val consentFactFromJson: ConsentFact = consentFactEither.right.get

      checkConsentFact(consentFactFromJson)
    }

    def checkConsentFact(consentFact: ConsentFact): Unit = {
      consentFact.userId must be("user1")
      consentFact.doneBy.role must be("role")
      consentFact.doneBy.userId must be("user")
      consentFact.version must be(1)

      consentFact.groups.size must be(2)

      consentFact.groups.head.key must be("group1")
      consentFact.groups.head.label must be("group 1")
      consentFact.groups.head.consents.size must be(2)
      consentFact.groups.head.consents.head.key must be("g1c1")
      consentFact.groups.head.consents.head.label must be("group 1 consent 1")
      consentFact.groups.head.consents.head.checked must be(false)
      consentFact.groups.head.consents(1).key must be("g1c2")
      consentFact.groups.head.consents(1).label must be("group 1 consent 2")
      consentFact.groups.head.consents(1).checked must be(true)

      consentFact.groups(1).key must be("group2")
      consentFact.groups(1).label must be("group 2")
      consentFact.groups(1).consents.size must be(2)
      consentFact.groups(1).consents.head.key must be("g2c1")
      consentFact.groups(1).consents.head.label must be("group 2 consent 1")
      consentFact.groups(1).consents.head.checked must be(true)
      consentFact.groups(1).consents(1).key must be("g2c2")
      consentFact.groups(1).consents(1).label must be("group 2 consent 2")
      consentFact.groups(1).consents(1).checked must be(false)

      consentFact.lastUpdate.toString(DateUtils.utcDateFormatter) must be(
        now.toString(DateUtils.utcDateFormatter))

      consentFact.orgKey.get must be("orgKey")

      consentFact.metaData.get.toSeq.head must be("key1", "value1")
      consentFact.metaData.get.toSeq(1) must be("key2", "value2")
    }
  }

  "Validation Account" should {

    val account: Account = Account(
      accountId = "1",
      lastUpdate = now,
      organisationsUsers = Seq(
        OrganisationUser(
          userId = "user1",
          orgKey = "orgKey1"
        ),
        OrganisationUser(
          userId = "user2",
          orgKey = "orgKey2"
        )
      )
    )

    "xml serialize/deserialize" in {
      val xml: Elem = account.asXml()
      val accountEither: Either[AppErrors, Account] = Account.fromXml(xml)

      accountEither.isRight must be(true)

      val accountFromXml: Account = accountEither.right.get

      checkAccount(accountFromXml)
    }

    "json serialize/deserialize" in {
      val json: JsValue = account.asJson()
      val accountEither: Either[AppErrors, Account] = Account.fromJson(json)

      accountEither.isRight must be(true)

      val accountFromJson: Account = accountEither.right.get

      checkAccount(accountFromJson)
    }

    def checkAccount(account: Account): Unit = {
      account.accountId must be("1")

      account.organisationsUsers.head.userId must be("user1")
      account.organisationsUsers.head.orgKey must be("orgKey1")
      account.organisationsUsers(1).userId must be("user2")
      account.organisationsUsers(1).orgKey must be("orgKey2")
    }
  }

  "Validation Organisation" should {

    val organisation: Organisation = Organisation(
      _id = "1",
      key = "orgKey1",
      label = "organisation 1",
      version = VersionInfo(
        status = "RELEASED",
        num = 2,
        latest = true,
        neverReleased = Some(false),
        lastUpdate = now
      ),
      groups = Seq(
        PermissionGroup(
          key = "group1",
          label = "group 1",
          permissions = Seq(
            Permission(
              key = "g1p1",
              label = "group 1 perm 1"
            ),
            Permission(
              key = "g1p2",
              label = "group 1 perm 2"
            )
          )
        ),
        PermissionGroup(
          key = "group2",
          label = "group 2",
          permissions = Seq(
            Permission(
              key = "g2p1",
              label = "group 2 perm 1"
            ),
            Permission(
              key = "g2p2",
              label = "group 2 perm 2"
            )
          )
        )
      )
    )

    "xml serialize/deserialize" in {
      val xml: Elem = organisation.asXml
      val organisationEither: Either[AppErrors, Organisation] =
        Organisation.fromXml(xml)

      organisationEither.isRight must be(true)
      val organisationFromXml: Organisation = organisationEither.right.get

      checkOrganisation(organisationFromXml)
    }

    "json serialize/deserialize" in {
      val json: JsValue = organisation.asJson
      val organisationEither: Either[AppErrors, Organisation] =
        Organisation.fromJson(json)

      organisationEither.isRight must be(true)
      val organisationFromJson: Organisation = organisationEither.right.get

      checkOrganisation(organisationFromJson)
    }

    def checkOrganisation(organisation: Organisation): Unit = {
      organisation.key must be("orgKey1")

      organisation.version.status must be("RELEASED")
      organisation.version.num must be(2)
      organisation.version.latest must be(true)
      organisation.version.lastUpdate
        .toString(DateUtils.utcDateFormatter) must be(
        now.toString(DateUtils.utcDateFormatter))

      organisation.groups.size must be(2)
      organisation.groups.head.key must be("group1")
      organisation.groups.head.label must be("group 1")
      organisation.groups.head.permissions.size must be(2)
      organisation.groups.head.permissions.head.key must be("g1p1")
      organisation.groups.head.permissions.head.label must be("group 1 perm 1")
      organisation.groups.head.permissions(1).key must be("g1p2")
      organisation.groups.head.permissions(1).label must be("group 1 perm 2")
      organisation.groups(1).key must be("group2")
      organisation.groups(1).label must be("group 2")
      organisation.groups(1).permissions.size must be(2)
      organisation.groups(1).permissions.head.key must be("g2p1")
      organisation.groups(1).permissions.head.label must be("group 2 perm 1")
      organisation.groups(1).permissions(1).key must be("g2p2")
      organisation.groups(1).permissions(1).label must be("group 2 perm 2")
    }
  }

  "Validation Tenant" should {

    val tenant: Tenant = Tenant(
      key = "tenant1",
      description = "tenant 1"
    )

    "xml serialize/deserialize" in {
      val xml: Elem = tenant.asXml
      val tenantEither: Either[AppErrors, Tenant] = Tenant.fromXml(xml)

      val tenantFromXml: Tenant = tenantEither.right.get
      checkTenant(tenantFromXml)
    }

    "json serialize/deserialize" in {
      val json: JsValue = tenant.asJson
      val tenantEither: Either[AppErrors, Tenant] = Tenant.fromJson(json)

      val tenantFromJson: Tenant = tenantEither.right.get
      checkTenant(tenantFromJson)
    }

    def checkTenant(tenant: Tenant): Unit = {
      tenant.key must be("tenant1")
      tenant.description must be("tenant 1")
    }
  }
}
