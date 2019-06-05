package models

import org.joda.time.DateTime
import play.api.libs.json._

import scala.util.Try

case class CatchupLock(tenant: String, expireAt: DateTime = DateTime.now)

object CatchupLock {
  implicit val catchupLockFormats: Format[CatchupLock] =
    new Format[CatchupLock] {
      override def reads(json: JsValue): JsResult[CatchupLock] =
        (Try {
          JsSuccess(
            CatchupLock(
              tenant = (json \ "tenant").as[String],
              expireAt = (json \ "expireAt").as(jodaDateFormat)
            ))
        } recover {
          case e => JsError(e.getMessage)
        }).get

      override def writes(c: CatchupLock): JsValue = Json.obj(
        "tenant" -> c.tenant,
        "expireAt" -> jodaDateFormat.writes(c.expireAt)
      )
    }

  implicit val jodaDateFormat = new Format[org.joda.time.DateTime] {
    override def reads(d: JsValue): JsResult[DateTime] = {
      JsSuccess(
        new DateTime(d.as[JsObject].\("$date").as[JsNumber].value.toLong))
    }

    override def writes(d: DateTime): JsValue = {
      JsObject(Seq("$date" -> JsNumber(d.getMillis)))
    }
  }
}
