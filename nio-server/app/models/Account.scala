package models

import controllers.ReadableEntity
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JsValue
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import utils.DateUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node}
import XmlUtil.XmlCleaner
import utils.Result.AppErrors

case class OrganisationUser(userId: String, orgKey: String)
    extends ModelTransformAs {
  override def asXml(): Elem = <organisationUser>
    <userId>{userId}</userId>
    <orgKey>{orgKey}</orgKey>
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

  def fromXml(xml: Elem): OrganisationUser = {
    val userId = (xml \ "userId").head.text
    val orgKey = (xml \ "orgKey").head.text

    OrganisationUser(userId, orgKey)
  }

  def fromJson(json: JsValue): Either[String, OrganisationUser] =
    json
      .validate[OrganisationUser]
      .asEither
      .left
      .map(error => error.mkString(", "))
}

case class Account(accountId: String,
                   lastUpdate: DateTime = DateTime.now(DateTimeZone.UTC),
                   organisationsUsers: Seq[OrganisationUser])
    extends ModelTransformAs {
  override def asXml(): Elem = <account>
    <accountId>{accountId}</accountId>
    <lastUpdate>{lastUpdate.toString(DateUtils.utcDateFormatter)}</lastUpdate>
    <organisationsUsers>{organisationsUsers.map(_.asXml())}</organisationsUsers>
  </account>.clean()

  override def asJson(): JsValue = Account.write.writes(this)
}

object Account extends ReadableEntity[Account] {

  implicit val read: Reads[Account] = (
    (__ \ "accountId").read[String] and
      (__ \ "lastUpdate")
        .readWithDefault[DateTime](DateTime.now(DateTimeZone.UTC))(
          DateUtils.utcDateTimeReads) and
      (__ \ "organisationsUsers").read[Seq[OrganisationUser]]
  )(Account.apply _)

  implicit val write: OWrites[Account] = (
    (JsPath \ "accountId").write[String] and
      (JsPath \ "lastUpdate")
        .write[DateTime](DateUtils.utcDateTimeWrites) and
      (JsPath \ "organisationsUsers").write[Seq[OrganisationUser]]
  )(unlift(Account.unapply))

  implicit val format: Format[Account] = Format(read, write)
  implicit val oformat: OFormat[Account] = OFormat(read, write)

  override def fromXml(xml: Elem): Either[AppErrors, Account] = {
    Try {
      val accountId = (xml \ "accountId").head.text
      val organisationsUsersXml: Node = (xml \ "organisationsUsers").head
      val organisationsUsers: Seq[OrganisationUser] =
        organisationsUsersXml.child.collect {
          case e: Elem => OrganisationUser.fromXml(e)
        }

      Account(accountId = accountId, organisationsUsers = organisationsUsers)
    } match {
      case Success(value)     => Right(value)
      case Failure(throwable) => Left(AppErrors.fromXmlError(throwable))
    }
  }

  override def fromJson(json: JsValue): Either[AppErrors, Account] =
    json
      .validate[Account]
      .asEither
      .left
      .map(error => AppErrors.fromJsError(error))
}

case class Accounts(accounts: Seq[Account]) extends ModelTransformAs {
  override def asXml(): Elem =
    <accounts>{accounts.map(_.asXml())}</accounts>.clean()

  override def asJson(): JsValue = JsArray(accounts.map(_.asJson()))
}

object Accounts {}
