package models

import controllers.ReadableEntity
import play.api.libs.json._

import scala.util.{Failure, Success, Try}
import scala.xml.Elem
import XmlUtil.XmlCleaner
import utils.Result.AppErrors

case class Tenant(key: String, description: String) extends ModelTransformAs {
  def asJson = Tenant.tenantFormats.writes(this)
  def asXml = {
    <tenant>
      <key>{key}</key>
      <description>{description}</description>
    </tenant>.clean()
  }
}

object Tenant extends ReadableEntity[Tenant] {
  implicit val tenantFormats = Json.format[Tenant]

  def fromXml(xml: Elem) = {
    Try {
      val key = (xml \ "key").head.text
      val description = (xml \ "description").head.text
      Tenant(key, description)
    } match {
      case Success(value)     => Right(value)
      case Failure(throwable) => Left(AppErrors.fromXmlError(throwable))
    }
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
