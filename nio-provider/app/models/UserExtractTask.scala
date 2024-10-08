package models

import java.time.{LocalDateTime, Clock}
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._
import utils.Result.AppErrors
import utils.{DateUtils, Result}
import scala.collection.Seq

case class UserExtractTask(
    _id: String,
    tenant: String,
    orgKey: String,
    userId: String,
    email: String,
    startedAt: LocalDateTime,
    uploadStartedAt: Option[LocalDateTime],
    endedAt: Option[LocalDateTime]
) {
  def asJson(): JsValue =
    UserExtractTask.userExtractTaskWrites.writes(this)
}

object UserExtractTask {
  implicit val dateFormats: Format[LocalDateTime] = DateUtils.utcDateTimeFormats

  implicit val userExtractTaskReads: Reads[UserExtractTask] = (
    (__ \ "_id").readNullable[String].map { maybeId =>
      maybeId.getOrElse("")
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

  implicit val format: Format[UserExtractTask]   =
    Format(userExtractTaskReads, userExtractTaskWrites)
  implicit val oformat: OFormat[UserExtractTask] =
    OFormat(userExtractTaskReads, userExtractTaskOWrites)

  def fromJson(json: JsValue): Either[Result.AppErrors, UserExtractTask] =
    json.validate[UserExtractTask] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}
