package models

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._
import utils.Result.AppErrors
import utils.{DateUtils, Result}

case class UserExtractTask(_id: String,
                           tenant: String,
                           orgKey: String,
                           userId: String,
                           email: String,
                           startedAt: DateTime,
                           uploadStartedAt: Option[DateTime],
                           endedAt: Option[DateTime]) {
  def asJson(): JsValue =
    UserExtractTask.userExtractTaskWrites.writes(this)
}

object UserExtractTask {
  implicit val dateFormats: Format[DateTime] = DateUtils.utcDateTimeFormats

  implicit val userExtractTaskReads: Reads[UserExtractTask] = (
    (__ \ "_id").readNullable[String].map { maybeId =>
      maybeId.getOrElse("")
    } and
      (__ \ "tenant").read[String] and
      (__ \ "orgKey").read[String] and
      (__ \ "userId").read[String] and
      (__ \ "email").read[String] and
      (__ \ "startedAt").readNullable[DateTime].map { maybeStartedAt =>
        maybeStartedAt.getOrElse(DateTime.now(DateTimeZone.UTC))
      } and
      (__ \ "uploadStartedAt").readNullable[DateTime] and
      (__ \ "endedAt").readNullable[DateTime]
  )(UserExtractTask.apply _)

  implicit val userExtractTaskWrites: Writes[UserExtractTask] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "tenant").write[String] and
      (JsPath \ "orgKey").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "startedAt").write[DateTime] and
      (JsPath \ "uploadStartedAt").writeNullable[DateTime] and
      (JsPath \ "endedAt").writeNullable[DateTime]
  )(unlift(UserExtractTask.unapply))

  implicit val userExtractTaskOWrites: OWrites[UserExtractTask] = (
    (JsPath \ "_id").write[String] and
      (JsPath \ "tenant").write[String] and
      (JsPath \ "orgKey").write[String] and
      (JsPath \ "userId").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "startedAt").write[DateTime] and
      (JsPath \ "uploadStartedAt").writeNullable[DateTime] and
      (JsPath \ "endedAt").writeNullable[DateTime]
  )(unlift(UserExtractTask.unapply))

  implicit val format: Format[UserExtractTask] =
    Format(userExtractTaskReads, userExtractTaskWrites)
  implicit val oformat: OFormat[UserExtractTask] =
    OFormat(userExtractTaskReads, userExtractTaskOWrites)

  def fromJson(json: JsValue): Either[Result.AppErrors, UserExtractTask] =
    json.validate[UserExtractTask] match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(AppErrors.fromJsError(errors))
    }
}
