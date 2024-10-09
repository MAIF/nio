package utils

import java.time.LocalDateTime
import play.api.libs.json._

import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

object DateUtils {
  val utcDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  val utcDateTimeReads = new Reads[LocalDateTime] {
    def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(LocalDateTime.parse(s, utcDateFormatter)) match {
          case Success(d) => JsSuccess(d.withNano(0))
          case Failure(_) => JsSuccess(null)
        }
      case _ => JsError("error.expected.date")
    }
  }

  val utcDateTimeWrites = new Writes[LocalDateTime] {
    override def writes(o: LocalDateTime): JsValue =
      JsString(o.format(DateUtils.utcDateFormatter))
  }

  val utcDateTimeFormats: Format[LocalDateTime] =
    Format(utcDateTimeReads, utcDateTimeWrites)
}
