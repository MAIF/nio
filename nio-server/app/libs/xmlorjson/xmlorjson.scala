package libs.xmlorjson

import controllers.ReadableEntity
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers}
import play.mvc.Http.MimeTypes
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.ExecutionContext
import scala.xml.{Elem, NodeSeq}

sealed trait XmlOrJson {
  def read[T](
      implicit readableEntity: ReadableEntity[T],
      ec: ExecutionContext
  ): Either[AppErrors, T] = {
    XmlOrJson.read(this)
  }
}

case class JsonBody(json: JsValue) extends XmlOrJson

case class XmlBody(json: NodeSeq) extends XmlOrJson

case class Other(anyContent: AnyContent) extends XmlOrJson

object XmlOrJson {

  import cats.implicits._

  def read[T](xmlOrJson: XmlOrJson)(
      implicit readableEntity: ReadableEntity[T],
      ec: ExecutionContext
  ): Either[AppErrors, T] = {
    xmlOrJson match {
      case JsonBody(json) =>
        Logger.info(s"Content type Json : $json")
        readableEntity.fromJson(json)
      case XmlBody(xml) =>
        Logger.info(s"Content type Xml : ${xml.head.asInstanceOf[Elem]}")
        readableEntity.fromXml(xml.head.asInstanceOf[Elem])
      case Other(other) =>
        Logger.info(s"Content type Other : $other")
        AppErrors(Seq(ErrorMessage("wrong.content.type")))
          .asLeft[T]
    }
  }

  def xmlorjson(parse: PlayBodyParsers)(
      implicit ec: ExecutionContext
  ): BodyParser[XmlOrJson] =
    parse.using { request =>
      request.contentType match {
        case Some(MimeTypes.JSON) =>
          parse.json.map { json =>
            JsonBody(json)
          }
        case Some(MimeTypes.XML) =>
          Logger.info(s"Content type Xml")
          parse.xml.map { xml =>
            XmlBody(xml)
          }
        case other =>
          Logger.info(s"Content type Unknow $other")
          parse.anyContent.map { c =>
            Other(c)
          }
      }
    }

  def bodyparser[T](parse: PlayBodyParsers)(
      implicit readableEntity: ReadableEntity[T],
      ec: ExecutionContext
  ): BodyParser[Either[AppErrors, T]] =
    parse.using { request =>
      request.contentType match {
        case Some(MimeTypes.JSON) =>
          parse.json.map { json =>
            readableEntity.fromJson(json)
          }
        case Some(MimeTypes.XML) =>
          parse.xml.map { xml =>
            readableEntity.fromXml(xml.head.asInstanceOf[Elem])
          }
        case other =>
          parse.anyContent.map { _ =>
            AppErrors(
              Seq(ErrorMessage("wrong.content.type", other.getOrElse(""))))
              .asLeft[T]
          }
      }
    }

}
