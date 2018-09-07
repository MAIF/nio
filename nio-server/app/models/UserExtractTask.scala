package models

import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import utils.Result.AppErrors
import utils.{DateUtils, Result}

import scala.xml.{Elem, NodeSeq}

case class UserExtractTask(_id: String,
                           tenant: String,
                           orgKey: String,
                           userId: String,
                           startedAt: DateTime,
                           endedAt: Option[DateTime])
    extends ModelTransformAs {
  override def asXml(): Elem = <userExtractTask>
    <tenant>
      {tenant}
    </tenant>
    <orgKey>
      {orgKey}
    </orgKey>
    <userId>
      {userId}
    </userId>
    <startedAt>
      {startedAt.toString(DateUtils.utcDateFormatter)}
    </startedAt>{endedAt.map(date => <endedAt>
      {date.toString(DateUtils.utcDateFormatter)}
    </endedAt>)}
  </userExtractTask>.clean()

  override def asJson(): JsValue =
    UserExtractTask.userExtractTaskWrites.writes(this)
}

object UserExtractTask extends ReadableEntity[UserExtractTask] {
  implicit val dateFormats = DateUtils.utcDateTimeFormats

  def instance(tenant: String, orgKey: String, userId: String) =
    UserExtractTask(
      _id = BSONObjectID.generate().stringify,
      tenant = tenant,
      orgKey = orgKey,
      userId = userId,
      startedAt = DateTime.now(DateTimeZone.UTC),
      endedAt = None
    )

  implicit val userExtractTaskReads: Reads[UserExtractTask] = (
    (__ \ "_id").readNullable[String].map { maybeId =>
      maybeId.getOrElse(BSONObjectID.generate().stringify)
    } and
      (__ \ "tenant").read[String] and
      (__ \ "orgKey").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "startedAt").readNullable[DateTime].map { maybeStartedAt =>
        maybeStartedAt.getOrElse(DateTime.now(DateTimeZone.UTC))
      } and
      (__ \ "endedAt").readNullable[DateTime]
  )(UserExtractTask.apply _)

  implicit val userExtractTaskWrites: Writes[UserExtractTask] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "tenant").write[String] and
      (JsPath \ "orgKey").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "startedAt").write[DateTime] and
      (JsPath \ "endedAt").writeNullable[DateTime]
  )(unlift(UserExtractTask.unapply))

  implicit val userExtractTaskOWrites: OWrites[UserExtractTask] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "tenant").write[String] and
      (JsPath \ "orgKey").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "startedAt").write[DateTime] and
      (JsPath \ "endedAt").writeNullable[DateTime]
  )(unlift(UserExtractTask.unapply))

  implicit val format = Format(userExtractTaskReads, userExtractTaskWrites)
  implicit val oformat = OFormat(userExtractTaskReads, userExtractTaskOWrites)

  implicit val userExtractTaskReadXml: XMLRead[UserExtractTask] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](
          BSONObjectID.generate().stringify,
          Some(s"${path.convert()}_id")),
        (node \ "tenant").validate[String](Some(s"${path.convert()}tenant")),
        (node \ "orgKey").validate[String](Some(s"${path.convert()}orgKey")),
        (node \ "userId").validate[String](Some(s"${path.convert()}userId")),
        (node \ "startedAt").validateNullable[DateTime](
          DateTime.now(DateTimeZone.UTC),
          Some(s"${path.convert()}startedAt")),
        (node \ "endedAt").validateNullable[DateTime](
          Some(s"${path.convert()}endedAt"))
      ).mapN(
        (_id, tenant, orgKey, userId, startedAt, endedAt) =>
          UserExtractTask(
            _id = _id,
            tenant = tenant,
            orgKey = orgKey,
            userId = userId,
            startedAt = startedAt,
            endedAt = endedAt
        ))

  override def fromXml(xml: Elem): Either[Result.AppErrors, UserExtractTask] =
    userExtractTaskReadXml.read(xml, Some("userExtractTask")).toEither

  override def fromJson(
      json: JsValue): Either[Result.AppErrors, UserExtractTask] =
    json.validate[UserExtractTask] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}
