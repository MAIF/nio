package models

import cats.data.Validated._
import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.{JsValue, _}
import utils.DateUtils
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}
import scala.collection.Seq

case class OrganisationUser(userId: String, orgKey: String) extends ModelTransformAs {
  override def asXml(): Elem = <organisationUser>
    <userId>
      {userId}
    </userId>
    <orgKey>
      {orgKey}
    </orgKey>
  </organisationUser>.clean()

  override def asJson(): JsValue = OrganisationUser.write.writes(this)
}

object OrganisationUser {

  implicit val read: Reads[OrganisationUser] = (
    (__ \ "userId").read[String] and
      (__ \ "orgKey").read[String]
  )(OrganisationUser.apply _)

  implicit val write: Writes[OrganisationUser] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "orgKey").write[String]
  )(unlift(OrganisationUser.unapply))

  implicit val format: Format[OrganisationUser] = Format(read, write)

  implicit val readXml: XMLRead[OrganisationUser] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "userId").validate[String](Some(s"${path.convert()}userId")),
        (node \ "orgKey").validate[String](Some(s"${path.convert()}orgKey"))
      ).mapN(OrganisationUser.apply)

  def fromJson(json: JsValue): Either[String, OrganisationUser] =
    json
      .validate[OrganisationUser]
      .asEither
      .left
      .map(error => error.mkString(", "))
}

case class Account(
    accountId: String,
    lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
    organisationsUsers: Seq[OrganisationUser]
) extends ModelTransformAs {
  override def asXml(): Elem = <account>
    <accountId>
      {accountId}
    </accountId>
    <lastUpdate>
      {lastUpdate.toString(DateUtils.utcDateFormatter)}
    </lastUpdate>
    <organisationsUsers>
      {organisationsUsers.map(_.asXml())}
    </organisationsUsers>
  </account>.clean()

  override def asJson(): JsValue = Account.write.writes(this)
}

object Account extends ReadableEntity[Account] {

  implicit val read: Reads[Account] = (
    (__ \ "accountId").read[String] and
      (__ \ "lastUpdate")
        .readWithDefault[DateTime](DateTime.now(DateTimeZone.UTC))(DateUtils.utcDateTimeReads) and
      (__ \ "organisationsUsers").read[Seq[OrganisationUser]]
  )(Account.apply _)

  implicit val write: OWrites[Account] = (
    (JsPath \ "accountId").write[String] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "organisationsUsers").write[Seq[OrganisationUser]]
  )(unlift(Account.unapply))

  implicit val format: Format[Account]   = Format(read, write)
  implicit val oformat: OFormat[Account] = OFormat(read, write)

  implicit val readXml: XMLRead[Account] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "accountId").validate[String](Some("account.accountId")),
        (node \ "organisationsUsers").validate[Seq[OrganisationUser]](Some("account.organisationsUsers"))
      ).mapN((accountId, organisationsUsers) => Account(accountId = accountId, organisationsUsers = organisationsUsers))

  override def fromXml(xml: Elem): Either[AppErrors, Account] =
    readXml.read(xml, Some("account")).toEither

  override def fromJson(json: JsValue): Either[AppErrors, Account] =
    json
      .validate[Account]
      .asEither
      .left
      .map(error => AppErrors.fromJsError(error))
}

case class Accounts(accounts: Seq[Account]) extends ModelTransformAs {
  override def asXml(): Elem = <accounts>
      {accounts.map(_.asXml())}
    </accounts>.clean()

  override def asJson(): JsValue = JsArray(accounts.map(_.asJson()))
}

object Accounts {}
