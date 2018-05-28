package models

import controllers.ReadableEntity
import play.api.libs.json._

import scala.util.{Failure, Success, Try}
import scala.xml.Elem


case class AppDone(orgKey: String, userId: String, appId: String) {
  def asJson = AppDone.appDoneFormats.writes(this)
}
object AppDone {
  implicit val appDoneFormats = Json.format[AppDone]
}

case class AppIds(appIds: Seq[String]) {
  def asXml = {
    <appIds>
      {appIds.map(appId => <appId>{appId}</appId>)}
    </appIds>
  }
}
object AppIds extends ReadableEntity[AppIds] {
  implicit val appIdsFormats = Json.format[AppIds]

  def fromXml(xml: Elem) = {
    Try {
      val appIds = (xml \\ "appId").map(_.head.text)
      AppIds(appIds)
    } match {
      case Success(value) => Right(value)
      case Failure(throwable) => {
        Left(throwable.getMessage)
      }
    }
  }

  def fromJson(json: JsValue) = {
    json.validate[AppIds](appIdsFormats) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(errors.mkString(", "))
    }
  }
}

case class Digest(digest: String) extends AnyVal

object Digest {

  def fromJson(jsValue: JsValue): Either[String, Digest] = {
    jsValue match {
      case JsString(str) => Right(Digest(str))
      case _             => Left("invalid.format")
    }
  }
}