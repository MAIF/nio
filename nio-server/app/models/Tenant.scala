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

case class Tenant(key: String, description: String) extends ModelTransformAs {
  def asJson = Tenant.tenantFormats.writes(this)
  def asXml = <tenant>
      <key>{key}</key>
      <description>{description}</description>
    </tenant>.clean()
}

object Tenant extends ReadableEntity[Tenant] {
  implicit val tenantFormats = Json.format[Tenant]

  implicit val readXml: XMLRead[Tenant] = (node: NodeSeq) =>
    (
      (node \ "key").validate[String],
      (node \ "description").validate[String]
    ).mapN(Tenant.apply)

  def fromXml(xml: Elem) = {
    readXml.read(xml).toEither
  }

  def fromJson(json: JsValue): Either[AppErrors, Tenant] = {
    json.validate[Tenant] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
  }
}

case class Tenants(tenants: Seq[Tenant]) extends ModelTransformAs {
  override def asXml(): Elem = <tenants>{tenants.map(_.asXml)}</tenants>.clean()

  override def asJson(): JsValue = JsArray(tenants.map(_.asJson))
}

object Tenants
