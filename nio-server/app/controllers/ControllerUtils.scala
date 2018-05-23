package controllers

import auth.AuthContext
import models.ModelTransformAs
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.xml.Elem

trait ReadableEntity[T] {
  def fromXml(xml: Elem): Either[String, T]

  def fromJson(json: JsValue): Either[String, T]
}

abstract class ControllerUtils(val controller: ControllerComponents)
    extends AbstractController(controller) {

  def renderMethod(obj: ModelTransformAs, status: Status = Ok)(
      implicit req: Request[Any]): Result = {

    val missingAcceptedTypes: Boolean = !req.headers
      .get(HeaderNames.ACCEPT)
      .toSeq
      .exists(s => {
        MimeTypes.JSON == s || MimeTypes.XML == s
      })

    if (missingAcceptedTypes) {
      req.contentType match {
        case Some(MimeTypes.JSON) =>
          status(obj.asJson)
        case Some(MimeTypes.XML) =>
          status(obj.asXml)
        case _ =>
          status(obj.asJson)
      }
    } else {

      render {
        case Accepts.Json() =>
          status(obj.asJson)
        case Accepts.Xml() =>
          status(obj.asXml)
      }
    }
  }

  def parseMethod[A](readable: ReadableEntity[A])(
      implicit req: AuthContext[AnyContent]): Either[String, A] = {

    req.contentType match {
      case Some(MimeTypes.JSON) =>
        req.body.asJson match {
          case Some(value) =>
            readable.fromJson(value)
          case _ => Left("error.invalid.json.format")
        }
      case Some(MimeTypes.XML) =>
        req.body.asXml match {
          case Some(value) =>
            readable.fromXml(value.head.asInstanceOf[Elem])
          case _ => Left("error.invalid.xml.format")
        }
      case _ =>
        Left("error.missing.content.type")
    }
  }

}
