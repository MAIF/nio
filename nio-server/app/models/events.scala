package models

import models.OrganisationUser.{read, write}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json._
import utils.{DateUtils, IdGenerator}

object EventType extends Enumeration {
  type WeekDay = Value
  val TenantCreated, TenantDeleted, OrganisationCreated, OrganisationUpdated,
  OrganisationReleased, OrganisationDeleted, ConsentFactCreated,
  ConsentFactUpdated, AccountCreated, AccountUpdated, AccountDeleted,
  SecuredEvent, DeletionStarted, DeletionAppDone, DeletionFinished,
  ExtractionStarted, ExtractionAppFilesMetadataReceived, ExtractionAppDone,
  ExtractionFinished, Unknown = Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)
}

object NioEvent {
  val gen = IdGenerator(1024)

  def fromJson(json: JsValue): Option[NioEvent] = {
    for {
      tYpe <- (json \ "type").asOpt[String]
      tenant <- (json \ "tenant").asOpt[String]
      author <- (json \ "author").asOpt[String]
      metadata = (json \ "metadata").asOpt[JsObject].map { o =>
        o.fields.map(
          f => (f._1, f._2.as[String])
        )
      }
      date <- (json \ "date").validate(DateUtils.utcDateTimeReads).asOpt
      id <- (json \ "id").asOpt[Long]
      payload <- (json \ "payload").toOption
      event <- EventType.from(tYpe) match {
        case EventType.TenantCreated =>
          Tenant
            .fromJson(payload)
            .toOption
            .map(t => TenantCreated(tenant, author, metadata, date, id, t))
        case EventType.TenantDeleted =>
          Tenant
            .fromJson(payload)
            .toOption
            .map(t => TenantDeleted(tenant, author, metadata, date, id, t))
        case EventType.OrganisationCreated =>
          Organisation
            .fromJson(payload)
            .toOption
            .map(o =>
              OrganisationCreated(tenant, author, metadata, date, id, o))
        case EventType.OrganisationUpdated =>
          for {
            oldValue <- (json \ "oldValue").asOpt[Organisation]
            payload <- Organisation.fromJson(payload).toOption
          } yield {
            OrganisationUpdated(tenant,
                                author,
                                metadata,
                                date,
                                id,
                                payload,
                                oldValue)
          }
        case EventType.OrganisationReleased =>
          Organisation
            .fromJson(payload)
            .toOption
            .map(o =>
              OrganisationReleased(tenant, author, metadata, date, id, o))
        case EventType.OrganisationDeleted =>
          Organisation
            .fromJson(payload)
            .toOption
            .map(o =>
              OrganisationDeleted(tenant, author, metadata, date, id, o))
        case EventType.ConsentFactCreated =>
          ConsentFact
            .fromJson(payload)
            .toOption
            .map(o => ConsentFactCreated(tenant, author, metadata, date, id, o))
        case EventType.ConsentFactUpdated =>
          for {
            oldValue <- (json \ "oldValue")
              .asOpt[JsValue]
              .flatMap(json => ConsentFact.fromJson(json).toOption)
            payload <- ConsentFact.fromJson(payload).toOption
          } yield {
            ConsentFactUpdated(tenant,
                               author,
                               metadata,
                               date,
                               id,
                               payload,
                               oldValue)
          }
        case EventType.AccountDeleted =>
          Account
            .fromJson(payload)
            .toOption
            .map(o => AccountDeleted(tenant, author, metadata, id, date, o))
        case EventType.AccountCreated =>
          Account
            .fromJson(payload)
            .toOption
            .map(o => AccountCreated(tenant, author, metadata, id, date, o))
        case EventType.AccountUpdated =>
          for {
            oldValue <- (json \ "oldValue")
              .asOpt[JsValue]
              .flatMap(json => Account.fromJson(json).toOption)
            payload <- Account.fromJson(payload).toOption
          } yield {
            AccountUpdated(tenant,
                           author,
                           metadata,
                           id,
                           date,
                           payload,
                           oldValue)
          }
        case EventType.SecuredEvent =>
          Digest
            .fromJson(payload)
            .toOption
            .map(
              digest => SecuredEvent(date = date, payload = digest)
            )
        case EventType.Unknown =>
          Logger.warn(
            s"Unknown event type $tYpe when deserializing ${Json.stringify(json)}")
          None
      }
    } yield {
      event
    }
  }
}

trait NioEvent {
  val id: Long
  val date: DateTime
  val tenant: String

  def tYpe: EventType.Value

  def asJson: JsValue

  def shardId: String

  def buildMetadata(metadata: Option[Seq[(String, String)]]): JsObject = {

    val maybeObjects: Option[Seq[(String, JsValue)]] = metadata
      .map(values => values.map(v => (v._1, JsString(v._2))))
    val jsObject: JsObject = JsObject(maybeObjects.getOrElse(Seq.empty))
    jsObject
  }
}

object CleanUpMetadata {

  implicit class CleanMetadata(val jsObject: JsObject) extends AnyVal {
    def cleanMetadata(): JsObject =
      jsObject.fields.exists(
        v =>
          "metadata".equals(v._1) && (JsNull.equals(v._2) || JsObject.empty
            .equals(v._2))) match {
        case v if v =>
          jsObject.-("metadata")
        case v if !v =>
          jsObject
      }
  }
}

import CleanUpMetadata.CleanMetadata

case class TenantCreated(tenant: String,
                         author: String = "tenant-admin",
                         metadata: Option[Seq[(String, String)]] = None,
                         date: DateTime = DateTime.now(DateTimeZone.UTC),
                         id: Long = NioEvent.gen.nextId(),
                         payload: Tenant)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.TenantCreated

  def asJson = {
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> payload.key,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
  }
}

case class TenantDeleted(tenant: String,
                         author: String = "tenant-admin",
                         metadata: Option[Seq[(String, String)]] = None,
                         date: DateTime = DateTime.now(DateTimeZone.UTC),
                         id: Long = NioEvent.gen.nextId(),
                         payload: Tenant)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.TenantDeleted

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> payload.key,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class OrganisationCreated(tenant: String,
                               author: String,
                               metadata: Option[Seq[(String, String)]] = None,
                               date: DateTime = DateTime.now(DateTimeZone.UTC),
                               id: Long = NioEvent.gen.nextId(),
                               payload: Organisation)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.OrganisationCreated

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class OrganisationUpdated(tenant: String,
                               author: String,
                               metadata: Option[Seq[(String, String)]] = None,
                               date: DateTime = DateTime.now(DateTimeZone.UTC),
                               id: Long = NioEvent.gen.nextId(),
                               payload: Organisation,
                               oldValue: Organisation)
    extends NioEvent {
  def shardId = oldValue.key

  def tYpe = EventType.OrganisationUpdated

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson,
        "oldValue" -> oldValue.asJson
      )
      .cleanMetadata()
}

case class OrganisationReleased(tenant: String,
                                author: String,
                                metadata: Option[Seq[(String, String)]] = None,
                                date: DateTime = DateTime.now(DateTimeZone.UTC),
                                id: Long = NioEvent.gen.nextId(),
                                payload: Organisation)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.OrganisationReleased

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class OrganisationDeleted(tenant: String,
                               author: String,
                               metadata: Option[Seq[(String, String)]] = None,
                               date: DateTime = DateTime.now(DateTimeZone.UTC),
                               id: Long = NioEvent.gen.nextId(),
                               payload: Organisation)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.OrganisationDeleted

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class ConsentFactCreated(tenant: String,
                              author: String,
                              metadata: Option[Seq[(String, String)]] = None,
                              date: DateTime = DateTime.now(DateTimeZone.UTC),
                              id: Long = NioEvent.gen.nextId(),
                              payload: ConsentFact)
    extends NioEvent() {
  def shardId = payload.userId

  def tYpe = EventType.ConsentFactCreated

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class ConsentFactUpdated(tenant: String,
                              author: String,
                              metadata: Option[Seq[(String, String)]] = None,
                              date: DateTime = DateTime.now(DateTimeZone.UTC),
                              id: Long = NioEvent.gen.nextId(),
                              payload: ConsentFact,
                              oldValue: ConsentFact)
    extends NioEvent {
  def shardId = oldValue.userId

  def tYpe = EventType.ConsentFactUpdated

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson,
        "oldValue" -> oldValue.asJson
      )
      .cleanMetadata()
}

case class AccountDeleted(tenant: String,
                          author: String,
                          metadata: Option[Seq[(String, String)]] = None,
                          id: Long = NioEvent.gen.nextId(),
                          date: DateTime = DateTime.now(DateTimeZone.UTC),
                          payload: Account)
    extends NioEvent {
  def shardId = payload.accountId

  def tYpe = EventType.AccountDeleted

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "account" -> payload.accountId,
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class AccountCreated(tenant: String,
                          author: String,
                          metadata: Option[Seq[(String, String)]] = None,
                          id: Long = NioEvent.gen.nextId(),
                          date: DateTime = DateTime.now(DateTimeZone.UTC),
                          payload: Account)
    extends NioEvent {
  def shardId = payload.accountId

  def tYpe = EventType.AccountCreated

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "account" -> payload.accountId,
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class AccountUpdated(tenant: String,
                          author: String,
                          metadata: Option[Seq[(String, String)]] = None,
                          id: Long = NioEvent.gen.nextId(),
                          date: DateTime = DateTime.now(DateTimeZone.UTC),
                          payload: Account,
                          oldValue: Account)
    extends NioEvent {
  def shardId = payload.accountId

  def tYpe = EventType.AccountUpdated

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "account" -> payload.accountId,
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson,
        "oldValue" -> oldValue.asJson
      )
      .cleanMetadata()
}

case class SecuredEvent(id: Long = NioEvent.gen.nextId(),
                        date: DateTime = DateTime.now(DateTimeZone.UTC),
                        payload: Digest)
    extends NioEvent {
  def tYpe = EventType.SecuredEvent

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.digest
      )
      .cleanMetadata()

  override val tenant: String = null

  override def shardId: String = "NA"
}

case class DeletionStarted(tenant: String,
                           author: String,
                           metadata: Option[Seq[(String, String)]] = None,
                           id: Long = NioEvent.gen.nextId(),
                           date: DateTime = DateTime.now(DateTimeZone.UTC),
                           payload: DeletionTaskInfoPerApp)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.DeletionStarted

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class DeletionAppDone(tenant: String,
                           author: String,
                           metadata: Option[Seq[(String, String)]] = None,
                           id: Long = NioEvent.gen.nextId(),
                           date: DateTime = DateTime.now(DateTimeZone.UTC),
                           payload: AppDone)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.DeletionAppDone

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class DeletionFinished(tenant: String,
                            author: String,
                            metadata: Option[Seq[(String, String)]] = None,
                            id: Long = NioEvent.gen.nextId(),
                            date: DateTime = DateTime.now(DateTimeZone.UTC),
                            payload: DeletionTask)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.DeletionFinished

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class ExtractionStarted(tenant: String,
                             author: String,
                             metadata: Option[Seq[(String, String)]] = None,
                             id: Long = NioEvent.gen.nextId(),
                             date: DateTime = DateTime.now(DateTimeZone.UTC),
                             payload: ExtractionTaskInfoPerApp)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.ExtractionStarted

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class ExtractionAppDone(tenant: String,
                             author: String,
                             metadata: Option[Seq[(String, String)]] = None,
                             id: Long = NioEvent.gen.nextId(),
                             date: DateTime = DateTime.now(DateTimeZone.UTC),
                             payload: AppDone)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.ExtractionAppDone

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class ExtractionAppFilesMetadataReceived(
    tenant: String,
    author: String,
    metadata: Option[Seq[(String, String)]] = None,
    id: Long = NioEvent.gen.nextId(),
    date: DateTime = DateTime.now(DateTimeZone.UTC),
    payload: AppFilesMetadata)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.ExtractionAppFilesMetadataReceived

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}

case class ExtractionFinished(tenant: String,
                              author: String,
                              metadata: Option[Seq[(String, String)]] = None,
                              id: Long = NioEvent.gen.nextId(),
                              date: DateTime = DateTime.now(DateTimeZone.UTC),
                              payload: ExtractionTask)
    extends NioEvent {
  def shardId = payload.userId

  def tYpe = EventType.ExtractionFinished

  def asJson =
    Json
      .obj(
        "type" -> tYpe,
        "tenant" -> tenant,
        "author" -> author,
        "metadata" -> buildMetadata(metadata),
        "date" -> date.toString(DateUtils.utcDateFormatter),
        "id" -> id,
        "payload" -> payload.asJson
      )
      .cleanMetadata()
}
