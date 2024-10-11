package models

import java.time.{Clock, LocalDateTime}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.JsValue
import utils.DateUtils
import utils.Result.AppErrors

import scala.xml.{Elem, NodeBuffer}

class ModelValidationSpec extends AnyWordSpecLike with Matchers {

  val now: LocalDateTime = LocalDateTime.now(Clock.systemUTC)

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
              checked = false,
              expiredAt = Some(LocalDateTime.now().plusMinutes(20))
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

      val xml: Elem                                         = consentFact.asXml()
      val consentFactEither: Either[AppErrors, ConsentFact] = ConsentFact.fromXml(xml)
      consentFactEither.isRight must be(true)

      val consentFactFromXml: ConsentFact = consentFactEither.toOption.get

      checkConsentFact(consentFactFromXml)
    }

    "xml invalid" in {
      val xml: Elem                                         = invalidConsentFact(consentFact)
      val consentFactEither: Either[AppErrors, ConsentFact] =
        ConsentFact.fromXml(xml)

      val appErrors: AppErrors = consentFactEither.left.getOrElse(AppErrors())

      appErrors.errors.head.message must be("unknow.path.consentFact.userId")
      appErrors.errors(1).message must be("unknow.path.consentFact.doneBy.userId")
      appErrors.errors(2).message must be("unknow.path.consentFact.doneBy.role")
      appErrors.errors(3).message must be("unknow.path.consentFact.version")
      appErrors.errors(4).message must be("unknow.path.consentFact.groups.0.key")
      appErrors.errors(5).message must be("unknow.path.consentFact.groups.0.label")
      appErrors.errors(6).message must be("unknow.path.consentFact.groups.0.consents.0.key")
      appErrors.errors(7).message must be("unknow.path.consentFact.groups.0.consents.0.label")
      appErrors.errors(8).message must be("unknow.path.consentFact.groups.0.consents.0.checked")
      appErrors.errors(9).message must be("unknow.path.consentFact.groups.0.consents.1.key")
      appErrors.errors(10).message must be("unknow.path.consentFact.groups.0.consents.1.label")
      appErrors.errors(11).message must be("unknow.path.consentFact.groups.0.consents.1.checked")
      appErrors.errors(12).message must be("unknow.path.consentFact.groups.1.key")
      appErrors.errors(13).message must be("unknow.path.consentFact.groups.1.label")
      appErrors.errors(14).message must be("unknow.path.consentFact.groups.1.consents.0.key")
      appErrors.errors(15).message must be("unknow.path.consentFact.groups.1.consents.0.label")
      appErrors.errors(16).message must be("unknow.path.consentFact.groups.1.consents.0.checked")
      appErrors.errors(17).message must be("unknow.path.consentFact.groups.1.consents.1.key")
      appErrors.errors(18).message must be("unknow.path.consentFact.groups.1.consents.1.label")
      appErrors.errors(19).message must be("unknow.path.consentFact.groups.1.consents.1.checked")
      appErrors.errors(20).message must be("unknow.path.consentFact.metaData.0.@key")
      appErrors.errors(21).message must be("unknow.path.consentFact.metaData.0.@value")
      appErrors.errors(22).message must be("unknow.path.consentFact.metaData.1.@key")
      appErrors.errors(23).message must be("unknow.path.consentFact.metaData.1.@value")
    }

    "json serialize/deserialize" in {
      val json: JsValue                                     = consentFact.asJson()
      val consentFactEither: Either[AppErrors, ConsentFact] =
        ConsentFact.fromJson(json)

      consentFactEither.isRight must be(true)
      checkConsentFact(consentFactEither.getOrElse(null))
    }

    def invalidConsentFact(consentFact: ConsentFact): Elem = <consentFact>
      <invalidUserId>{consentFact.userId}</invalidUserId>
      <doneBy>
        <invalidUserId>{consentFact.doneBy.userId}</invalidUserId>
        <invalidRole>{consentFact.doneBy.role}</invalidRole>
      </doneBy>
      <invalidVersion>{consentFact.version}</invalidVersion>
      <groups>
        {
      consentFact.groups.map(group =>
        <consentGroup>
        <invalidKey>{group.key}</invalidKey>
        <invalidLabel>{group.label}</invalidLabel>
        <consents>
          {
          group.consents.map(consent => <consent>
          <invalidKey>{consent.key}</invalidKey>
          <invalidLabel>{consent.label}</invalidLabel>
          <invalidChecked>{consent.checked}</invalidChecked>
        </consent>)
        }
        </consents>
      </consentGroup>
      )
    }
      </groups>
      <invalidLastUpdate>{consentFact.lastUpdate.format(DateUtils.utcDateFormatter)}</invalidLastUpdate>
      <invalidOrgKey>{consentFact.orgKey.getOrElse("")}</invalidOrgKey>
      {consentFact.metaData.map { md =>
          <metaData>
            {md.map(e => <metaDataEntry invalidKey={e._1} invalidValue={e._2}/>)}
          </metaData>
        }.getOrElse(new NodeBuffer())
    }
    </consentFact>

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

      consentFact.lastUpdate.format(DateUtils.utcDateFormatter) must be(now.format(DateUtils.utcDateFormatter))

      consentFact.orgKey.get must be("orgKey")

      consentFact.metaData must be(Some(Map("key1" -> "value1", "key2" -> "value2")))
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
      val xml: Elem                                 = account.asXml()
      val accountEither: Either[AppErrors, Account] = Account.fromXml(xml)

      accountEither.isRight must be(true)

      checkAccount(accountEither.getOrElse(null))
    }

    "xml invalid" in {
      val xml: Elem                                 = invalidAccount(account)
      val accountEither: Either[AppErrors, Account] = Account.fromXml(xml)

      val appErrors: AppErrors = accountEither.left.getOrElse(AppErrors())

      appErrors.errors.head.message must be("unknow.path.account.accountId")
      appErrors.errors(1).message must be("unknow.path.account.organisationsUsers.0.userId")
      appErrors.errors(2).message must be("unknow.path.account.organisationsUsers.0.orgKey")
      appErrors.errors(3).message must be("unknow.path.account.organisationsUsers.1.userId")
      appErrors.errors(4).message must be("unknow.path.account.organisationsUsers.1.orgKey")
    }

    "json serialize/deserialize" in {
      val json: JsValue                             = account.asJson()
      val accountEither: Either[AppErrors, Account] = Account.fromJson(json)

      accountEither.isRight must be(true)

      checkAccount(accountEither.getOrElse(null))
    }

    def invalidAccount(account: Account): Elem = <account>
    <invalidAccountId>{account.accountId}</invalidAccountId>
      <invalidLastUpdate>{account.lastUpdate.format(DateUtils.utcDateFormatter)}</invalidLastUpdate>
      <organisationsUsers>
        {
      account.organisationsUsers.map(ou => <organisationUser>
        <invalidUserId>
          {ou.userId}
        </invalidUserId>
        <invalidOrgKey>
          {ou.orgKey}
        </invalidOrgKey>
      </organisationUser>)
    }
      </organisationsUsers>
    </account>

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
      val xml: Elem                                           = organisation.asXml()
      val organisationEither: Either[AppErrors, Organisation] =
        Organisation.fromXml(xml)

      organisationEither.isRight must be(true)

      checkOrganisation(organisationEither.getOrElse(null))
    }

    "xml invalid" in {
      val xml: Elem                                           = invalidOrganisation(organisation)
      val organisationEither: Either[AppErrors, Organisation] =
        Organisation.fromXml(xml)

      val appErrors: AppErrors = organisationEither.left.getOrElse(AppErrors())

      appErrors.errors.head.message must be("unknow.path.organisation.key")
      appErrors.errors(1).message must be("unknow.path.organisation.label")
      appErrors.errors(2).message must be("unknow.path.organisation.version.status")
      appErrors.errors(3).message must be("unknow.path.organisation.version.num")
      appErrors.errors(4).message must be("unknow.path.organisation.version.latest")
      appErrors.errors(5).message must be("unknow.path.organisation.groups.0.key")
      appErrors.errors(6).message must be("unknow.path.organisation.groups.0.label")
      appErrors.errors(7).message must be("unknow.path.organisation.groups.0.permissions.0.key")
      appErrors.errors(8).message must be("unknow.path.organisation.groups.0.permissions.0.label")
      appErrors.errors(9).message must be("unknow.path.organisation.groups.0.permissions.1.key")
      appErrors.errors(10).message must be("unknow.path.organisation.groups.0.permissions.1.label")
      appErrors.errors(11).message must be("unknow.path.organisation.groups.1.key")
      appErrors.errors(12).message must be("unknow.path.organisation.groups.1.label")
      appErrors.errors(13).message must be("unknow.path.organisation.groups.1.permissions.0.key")
      appErrors.errors(14).message must be("unknow.path.organisation.groups.1.permissions.0.label")
      appErrors.errors(15).message must be("unknow.path.organisation.groups.1.permissions.1.key")
      appErrors.errors(16).message must be("unknow.path.organisation.groups.1.permissions.1.label")

    }

    "json serialize/deserialize" in {
      val json: JsValue                                       = organisation.asJson()
      val organisationEither: Either[AppErrors, Organisation] =
        Organisation.fromJson(json)

      organisationEither.isRight must be(true)

      checkOrganisation(organisationEither.getOrElse(null))
    }

    def invalidOrganisation(organisation: Organisation): Elem = <organisation>
      <invalidKey>{organisation.key}</invalidKey>
      <invalidLabel>{organisation.label}</invalidLabel>
      <version>
        <invalidStatus>{organisation.version.status}</invalidStatus>
        <invalidNum>{organisation.version.num}</invalidNum>
        <invalidLatest>{organisation.version.latest}</invalidLatest>
        <invalidLastUpdate>{organisation.version.lastUpdate.format(DateUtils.utcDateFormatter)}</invalidLastUpdate>
      </version>
      <groups>{
      organisation.groups.map(group =>
        <permissionGroup>
        <invalidKey>{group.key}</invalidKey>
        <invalidLabel>{group.label}</invalidLabel>
        <permissions>{
          group.permissions.map(permission => <permission>
          <invalidKey>{permission.key}</invalidKey>
          <invalidLabel>{permission.label}</invalidLabel>
        </permission>)
        }</permissions>
      </permissionGroup>
      )
    }</groups>
    </organisation>

    def checkOrganisation(organisation: Organisation): Unit = {
      organisation.key must be("orgKey1")

      organisation.version.status must be("RELEASED")
      organisation.version.num must be(2)
      organisation.version.latest must be(true)
      organisation.version.lastUpdate
        .format(DateUtils.utcDateFormatter) must be(now.format(DateUtils.utcDateFormatter))

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
      val xml: Elem                               = tenant.asXml()
      val tenantEither: Either[AppErrors, Tenant] = Tenant.fromXml(xml)

      checkTenant(tenantEither.getOrElse(null))
    }

    "xml invalid" in {
      val xml: Elem                               = invalidTenant(tenant)
      val tenantEither: Either[AppErrors, Tenant] = Tenant.fromXml(xml)

      val appErrors: AppErrors = tenantEither.left.getOrElse(AppErrors())

      appErrors.errors.head.message must be("unknow.path.tenant.key")
      appErrors.errors(1).message must be("unknow.path.tenant.description")
    }

    "json serialize/deserialize" in {
      val json: JsValue                           = tenant.asJson()
      val tenantEither: Either[AppErrors, Tenant] = Tenant.fromJson(json)

      checkTenant(tenantEither.getOrElse(null))
    }

    def checkTenant(tenant: Tenant): Unit = {
      tenant.key must be("tenant1")
      tenant.description must be("tenant 1")
    }

    def invalidTenant(tenant: Tenant): Elem = <tenant>
      <invalidKey>{tenant.key}</invalidKey>
      <invalidDescription>{tenant.description}</invalidDescription>
    </tenant>
  }

  "Validation Offer" should {

    val offer: Offer = Offer(
      key = "offer1",
      label = "offer 1",
      version = 4,
      groups = Seq(
        PermissionGroup(
          key = "keyGroup",
          label = "labelGroup",
          permissions = Seq(
            Permission(
              key = "keyPerm",
              label = "labelPerm"
            )
          )
        )
      )
    )

    "xml serialize/deserialize" in {
      val xml: Elem                             = offer.asXml()
      val offerEither: Either[AppErrors, Offer] = Offer.fromXml(xml)

      checkOffer(offerEither.getOrElse(null))
    }

    "xml invalid" in {
      val xml: Elem                             = invalidOffer(offer)
      val offerEither: Either[AppErrors, Offer] = Offer.fromXml(xml)

      val appErrors: AppErrors = offerEither.left.getOrElse(AppErrors())

      appErrors.errors.head.message must be("unknow.path.offer.key")
      appErrors.errors(1).message must be("unknow.path.offer.label")
      appErrors.errors(2).message must be("unknow.path.offer.groups")
    }

    "json serialize/deserialize" in {
      val json: JsValue                         = offer.asJson()
      val offerEither: Either[AppErrors, Offer] = Offer.fromJson(json)

      checkOffer(offerEither.getOrElse(null))
    }

    def checkOffer(offer: Offer): Unit = {
      offer.key must be("offer1")
      offer.label must be("offer 1")
      offer.version must be(4)
      offer.groups.size must be(1)
      offer.groups.head.key must be("keyGroup")
      offer.groups.head.label must be("labelGroup")
      offer.groups.head.permissions.size must be(1)
      offer.groups.head.permissions.head.key must be("keyPerm")
      offer.groups.head.permissions.head.label must be("labelPerm")
    }

    def invalidOffer(offer: Offer): Elem = <offer>
      <invalidKey>{offer.key}</invalidKey>
      <invalidLabel>{offer.label}</invalidLabel>
    </offer>
  }
}
