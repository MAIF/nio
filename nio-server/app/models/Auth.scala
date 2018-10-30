package models

import cats.data.Validated._
import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.json.{JsValue, _}
import utils.Result
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}

case class Auth(email: String, password: String)

object Auth extends ReadableEntity[Auth] {
  implicit val format: OFormat[Auth] = Json.format[Auth]
  implicit val readXml: XMLRead[Auth] = (node: NodeSeq, path: Option[String]) =>
    (
      (node \ "email").validate[String](Some(s"${path.convert()}email")),
      (node \ "password").validate[String](Some(s"${path.convert()}password"))
    ).mapN((email, password) => Auth(email, password))
  override def fromXml(xml: Elem): Either[Result.AppErrors, Auth] = ???

  override def fromJson(json: JsValue): Either[Result.AppErrors, Auth] =
    json.validate[Auth] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}
