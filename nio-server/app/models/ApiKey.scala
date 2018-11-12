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
import reactivemongo.bson.BSONObjectID
import utils.Result.AppErrors

import scala.xml.{Elem, NodeSeq}

case class ApiKeyUpdate(isAdmin: Boolean,
                        offerRestrictionPatterns: Option[Seq[String]] = None)

object ApiKeyUpdate extends ReadableEntity[ApiKeyUpdate] {
  implicit val read: Reads[ApiKeyUpdate] = (
    (__ \ "isAdmin").read[Boolean] and
      (__ \ "offerRestrictionPatterns").readNullable[Seq[String]]
  )(ApiKeyUpdate.apply _)

  implicit val readXml: XMLRead[ApiKeyUpdate] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "isAdmin").validate[Boolean](Some(s"${path.convert()}isAdmin")),
        (node \ "offerRestrictionPatterns").validateNullable[Seq[String]](
          Some(s"${path.convert()}offerRestrictionPatterns"))
      ).mapN(
        (isAdmin, offerRestrictionPatterns) =>
          ApiKeyUpdate(
            isAdmin = isAdmin,
            offerRestrictionPatterns = offerRestrictionPatterns
        )
    )

  def fromXml(xml: Elem): Either[AppErrors, ApiKeyUpdate] = {
    readXml.read(xml, Some("ApiKey")).toEither
  }

  def fromJson(json: JsValue) = {
    json.validate[ApiKeyUpdate] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
  }
}

case class ApiKey(_id: String = BSONObjectID.generate().stringify,
                  clientId: String,
                  clientSecret: String,
                  offerRestrictionPatterns: Option[Seq[String]] = None)
    extends ModelTransformAs {
  override def asXml(): Elem = <ApiKey>
    <clientId>
      {clientId}
    </clientId>
    <clientSecret>
      {clientSecret}
    </clientSecret>
    {offerRestrictionPatterns
      .map(o =>
        <offerRestrictionPatterns>
          {o.map(
          l => <offerRestrictionPattern>
            {l}
          </offerRestrictionPattern>
        )}
        </offerRestrictionPatterns>
      )}
  </ApiKey>.clean()

  override def asJson(): JsValue = ApiKey.writeClean.writes(this)

  def toAuthInfo(): AuthInfo = {
    AuthInfo(clientId, false, None, offerRestrictionPatterns)
  }
}

object ApiKey extends ReadableEntity[ApiKey] {

  implicit val read: Reads[ApiKey] = (
    (__ \ "_id").readNullable[String].map { mayBeId =>
      mayBeId.getOrElse(BSONObjectID.generate().stringify)
    } and
      (__ \ "clientId").read[String] and
      (__ \ "clientSecret").read[String] and
      (__ \ "offerRestrictionPatterns").readNullable[Seq[String]]
  )(ApiKey.apply _)

  implicit val writeClean: Writes[ApiKey] = Writes { userAccount =>
    Json.obj(
      "_id" -> userAccount._id,
      "clientId" -> userAccount.clientId,
      "clientSecret" -> userAccount.clientSecret,
      "offerRestrictionPatterns" -> userAccount.offerRestrictionPatterns
    )
  }

  implicit val write: Writes[ApiKey] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "clientId").write[String] and
      (JsPath \ "clientSecret").write[String] and
      (JsPath \ "offerRestrictionPatterns").writeNullable[Seq[String]]
  )(unlift(ApiKey.unapply))

  implicit val owrite: OWrites[ApiKey] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "clientId").write[String] and
      (JsPath \ "clientSecret").write[String] and
      (JsPath \ "offerRestrictionPatterns").writeNullable[Seq[String]]
  )(unlift(ApiKey.unapply))

  implicit val formats: Format[ApiKey] = Format(read, write)
  implicit val oformats: OFormat[ApiKey] = OFormat(read, owrite)

  implicit val readXml: XMLRead[ApiKey] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](
          BSONObjectID.generate().stringify,
          Some(s"${path.convert()}_id")),
        (node \ "clientId").validate[String](
          Some(s"${path.convert()}clientId")),
        (node \ "clientSecret").validate[String](
          Some(s"${path.convert()}clientSecret")),
        (node \ "offerRestrictionPatterns").validateNullable[Seq[String]](
          Some(s"${path.convert()}offerRestrictionPatterns"))
      ).mapN(
        (_id, clientId, clientSecret, offerRestrictionPatterns) =>
          ApiKey(
            _id = _id,
            clientId = clientId,
            clientSecret = clientSecret,
            offerRestrictionPatterns = offerRestrictionPatterns
        )
    )

  def fromXml(xml: Elem): Either[AppErrors, ApiKey] = {
    readXml.read(xml, Some("ApiKey")).toEither
  }

  def fromJson(json: JsValue) = {
    json.validate[ApiKey] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
  }

}

case class ApiKeys(page: Int, pageSize: Int, count: Int, items: Seq[ApiKey])
    extends ModelTransformAs {
  def asJson =
    Json.obj("page" -> page,
             "pageSize" -> pageSize,
             "count" -> count,
             "items" -> JsArray(items.map(_.asJson)))

  def asXml = <apiKeys>
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
      {items.map(_.asXml)}
    </items>
  </apiKeys>
}

import cats.implicits._
import utils.Result.{AppErrors, ErrorMessage, Result}

sealed trait ApiKeyValidator {
  private def validateClientKey(
      clientKey: String,
      errorKey: String): ValidatorUtils.ValidationResult[String] = {
    clientKey match {
      case k if k.matches(ValidatorUtils.keyPattern) => clientKey.validNel
      case _                                         => errorKey.invalidNel
    }
  }

  def validateApiKey(apiKey: ApiKey): Result[ApiKey] = {
    (
      validateClientKey(apiKey.clientId, "account.clientId"),
      validateClientKey(apiKey.clientSecret, "account.clientSecret")
    ).mapN((_, _) => apiKey)
      .toEither
      .leftMap(s =>
        AppErrors(s.toList.map(errorMessage => ErrorMessage(errorMessage))))
  }
}

object ApiKeyValidator extends ApiKeyValidator
