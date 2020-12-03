package models
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.implicits._
import libs.xml.syntax._
import libs.xml.XmlUtil.XmlCleaner
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.Result.AppErrors
import scala.collection.Seq

import scala.xml.{Elem, NodeSeq}

case class UserExtract(email: String) extends ModelTransformAs {
  override def asXml(): Elem = <userExtract>
    <email>{email}</email>
  </userExtract>.clean()

  override def asJson(): JsValue = UserExtract.userExtractWrite.writes(this)
}

object UserExtract extends ReadableEntity[UserExtract] {

  implicit val userExtractRead: Reads[UserExtract] =
    (__ \ "email").read[String].map(UserExtract.apply)

  implicit val userExtractWrite: Writes[UserExtract] = Writes { user =>
    (JsPath \ "email").write[String].writes(user.email)
  }

  implicit val format: Format[UserExtract] =
    Format(userExtractRead, userExtractWrite)

  implicit val userExtractReadXml: XMLRead[UserExtract] =
    (node: NodeSeq, path: Option[String]) => (node \ "email").validate[String].map(email => UserExtract(email))

  override def fromXml(xml: Elem): Either[AppErrors, UserExtract] =
    userExtractReadXml.read(xml, Some("userExtract")).toEither

  override def fromJson(json: JsValue): Either[AppErrors, UserExtract] =
    json.validate[UserExtract] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}
