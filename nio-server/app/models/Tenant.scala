package models

import controllers.ReadableEntity
import play.api.libs.json._

import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class Tenant(key: String, description: String) extends ModelTransformAs {
  def asJson = Tenant.tenantFormats.writes(this)
  def asXml = {
    <tenant>
      <key>{key}</key>
      <description>{description}</description>
    </tenant>
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
      case Failure(throwable) => Left(throwable.getMessage)
    }
  }

  def fromJson(json: JsValue) = {
    json.validate[Tenant] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(errors.mkString(", "))
    }
  }
}

case class Tenants(tenants: Seq[Tenant]) extends ModelTransformAs {
  override def asXml(): Elem = (<tenants>{tenants.map(_.asXml)}</tenants>)

  override def asJson(): JsValue = JsArray(tenants.map(_.asJson))
}

object Tenants
