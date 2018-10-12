package models

import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}

case class Offer(key: String,
                 label: String,
                 version: Int = 1,
                 groups: Seq[PermissionGroup])
    extends ModelTransformAs {
  override def asXml(): Elem = <offer>
    <version>
      {version}
    </version>
    <key>
      {key}
    </key>
    <label>
      {label}
    </label>
    <groups>
      {groups.map(_.asXml)}
    </groups>
  </offer>.clean()

  override def asJson(): JsValue = Offer.offerWrites.writes(this)
}

object Offer extends ReadableEntity[Offer] {
  implicit val offerReads: Reads[Offer] = (
    (__ \ "key").read[String] and
      (__ \ "label").read[String] and
      (__ \ "version").readNullable[Int].map { version =>
        version.getOrElse(1)
      } and
      (__ \ "groups").read[Seq[PermissionGroup]]
  )(Offer.apply _)

  implicit val offerWrites: Writes[Offer] = (
    (__ \ "key").write[String] and
      (__ \ "label").write[String] and
      (__ \ "version").write[Int] and
      (__ \ "groups").write[Seq[PermissionGroup]]
  )(unlift(Offer.unapply))

  implicit val offerOWrites: OWrites[Offer] = (
    (__ \ "key").write[String] and
      (__ \ "label").write[String] and
      (__ \ "version").write[Int] and
      (__ \ "groups").write[Seq[PermissionGroup]]
  )(unlift(Offer.unapply))

  implicit val format: Format[Offer] = Format(offerReads, offerWrites)
  implicit val oformat: OFormat[Offer] = OFormat(offerReads, offerOWrites)

  implicit val offerReadXml: XMLRead[Offer] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "key").validate[String](Some(s"${path.convert()}key")),
        (node \ "label").validate[String](Some(s"${path.convert()}label")),
        (node \ "version")
          .validateNullable[Int](1, Some(s"${path.convert()}version")),
        (node \ "groups").validate[Seq[PermissionGroup]](
          Some(s"${path.convert()}groups"))
      ).mapN(
        (key, label, version, groups) => Offer(key, label, version, groups)
    )

  override def fromXml(xml: Elem): Either[AppErrors, Offer] =
    offerReadXml.read(xml, Some("offer")).toEither

  override def fromJson(json: JsValue): Either[AppErrors, Offer] =
    json.validate[Offer] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors)     => Left(AppErrors.fromJsError(errors))
    }
}
