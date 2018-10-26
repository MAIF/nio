package models

import auth.AuthInfo
import cats.data.Validated._
import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.{JsValue, _}
import reactivemongo.bson.BSONObjectID
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}

case class UserAccount(_id: String = BSONObjectID.generate().stringify,
                       email: String,
                       password: String,
                       clientId: String,
                       clientSecret: String,
                       isAdmin: Boolean,
                       offerRestrictionPatterns: Option[Seq[String]] = None)
    extends ModelTransformAs {
  override def asXml(): Elem = <UserAccount>
    <email>
      {email}
    </email>
    <clientId>
      {clientId}
    </clientId>
    <clientSecret>
      {clientSecret}
    </clientSecret>
    <isAdmin>
      {isAdmin}
    </isAdmin>{offerRestrictionPatterns
      .map(o =>
        <offerRestrictionPatterns>
          {o.map(
          l => <offerRestrictionPattern>
            {l}
          </offerRestrictionPattern>
        )}
        </offerRestrictionPatterns>
      )}
  </UserAccount>.clean()

  override def asJson(): JsValue = UserAccount.writeWithoutId.writes(this)

  def toAuthInfo(): AuthInfo = {
    val maybeOfferRestrictionPatterns: Option[Seq[String]] = isAdmin match {
      case true  => Some(Seq("*"))
      case false => offerRestrictionPatterns
    }

    AuthInfo(email, isAdmin, None, maybeOfferRestrictionPatterns)
  }
}

object UserAccount extends ReadableEntity[UserAccount] {

  implicit val read: Reads[UserAccount] = (
    (__ \ "_id").readNullable[String].map { mayBeId =>
      mayBeId.getOrElse(BSONObjectID.generate().stringify)
    } and
      (__ \ "email").read[String] and
      (__ \ "password").read[String] and
      (__ \ "clientId").read[String] and
      (__ \ "clientSecret").read[String] and
      (__ \ "isAdmin").read[Boolean] and
      (__ \ "offerRestrictionPatterns").readNullable[Seq[String]]
  )(UserAccount.apply _)

  implicit val writeWithoutId: Writes[UserAccount] = Writes { userAccount =>
    Json.obj(
      "email" -> userAccount.email,
      "clientId" -> userAccount.clientId,
      "clientSecret" -> userAccount.clientSecret,
      "isAdmin" -> userAccount.isAdmin,
      "offerRestrictionPatterns" -> userAccount.offerRestrictionPatterns
    )
  }

  implicit val write: Writes[UserAccount] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "clientId").write[String] and
      (JsPath \ "clientSecret").write[String] and
      (JsPath \ "isAdmin").write[Boolean] and
      (JsPath \ "offerRestrictionPatterns").writeNullable[Seq[String]]
  )(unlift(UserAccount.unapply))

  implicit val owrite: OWrites[UserAccount] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "clientId").write[String] and
      (JsPath \ "clientSecret").write[String] and
      (JsPath \ "isAdmin").write[Boolean] and
      (JsPath \ "offerRestrictionPatterns").writeNullable[Seq[String]]
  )(unlift(UserAccount.unapply))

  implicit val formats: Format[UserAccount] = Format(read, write)
  implicit val oformats: OFormat[UserAccount] = OFormat(read, owrite)

  implicit val readXml
    : XMLRead[UserAccount] = (node: NodeSeq, path: Option[String]) =>
    (
      (node \ "_id").validateNullable[String](BSONObjectID.generate().stringify,
                                              Some(s"${path.convert()}_id")),
      (node \ "email").validate[String](Some(s"${path.convert()}email")),
      (node \ "password").validate[String](Some(s"${path.convert()}password")),
      (node \ "clientId").validate[String](Some(s"${path.convert()}clientId")),
      (node \ "clientSecret").validate[String](
        Some(s"${path.convert()}clientSecret")),
      (node \ "isAdmin").validate[Boolean](Some(s"${path.convert()}isAdmin")),
      (node \ "offerRestrictionPatterns").validateNullable[Seq[String]](
        Some(s"${path.convert()}offerRestrictionPatterns"))
    ).mapN(
      (_id,
       email,
       password,
       clientId,
       clientSecret,
       isAdmin,
       offerRestrictionPatterns) =>
        UserAccount(
          _id = _id,
          email = email,
          password = password,
          clientId = clientId,
          clientSecret = clientSecret,
          isAdmin = isAdmin,
          offerRestrictionPatterns = offerRestrictionPatterns
      )
  )

  def fromXml(xml: Elem): Either[AppErrors, UserAccount] = {
    readXml.read(xml, Some("UserAccount")).toEither
  }

  def fromJson(json: JsValue) = {
    json.validate[UserAccount] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
  }

}
