package models

import auth.AuthInfo
import cats.data.Validated._
import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.{JsValue, _}
import reactivemongo.api.bson.BSONObjectID
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}
import scala.collection.Seq

case class NioAccountUpdate(isAdmin: Boolean, offerRestrictionPatterns: Option[Seq[String]] = None)

object NioAccountUpdate extends ReadableEntity[NioAccountUpdate] {
  implicit val read: Reads[NioAccountUpdate] = (
    (__ \ "isAdmin").read[Boolean] and
      (__ \ "offerRestrictionPatterns").readNullable[Seq[String]]
  )(NioAccountUpdate.apply _)

  implicit val readXml: XMLRead[NioAccountUpdate] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "isAdmin").validate[Boolean](Some(s"${path.convert()}isAdmin")),
        (node \ "offerRestrictionPatterns").validateNullable[Seq[String]](
          Some(s"${path.convert()}offerRestrictionPatterns")
        )
      ).mapN((isAdmin, offerRestrictionPatterns) =>
        NioAccountUpdate(
          isAdmin = isAdmin,
          offerRestrictionPatterns = offerRestrictionPatterns
        )
      )

  def fromXml(xml: Elem): Either[AppErrors, NioAccountUpdate] =
    readXml.read(xml, Some("NioAccount")).toEither

  def fromJson(json: JsValue) =
    json.validate[NioAccountUpdate] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}

case class NioAccount(
    _id: String = BSONObjectID.generate().stringify,
    email: String,
    password: String,
    isAdmin: Boolean,
    offerRestrictionPatterns: Option[Seq[String]] = None
) extends ModelTransformAs {
  override def asXml(): Elem = <NioAccount>
    <email>
      {email}
    </email>
    <isAdmin>
      {isAdmin}
    </isAdmin>{
    offerRestrictionPatterns
      .map(o =>
        <offerRestrictionPatterns>
          {
          o.map(l => <offerRestrictionPattern>
            {l}
          </offerRestrictionPattern>)
        }
        </offerRestrictionPatterns>
      )
  }
  </NioAccount>.clean()

  override def asJson(): JsValue = NioAccount.writeClean.writes(this)

  def toAuthInfo(): AuthInfo = {
    val maybeOfferRestrictionPatterns: Option[Seq[String]] = isAdmin match {
      case true  => Some(Seq("*"))
      case false => offerRestrictionPatterns
    }

    AuthInfo(email, isAdmin, None, maybeOfferRestrictionPatterns)
  }
}

object NioAccount extends ReadableEntity[NioAccount] {

  implicit val read: Reads[NioAccount] = (
    (__ \ "_id").readNullable[String].map { mayBeId =>
      mayBeId.getOrElse(BSONObjectID.generate().stringify)
    } and
      (__ \ "email").read[String] and
      (__ \ "password").read[String] and
      (__ \ "isAdmin").read[Boolean] and
      (__ \ "offerRestrictionPatterns").readNullable[Seq[String]]
  )(NioAccount.apply _)

  implicit val writeClean: Writes[NioAccount] = Writes { userAccount =>
    Json.obj(
      "_id"                      -> userAccount._id,
      "email"                    -> userAccount.email,
      "isAdmin"                  -> userAccount.isAdmin,
      "offerRestrictionPatterns" -> userAccount.offerRestrictionPatterns
    )
  }

  implicit val write: Writes[NioAccount] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "isAdmin").write[Boolean] and
      (JsPath \ "offerRestrictionPatterns").writeNullable[Seq[String]]
  )(unlift(NioAccount.unapply))

  implicit val owrite: OWrites[NioAccount] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "isAdmin").write[Boolean] and
      (JsPath \ "offerRestrictionPatterns").writeNullable[Seq[String]]
  )(unlift(NioAccount.unapply))

  implicit val formats: Format[NioAccount]   = Format(read, write)
  implicit val oformats: OFormat[NioAccount] = OFormat(read, owrite)

  implicit val readXml: XMLRead[NioAccount] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](BSONObjectID.generate().stringify, Some(s"${path.convert()}_id")),
        (node \ "email").validate[String](Some(s"${path.convert()}email")),
        (node \ "password").validate[String](Some(s"${path.convert()}password")),
        (node \ "isAdmin").validate[Boolean](Some(s"${path.convert()}isAdmin")),
        (node \ "offerRestrictionPatterns").validateNullable[Seq[String]](
          Some(s"${path.convert()}offerRestrictionPatterns")
        )
      ).mapN((_id, email, password, isAdmin, offerRestrictionPatterns) =>
        NioAccount(
          _id = _id,
          email = email,
          password = password,
          isAdmin = isAdmin,
          offerRestrictionPatterns = offerRestrictionPatterns
        )
      )

  def fromXml(xml: Elem): Either[AppErrors, NioAccount] =
    readXml.read(xml, Some("NioAccount")).toEither

  def fromJson(json: JsValue) =
    json.validate[NioAccount] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }

}

case class NioAccounts(page: Int, pageSize: Int, count: Long, items: Seq[NioAccount]) extends ModelTransformAs {
  def asJson(): JsObject =
    Json.obj("page" -> page, "pageSize" -> pageSize, "count" -> count, "items" -> JsArray(items.map(_.asJson())))

  def asXml(): Elem = <nioAccounts>
    <page>
      {page}
    </page>
    <pageSize>
      {pageSize}
    </pageSize>
    <count>
      {count}
    </count>
    <items>
      {items.map(_.asXml())}
    </items>
  </nioAccounts>
}
