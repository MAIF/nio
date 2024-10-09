package models

import java.time.{Clock, LocalDateTime}
import utils.NioLogger
import play.api.libs.json._
import utils.{DateUtils, IdGenerator}

import scala.collection.Seq

object EventType extends Enumeration {
  type WeekDay = Value
  val TenantCreated, TenantDeleted, OrganisationCreated, OrganisationUpdated, OrganisationReleased, OrganisationDeleted,
      ConsentFactCreated, ConsentFactUpdated, AccountCreated, AccountUpdated, AccountDeleted, SecuredEvent,
      DeletionStarted, DeletionAppDone, DeletionFinished, ExtractionStarted, ExtractionAppFilesMetadataReceived,
      ExtractionAppDone, ExtractionFinished, UserExtractTaskAsked, UserExtractTaskCompleted, Unknown =
    Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}

object NioEvent {
  val gen = IdGenerator(1024)

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
                   case EventType.TenantCreated            =>
                     Tenant
                       .fromJson(payload)
                       .toOption
                       .map(t => TenantCreated(tenant, author, metadata, date, id, t))
                   case EventType.TenantDeleted            =>
                     Tenant
                       .fromJson(payload)
                       .toOption
                       .map(t => TenantDeleted(tenant, author, metadata, date, id, t))
                   case EventType.OrganisationCreated      =>
                     Organisation
                       .fromJson(payload)
                       .toOption
                       .map(o => OrganisationCreated(tenant, author, metadata, date, id, o))
                   case EventType.OrganisationUpdated      =>
                     for {
                       oldValue <- (json \ "oldValue").asOpt[Organisation]
                       payload  <- Organisation.fromJson(payload).toOption
                     } yield OrganisationUpdated(tenant, author, metadata, date, id, payload, oldValue)
                   case EventType.OrganisationReleased     =>
                     Organisation
                       .fromJson(payload)
                       .toOption
                       .map(o => OrganisationReleased(tenant, author, metadata, date, id, o))
                   case EventType.OrganisationDeleted      =>
                     Organisation
                       .fromJson(payload)
                       .toOption
                       .map(o => OrganisationDeleted(tenant, author, metadata, date, id, o))
                   case EventType.ConsentFactCreated       =>
                     ConsentFact
                       .fromJson(payload)
                       .toOption
                       .map(o => ConsentFactCreated(tenant, author, metadata, date, id, o, (json \ "oldValue").asOpt[JsValue].getOrElse(Json.obj())))
                   case EventType.ConsentFactUpdated       =>
                     for {
                       oldValue <- (json \ "oldValue")
                                     .asOpt[JsValue]
                                     .flatMap(json => ConsentFact.fromJson(json).toOption)
                       payload  <- ConsentFact.fromJson(payload).toOption
                       command  <- Option((json \ "oldValue").asOpt[JsValue].getOrElse(Json.obj()))
                     } yield ConsentFactUpdated(tenant, author, metadata, date, id, payload, oldValue, command)
                   case EventType.AccountDeleted           =>
                     Account
                       .fromJson(payload)
                       .toOption
                       .map(o => AccountDeleted(tenant, author, metadata, id, date, o))
                   case EventType.AccountCreated           =>
                     Account
                       .fromJson(payload)
                       .toOption
                       .map(o => AccountCreated(tenant, author, metadata, id, date, o))
                   case EventType.AccountUpdated           =>
                     for {
                       oldValue <- (json \ "oldValue")
                                     .asOpt[JsValue]
                                     .flatMap(json => Account.fromJson(json).toOption)
                       payload  <- Account.fromJson(payload).toOption
                     } yield AccountUpdated(tenant, author, metadata, id, date, payload, oldValue)
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
                   case EventType.SecuredEvent             =>
                     Digest
                       .fromJson(payload)
                       .toOption
                       .map(digest => SecuredEvent(date = date, payload = digest))
                   case EventType.Unknown                  =>
                     NioLogger.warn(s"Unknown event type $tYpe when deserializing ${Json.stringify(json)}")
                     None
                 }
    } yield event
}

trait NioEvent {
  val id: Long
  val date: LocalDateTime
  val tenant: String

  def `type`: EventType.Value

  def asJson(): JsValue

  def shardId: String

  def buildMetadata(metadata: Option[Seq[(String, String)]]): JsObject = {

    val maybeObjects: Option[Seq[(String, JsValue)]] = metadata
      .map(values => values.map(v => (v._1, JsString(v._2))))
    val jsObject: JsObject                           = JsObject(maybeObjects.getOrElse(Seq.empty))
    jsObject
  }
}

object CleanUpMetadata {

  implicit class CleanMetadata(val jsObject: JsObject) extends AnyVal {
    def cleanMetadata(): JsObject =
      jsObject.fields.exists(v =>
        "metadata".equals(v._1) && (JsNull.equals(v._2) || JsObject.empty
          .equals(v._2))
      ) match {
        case v if v  => jsObject.-("metadata")
        case _ => jsObject
      }
  }

}

import CleanUpMetadata.CleanMetadata

case class TenantCreated(
                          tenant: String,
                          author: String = "tenant-admin",
                          metadata: Option[Seq[(String, String)]] = None,
                          date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
                          id: Long = NioEvent.gen.nextId(),
                          payload: Tenant
) extends NioEvent {
  def shardId: String = payload.key

  def `type`: EventType.Value = EventType.TenantCreated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> payload.key,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class TenantDeleted(
    tenant: String,
    author: String = "tenant-admin",
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: Tenant
) extends NioEvent {
  def shardId: String = payload.key

  def `type`: EventType.Value = EventType.TenantDeleted

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> payload.key,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class OrganisationCreated(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: Organisation
) extends NioEvent {
  def shardId: String = payload.key

  def `type`: EventType.Value = EventType.OrganisationCreated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class OrganisationUpdated(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: Organisation,
    oldValue: Organisation
) extends NioEvent {
  def shardId: String = oldValue.key

  def `type`: EventType.Value = EventType.OrganisationUpdated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson(),
        "oldValue" -> oldValue.asJson()
      )
      .cleanMetadata()
}

case class OrganisationReleased(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: Organisation
) extends NioEvent {
  def shardId: String  = payload.key

  def `type`: EventType.Value = EventType.OrganisationReleased

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class OrganisationDeleted(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: Organisation
) extends NioEvent {
  def shardId: String  = payload.key

  def `type`: EventType.Value = EventType.OrganisationDeleted

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class ConsentFactCreated(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: ConsentFact,
    command: JsValue
) extends NioEvent() {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.ConsentFactCreated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson(),
        "command"  -> command
      )
      .cleanMetadata()
}

case class ConsentFactUpdated(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    id: Long = NioEvent.gen.nextId(),
    payload: ConsentFact,
    oldValue: ConsentFact,
    command: JsValue
) extends NioEvent {
  def shardId: String  = oldValue.userId

  def `type`: EventType.Value = EventType.ConsentFactUpdated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson(),
        "oldValue" -> oldValue.asJson(),
        "command"  -> command
      )
      .cleanMetadata()
}

case class AccountDeleted(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: Account
) extends NioEvent {
  def shardId: String  = payload.accountId

  def `type`: EventType.Value = EventType.AccountDeleted

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "account"  -> payload.accountId,
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class AccountCreated(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: Account
) extends NioEvent {
  def shardId: String  = payload.accountId

  def `type`: EventType.Value = EventType.AccountCreated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "account"  -> payload.accountId,
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class AccountUpdated(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: Account,
    oldValue: Account
) extends NioEvent {
  def shardId: String  = payload.accountId

  def `type`: EventType.Value = EventType.AccountUpdated

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "account"  -> payload.accountId,
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson(),
        "oldValue" -> oldValue.asJson()
      )
      .cleanMetadata()
}

case class SecuredEvent(
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: Digest
) extends NioEvent {
  def `type`: EventType.Value = EventType.SecuredEvent

  def asJson(): JsObject =
    Json
      .obj(
        "type"    -> `type`,
        "tenant"  -> tenant,
        "date"    -> date.format(DateUtils.utcDateFormatter),
        "id"      -> id,
        "payload" -> payload.digest
      )
      .cleanMetadata()

  override val tenant: String = null

  override def shardId: String = "NA"
}

case class DeletionStarted(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: DeletionTaskInfoPerApp
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.DeletionStarted

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class DeletionAppDone(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: AppDone
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.DeletionAppDone

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class DeletionFinished(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: DeletionTask
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.DeletionFinished

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class UserExtractTaskAsked(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: UserExtractTask
) extends NioEvent {
  override def `type`: EventType.Value = EventType.UserExtractTaskAsked

  override def asJson(): JsValue =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()

  override def shardId: String = payload.userId
}
case class UserExtractTaskCompleted(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: UserExtractTask
) extends NioEvent {
  override def `type`: EventType.Value = EventType.UserExtractTaskCompleted

  override def asJson(): JsValue =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()

  override def shardId: String = payload.userId
}

case class ExtractionStarted(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: ExtractionTaskInfoPerApp
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.ExtractionStarted

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class ExtractionAppDone(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: AppDone
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.ExtractionAppDone

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class ExtractionAppFilesMetadataReceived(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: AppFilesMetadata
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.ExtractionAppFilesMetadataReceived

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}

case class ExtractionFinished(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: LocalDateTime = LocalDateTime.now(Clock.systemUTC),
    payload: ExtractionTask
) extends NioEvent {
  def shardId: String  = payload.userId

  def `type`: EventType.Value = EventType.ExtractionFinished

  def asJson(): JsObject =
    Json
      .obj(
        "type"     -> `type`,
        "tenant"   -> tenant,
        "author"   -> author,
        "metadata" -> buildMetadata(metadata),
        "date"     -> date.format(DateUtils.utcDateFormatter),
        "id"       -> id,
        "payload"  -> payload.asJson()
      )
      .cleanMetadata()
}
