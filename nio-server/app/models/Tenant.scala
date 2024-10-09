package models

import cats.data.Validated._
import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.json._
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}
import scala.collection.Seq

case class Tenant(key: String, description: String) extends ModelTransformAs {
  def asJson(): JsObject = Tenant.tenantFormats.writes(this)
  def asXml(): Elem = <tenant>
      <key>{key}</key>
      <description>{description}</description>
    </tenant>.clean()
}

object Tenant extends ReadableEntity[Tenant] {
  implicit val tenantFormats: OFormat[Tenant] = Json.format[Tenant]

  implicit val readXml: XMLRead[Tenant] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "description").validate[String](Some(s"${path.convert()}description"))
      ).mapN(Tenant.apply)

  def fromXml(xml: Elem): Either[AppErrors, Tenant] =
    readXml.read(xml, Some("tenant")).toEither

  def fromJson(json: JsValue): Either[AppErrors, Tenant] =
    json.validate[Tenant] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}

case class Tenants(tenants: Seq[Tenant]) extends ModelTransformAs {
  override def asXml(): Elem = <tenants>{tenants.map(_.asXml())}</tenants>.clean()

  override def asJson(): JsValue = JsArray(tenants.map(_.asJson()))
}

object Tenants
