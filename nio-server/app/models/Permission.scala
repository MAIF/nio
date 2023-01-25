package models

import cats.data.Validated
import cats.data.Validated._
import cats.implicits._
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs._
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json._
import play.api.libs.json.Reads._
import utils.Result.AppErrors

import scala.xml.NodeSeq

sealed trait PermissionType {
  def print : String = this match {
    case OptIn => "OptIn"
    case OptOut => "OptOut"
  }

}
case object OptOut extends PermissionType with Product
case object OptIn extends PermissionType with Product

object PermissionType {

  def parse(text: String): Validated[AppErrors, PermissionType] = text match {
    case "OptIn" => OptIn.valid[AppErrors]
    case "OptOut" => OptOut.valid[AppErrors]
    case other => Invalid(AppErrors.error(s"Unknown PermissionType $other"))
  }

  implicit val formats = Format[PermissionType](
    Reads[PermissionType] {
      case JsString("OptIn") => JsSuccess(OptIn)
      case JsString("OptOut") => JsSuccess(OptOut)
      case JsNull => JsSuccess(OptIn)
      case other => JsError(s"Unknown PermissionType $other")
    },
    Writes[PermissionType] {
      case OptIn => JsString("OptIn")
      case OptOut => JsString("OptOut")
    }
  )
  implicit val readXml: XMLRead[PermissionType] = (node: NodeSeq, _: Option[String]) =>
    node match {
      case scala.xml.Text(t) => PermissionType.parse(t)
      case scala.xml.Node(_, _, scala.xml.Text(t)) => PermissionType.parse(t)
      case other => Invalid(AppErrors.error(s"Unknown PermissionType $other"))
    }
}

case class Permission(key: String, label: String, `type`: PermissionType = OptIn) {
  def checkDefault(): Boolean = `type` match {
    case OptIn => false
    case OptOut => true 
  }

  def asXml() = <permission>
      <key>{key}</key>
      <label>{label}</label>
      <type>{`type`.print}</type>
    </permission>.clean()
}

object Permission {
  implicit val formats = Format[Permission](
    (
      (__ \ "key").read[String] and
        (__ \ "label").read[String] and
        (__ \ "type").read[PermissionType].orElse(Reads.pure(OptIn))
      )(Permission.apply _),
    Json.writes[Permission]
  )

  implicit val readXml: XMLRead[Permission] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validate[String](Some(s"${path.convert()}label")),
        (node \ "type").validateNullable[PermissionType](OptIn, Some(s"${path.convert()}type"))
      ).mapN(Permission.apply)

}
