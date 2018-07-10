package controllers

import auth.AuthContext
import models.ModelTransformAs
import play.api.{Logger, mvc}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.JsValue
import play.api.mvc.Results.Status
import play.api.mvc._
import utils.Result.AppErrors

import scala.xml.Elem

trait ReadableEntity[T] {

  def fromXml(xml: Elem): Either[AppErrors, T]

  def fromJson(json: JsValue): Either[AppErrors, T]
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
          status(obj.asJson())
        case Some(MimeTypes.XML) =>
          status(obj.asXml())
        case _ =>
          status(obj.asJson())
      }
    } else {

      render {
        case Accepts.Json() =>
          status(obj.asJson())
        case Accepts.Xml() =>
          status(obj.asXml())
      }
    }
  }

  def parseMethod[A](readable: ReadableEntity[A])(
      implicit req: AuthContext[AnyContent]): Either[AppErrors, A] = {

    req.contentType match {
      case Some(MimeTypes.JSON) =>
        req.body.asJson match {
          case Some(value) =>
            Logger.info(s"JSON body : $value")
            readable.fromJson(value)
          case _ =>
            Logger.error(s"error.invalid.json.format ${req.body.asText}")
            Logger.error(s"error.invalid.json.format ${req.body.asRaw}")
            Left(AppErrors.error("error.invalid.json.format"))
        }
      case Some(MimeTypes.XML) =>
        req.body.asXml match {
          case Some(value) =>
            Logger.info(s"XML body : $value")
            readable.fromXml(value.head.asInstanceOf[Elem])
          case _ =>
            Logger.error(s"error.invalid.xml.format ${req.body.asText}")
            Logger.error(s"error.invalid.xml.format ${req.body.asRaw}")
            Left(AppErrors.error("error.invalid.xml.format"))
        }
      case _ =>
        Logger.error(s"error.missing.content.type ${req.body.asText}")
        Logger.error(s"error.missing.content.type ${req.body.asRaw}")
        Left(AppErrors.error("error.missing.content.type"))
    }
  }

  implicit def renderError = new ErrorManagerSuite {
    override def convert(appErrors: AppErrors, status: mvc.Results.Status)(
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
            status(appErrors.asJson)
          case Some(MimeTypes.XML) =>
            status(appErrors.asXml)
          case _ =>
            status(appErrors.asJson)
        }
      } else {

        render {
          case Accepts.Json() =>
            status(appErrors.asJson)
          case Accepts.Xml() =>
            status(appErrors.asXml)
        }
      }
    }
  }

}

trait ErrorManagerSuite {
  def convert(appErrors: AppErrors, status: Status)(
      implicit req: Request[Any]): Result
}
