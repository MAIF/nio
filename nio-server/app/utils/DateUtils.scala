package utils

import play.api.libs.json._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.util.{Failure, Success, Try}

object DateUtils {
  val utcDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  val utcDateTimeReads = new Reads[DateTime] {
    def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(DateTime.parse(s, utcDateFormatter)) match {
          case Success(d) => JsSuccess(d.withMillisOfSecond(0))
          case Failure(f) => JsSuccess(null)
        }
      case _ => JsError("error.expected.date")
    }
  }

  val utcDateTimeWrites = new Writes[DateTime] {
    override def writes(o: DateTime): JsValue =
      JsString(o.toString(DateUtils.utcDateFormatter))
  }

  val utcDateTimeFormats: Format[DateTime] =
    Format(utcDateTimeReads, utcDateTimeWrites)
}
