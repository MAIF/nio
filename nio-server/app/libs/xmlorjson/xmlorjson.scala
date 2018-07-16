package libs.xmlorjson

import libs.xml.XMLRead
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers}
import play.mvc.Http.MimeTypes
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.ExecutionContext
import scala.xml.{Elem, NodeSeq}

sealed trait XmlOrJson {
  def read[T](
      implicit jsonReads: Reads[T],
      xmlReads: XMLRead[T],
      ec: ExecutionContext
  ): Either[AppErrors, T] = {
    XmlOrJson.read(this)
  }
}
case class JsonBody(json: JsValue) extends XmlOrJson
case class XmlBody(json: NodeSeq) extends XmlOrJson
case class Other(anyContent: AnyContent) extends XmlOrJson

object XmlOrJson {

  import cats._
  import cats.implicits._

  def read[T](xmlOrJson: XmlOrJson)(
      implicit jsonReads: Reads[T],
      xmlReads: XMLRead[T],
      ec: ExecutionContext
  ): Either[AppErrors, T] = {
    xmlOrJson match {
      case JsonBody(json) =>
        jsonReads.reads(json).asEither.leftMap(AppErrors.fromJsError)
      case XmlBody(xml) => xmlReads.read(xml).toEither
      case Other(other) =>
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
          parse.xml.map { xml =>
            XmlBody(xml)
          }
        case other =>
          parse.anyContent.map { c =>
            Other(c)
          }
      }
    }

  def bodyparser[T](parse: PlayBodyParsers)(
      implicit jsonReads: Reads[T],
      xmlReads: XMLRead[T],
      ec: ExecutionContext
  ): BodyParser[Either[AppErrors, T]] =
    parse.using { request =>
      request.contentType match {
        case Some(MimeTypes.JSON) =>
          parse.json.map { json =>
            jsonReads.reads(json).asEither.leftMap(AppErrors.fromJsError)
          }
        case Some(MimeTypes.XML) =>
          parse.xml.map { xml =>
            xmlReads.read(xml).toEither
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
