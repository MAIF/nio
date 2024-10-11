package models

import cats.data.Validated
import cats.data.Validated.*
import cats.implicits.*
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits.*
import libs.xml.syntax.*
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import utils.Result.AppErrors

import java.time.{Clock, LocalDateTime, ZoneId}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeBuffer, NodeSeq}

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

  implicit val formats: Format[PermissionType] = Format[PermissionType](
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

case class Permission(key: String, label: String, `type`: PermissionType = OptIn, validityPeriod: Option[FiniteDuration] = None) {

  def getValidityPeriod: Option[LocalDateTime] = validityPeriod.map(fd => LocalDateTime.now(Clock.systemUTC()).plusMinutes(fd.toMinutes).withNano(0))

  def checkDefault(): Boolean = `type` match {
    case OptIn => false
    case OptOut => true 
  }

  def asXml(): Elem = <permission>
      <key>{key}</key>
      <label>{label}</label>
      <type>{`type`.print}</type>
      {validityPeriod.map(p => <validityPeriod>p.toString()</validityPeriod>).getOrElse(new NodeBuffer())}
    </permission>.clean()
}

object Permission {

  implicit val formatFiniteDuration: Format[FiniteDuration] =
      Format[FiniteDuration](
        Reads[FiniteDuration] {
          case JsString(str) => Try (Duration(str)) match {
            case Success(value: FiniteDuration) => JsSuccess(value)
            case Success(_) => JsError("invalid duration")
            case Failure(_) => JsError("invalid duration")
          }
          case _ => JsError("invalid duration")
        },
        Writes[FiniteDuration] { d =>
          JsString(d.toString())
        }
      )

  implicit val readXmlFiniteDuration: XMLRead[FiniteDuration] = (xml: NodeSeq, path: Option[String]) =>
    Try(xml.head.text)
      .map { (str: String) =>
        Try (Duration(str)) match {
          case Success(value: FiniteDuration) => value.valid
          case Success(_) => AppErrors.error("invalid duration").invalid
          case Failure(_) => AppErrors.error("invalid duration").invalid
        }
      }
      .getOrElse(buildError(path).invalid)

  implicit val formats: Format[Permission] = Format[Permission](
    (
      (__ \ "key").read[String] and
        (__ \ "label").read[String] and
        (__ \ "type").read[PermissionType].orElse(Reads.pure(OptIn)) and
        (__ \ "validityPeriod").readNullable[FiniteDuration]
      )(Permission.apply),
    Json.writes[Permission]
  )

  implicit val readXml: XMLRead[Permission] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validate[String](Some(s"${path.convert()}label")),
        (node \ "type").validateNullable[PermissionType](OptIn, Some(s"${path.convert()}type")),
        (node \ "validityPeriod").validateNullable[FiniteDuration]
      ).mapN(Permission.apply)

}
