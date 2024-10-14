package models

import cats.implicits._
import controllers.ReadableEntity
import libs.xml.XMLRead
import libs.xml.XmlUtil.XmlCleaner
import libs.xml.implicits._
import libs.xml.syntax._
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._
import reactivemongo.api.bson.BSONObjectID
import utils.Result.AppErrors
import utils.{DateUtils, Result}

import java.time.{Clock, LocalDateTime}
import scala.collection.Seq
import scala.xml.{Elem, NodeSeq}

case class UserExtractTask(
                            _id: String,
                            tenant: String,
                            orgKey: String,
                            userId: String,
                            email: String,
                            startedAt: LocalDateTime,
                            uploadStartedAt: Option[LocalDateTime],
                            endedAt: Option[LocalDateTime]
) extends ModelTransformAs {
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
    <email>
      {email}
    </email>
    <startedAt>
      {startedAt.format(DateUtils.utcDateFormatter)}
    </startedAt>
    {
    uploadStartedAt.map(date => <uploadStartedAt>
      {date.format(DateUtils.utcDateFormatter)}
    </uploadStartedAt>)
  }
    {
    endedAt.map(date => <endedAt>
      {date.format(DateUtils.utcDateFormatter)}
    </endedAt>)
  }
  </userExtractTask>.clean()

  override def asJson(): JsValue =
    UserExtractTask.userExtractTaskWrites.writes(this)
}

object UserExtractTask extends ReadableEntity[UserExtractTask] {
  implicit val dateFormats: Format[LocalDateTime] = DateUtils.utcDateTimeFormats

  def instance(tenant: String, orgKey: String, userId: String, email: String): UserExtractTask =
    UserExtractTask(
      _id = BSONObjectID.generate().stringify,
      tenant = tenant,
      orgKey = orgKey,
      userId = userId,
      email = email,
      startedAt = LocalDateTime.now(Clock.systemUTC),
      uploadStartedAt = None,
      endedAt = None
    )

  implicit val userExtractTaskReads: Reads[UserExtractTask] = (
    (__ \ "_id").readNullable[String].map { maybeId =>
      maybeId.getOrElse(BSONObjectID.generate().stringify)
    } and
      (__ \ "tenant").read[String] and
      (__ \ "orgKey").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "email").read[String] and
      (__ \ "startedAt").readNullable[LocalDateTime].map { maybeStartedAt =>
        maybeStartedAt.getOrElse(LocalDateTime.now(Clock.systemUTC))
      } and
      (__ \ "uploadStartedAt").readNullable[LocalDateTime] and
      (__ \ "endedAt").readNullable[LocalDateTime]
  )(UserExtractTask.apply _)

  implicit val userExtractTaskWrites: Writes[UserExtractTask] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "tenant").write[String] and
      (JsPath \ "orgKey").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "startedAt").write[LocalDateTime] and
      (JsPath \ "uploadStartedAt").writeNullable[LocalDateTime] and
      (JsPath \ "endedAt").writeNullable[LocalDateTime]
  )(unlift(UserExtractTask.unapply))

  implicit val userExtractTaskOWrites: OWrites[UserExtractTask] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "tenant").write[String] and
      (JsPath \ "orgKey").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "startedAt").write[LocalDateTime] and
      (JsPath \ "uploadStartedAt").writeNullable[LocalDateTime] and
      (JsPath \ "endedAt").writeNullable[LocalDateTime]
  )(unlift(UserExtractTask.unapply))

  implicit val format: Format[UserExtractTask] = Format(userExtractTaskReads, userExtractTaskWrites)
  implicit val oformat: OFormat[UserExtractTask] = OFormat(userExtractTaskReads, userExtractTaskOWrites)

  implicit val userExtractTaskReadXml: XMLRead[UserExtractTask] =
    (node: NodeSeq, path: Option[String]) =>
      (
        (node \ "_id").validateNullable[String](BSONObjectID.generate().stringify, Some(s"${path.convert()}_id")),
        (node \ "tenant").validate[String](Some(s"${path.convert()}tenant")),
        (node \ "orgKey").validate[String](Some(s"${path.convert()}orgKey")),
        (node \ "userId").validate[String](Some(s"${path.convert()}userId")),
        (node \ "email").validate[String](Some(s"${path.convert()}email")),
        (node \ "startedAt")
          .validateNullable[LocalDateTime](LocalDateTime.now(Clock.systemUTC), Some(s"${path.convert()}startedAt")),
        (node \ "uploadStartedAt").validateNullable[LocalDateTime](Some(s"${path.convert()}uploadStartedAt")),
        (node \ "endedAt").validateNullable[LocalDateTime](Some(s"${path.convert()}endedAt"))
      ).mapN((_id, tenant, orgKey, userId, email, startedAt, uploadStartedAt, endedAt) =>
        UserExtractTask(
          _id = _id,
          tenant = tenant,
          orgKey = orgKey,
          userId = userId,
          email = email,
          startedAt = startedAt,
          uploadStartedAt = uploadStartedAt,
          endedAt = endedAt
        )
      )

  override def fromXml(xml: Elem): Either[Result.AppErrors, UserExtractTask] =
    userExtractTaskReadXml.read(xml, Some("userExtractTask")).toEither

  override def fromJson(json: JsValue): Either[Result.AppErrors, UserExtractTask] =
    json.validate[UserExtractTask] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}

case class UserExtractTasks(page: Int, pageSize: Int, count: Long, items: Seq[UserExtractTask])
    extends ModelTransformAs {
  override def asXml(): Elem = <userExtractTasks>
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
  </userExtractTasks>.clean()

  override def asJson(): JsValue =
    Json.obj("page" -> page, "pageSize" -> pageSize, "count" -> count, "items" -> items.map(_.asJson()))
}
