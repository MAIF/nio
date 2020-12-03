package models

import org.joda.time.{DateTime, DateTimeZone}
import utils.NioLogger
import play.api.libs.json._
import utils.DateUtils
import scala.collection.Seq

object EventType extends Enumeration {
  type WeekDay = Value
  val UserExtractTaskAsked, UserExtractTaskCompleted, Unknown = Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}

object NioEvent {

  def fromJson(json: JsValue): Option[NioEvent] =
    for {
      tYpe    <- (json \ "type").asOpt[String]
      tenant  <- (json \ "tenant").asOpt[String]
      author  <- (json \ "author").asOpt[String]
      metadata = (json \ "metadata").asOpt[JsObject].map { o =>
                   o.fields.map(f => (f._1, f._2.as[String]))
                 }
      date    <- (json \ "date").validate(DateUtils.utcDateTimeReads).asOpt
      id      <- (json \ "id").asOpt[Long]
      payload <- (json \ "payload").toOption
      event   <- EventType.from(tYpe) match {
                   case EventType.UserExtractTaskAsked     =>
                     UserExtractTask
                       .fromJson(payload)
                       .toOption
                       .map(o => UserExtractTaskAsked(tenant, author, metadata, id, date, o))
                   case EventType.UserExtractTaskCompleted =>
                     UserExtractTask
                       .fromJson(payload)
                       .toOption
                       .map(o => UserExtractTaskCompleted(tenant, author, metadata, id, date, o))
                   case EventType.Unknown                  =>
                     NioLogger.warn(s"Unknown event type $tYpe when deserializing ${Json.stringify(json)}")
                     None
                 }
    } yield event
}

trait NioEvent {
  val id: Long
  val date: DateTime
  val tenant: String

  def tYpe: EventType.Value

  def shardId: String

  def asJson(): JsValue

  def buildMetadata(metadata: Option[Seq[(String, String)]]): JsObject = {

    val maybeObjects: Option[Seq[(String, JsValue)]] = metadata
      .map(values => values.map(v => (v._1, JsString(v._2))))
    val jsObject: JsObject                           = JsObject(maybeObjects.getOrElse(Seq.empty))
    jsObject
  }
}

case class UserExtractTaskAsked(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long,
    date: DateTime = DateTime.now(DateTimeZone.UTC),
    payload: UserExtractTask
) extends NioEvent {
  override def tYpe: EventType.Value = EventType.UserExtractTaskAsked

  override def asJson(): JsValue =
    Json
      .obj(
        "type"     -> tYpe,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.toString(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )

  override def shardId: String = payload.userId
}
case class UserExtractTaskCompleted(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long,
    date: DateTime = DateTime.now(DateTimeZone.UTC),
    payload: UserExtractTask
) extends NioEvent {
  override def tYpe: EventType.Value = EventType.UserExtractTaskCompleted

  override def asJson(): JsValue =
    Json
      .obj(
        "type"     -> tYpe,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.toString(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )

  override def shardId: String = payload.userId
}
