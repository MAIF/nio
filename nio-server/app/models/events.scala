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
      date <- (json \ "date").validate(DateUtils.utcDateTimeReads).asOpt
      id <- (json \ "id").asOpt[Long]
      payload <- (json \ "payload").toOption
      event <- EventType.from(tYpe) match {
        case EventType.TenantCreated =>
          Tenant
            .fromJson(payload)
            .toOption
            .map(t => TenantCreated(tenant, author, date, id, t))
        case EventType.TenantDeleted =>
          Tenant
            .fromJson(payload)
            .toOption
            .map(t => TenantDeleted(tenant, author, date, id, t))
        case EventType.OrganisationCreated =>
          Organisation
            .fromJson(payload)
            .toOption
            .map(o => OrganisationCreated(tenant, author, date, id, o))
        case EventType.OrganisationUpdated =>
          for {
            oldValue <- (json \ "oldValue").asOpt[Organisation]
            payload <- Organisation.fromJson(payload).toOption
          } yield {
            OrganisationUpdated(tenant, author, date, id, payload, oldValue)
          }
        case EventType.OrganisationReleased =>
          Organisation
            .fromJson(payload)
            .toOption
            .map(o => OrganisationReleased(tenant, author, date, id, o))
        case EventType.OrganisationDeleted =>
          Organisation
            .fromJson(payload)
            .toOption
            .map(o => OrganisationDeleted(tenant, author, date, id, o))
        case EventType.ConsentFactCreated =>
          ConsentFact
            .fromJson(payload)
            .toOption
            .map(o => ConsentFactCreated(tenant, author, date, id, o))
        case EventType.ConsentFactUpdated =>
          for {
            oldValue <- (json \ "oldValue")
              .asOpt[JsValue]
              .flatMap(json => ConsentFact.fromJson(json).toOption)
            payload <- ConsentFact.fromJson(payload).toOption
          } yield {
            ConsentFactUpdated(tenant, author, date, id, payload, oldValue)
          }
        case EventType.AccountDeleted =>
          Account
            .fromJson(payload)
            .toOption
            .map(o => AccountDeleted(tenant, id, date, o))
        case EventType.AccountCreated =>
          Account
            .fromJson(payload)
            .toOption
            .map(o => AccountCreated(tenant, id, date, o))
        case EventType.AccountUpdated =>
          for {
            oldValue <- (json \ "oldValue")
              .asOpt[JsValue]
              .flatMap(json => Account.fromJson(json).toOption)
            payload <- Account.fromJson(payload).toOption
          } yield {
            AccountUpdated(tenant, id, date, payload, oldValue)
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
}

case class TenantCreated(tenant: String,
                         author: String = "tenant-admin",
                         date: DateTime = DateTime.now(DateTimeZone.UTC),
                         id: Long = NioEvent.gen.nextId(),
                         payload: Tenant)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.TenantCreated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> payload.key,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class TenantDeleted(tenant: String,
                         author: String = "tenant-admin",
                         date: DateTime = DateTime.now(DateTimeZone.UTC),
                         id: Long = NioEvent.gen.nextId(),
                         payload: Tenant)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.TenantDeleted

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> payload.key,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class OrganisationCreated(tenant: String,
                               author: String,
                               date: DateTime = DateTime.now(DateTimeZone.UTC),
                               id: Long = NioEvent.gen.nextId(),
                               payload: Organisation)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.OrganisationCreated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class OrganisationUpdated(tenant: String,
                               author: String,
                               date: DateTime = DateTime.now(DateTimeZone.UTC),
                               id: Long = NioEvent.gen.nextId(),
                               payload: Organisation,
                               oldValue: Organisation)
    extends NioEvent {
  def shardId = oldValue.key

  def tYpe = EventType.OrganisationUpdated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson,
    "oldValue" -> oldValue.asJson
  )
}

case class OrganisationReleased(tenant: String,
                                author: String,
                                date: DateTime = DateTime.now(DateTimeZone.UTC),
                                id: Long = NioEvent.gen.nextId(),
                                payload: Organisation)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.OrganisationReleased

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class OrganisationDeleted(tenant: String,
                               author: String,
                               date: DateTime = DateTime.now(DateTimeZone.UTC),
                               id: Long = NioEvent.gen.nextId(),
                               payload: Organisation)
    extends NioEvent {
  def shardId = payload.key

  def tYpe = EventType.OrganisationDeleted

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class ConsentFactCreated(tenant: String,
                              author: String,
                              date: DateTime = DateTime.now(DateTimeZone.UTC),
                              id: Long = NioEvent.gen.nextId(),
                              payload: ConsentFact)
    extends NioEvent() {
  def shardId = payload.userId

  def tYpe = EventType.ConsentFactCreated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class ConsentFactUpdated(tenant: String,
                              author: String,
                              date: DateTime = DateTime.now(DateTimeZone.UTC),
                              id: Long = NioEvent.gen.nextId(),
                              payload: ConsentFact,
                              oldValue: ConsentFact)
    extends NioEvent {
  def shardId = oldValue.userId

  def tYpe = EventType.ConsentFactUpdated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson,
    "oldValue" -> oldValue.asJson
  )
}

case class AccountDeleted(tenant: String,
                          id: Long = NioEvent.gen.nextId(),
                          date: DateTime = DateTime.now(DateTimeZone.UTC),
                          payload: Account)
    extends NioEvent {
  def shardId = payload.accountId

  def tYpe = EventType.AccountDeleted

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "account" -> payload.accountId,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class AccountCreated(tenant: String,
                          id: Long = NioEvent.gen.nextId(),
                          date: DateTime = DateTime.now(DateTimeZone.UTC),
                          payload: Account)
    extends NioEvent {
  def shardId = payload.accountId

  def tYpe = EventType.AccountCreated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "account" -> payload.accountId,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class AccountUpdated(tenant: String,
                          id: Long = NioEvent.gen.nextId(),
                          date: DateTime = DateTime.now(DateTimeZone.UTC),
                          payload: Account,
                          oldValue: Account)
    extends NioEvent {
  def shardId = payload.accountId

  def tYpe = EventType.AccountUpdated

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "account" -> payload.accountId,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson,
    "oldValue" -> oldValue.asJson
  )
}

case class SecuredEvent(id: Long = NioEvent.gen.nextId(),
                        date: DateTime = DateTime.now(DateTimeZone.UTC),
                        payload: Digest)
    extends NioEvent {
  def tYpe = EventType.SecuredEvent

  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.digest
  )

  override val tenant: String = null

  override def shardId: String = "NA"
}

case class DeletionStarted(tenant: String,
                           author: String,
                           id: Long = NioEvent.gen.nextId(),
                           date: DateTime = DateTime.now(DateTimeZone.UTC),
                           payload: DeletionTaskInfoPerApp)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.DeletionStarted
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class DeletionAppDone(tenant: String,
                           author: String,
                           id: Long = NioEvent.gen.nextId(),
                           date: DateTime = DateTime.now(DateTimeZone.UTC),
                           payload: AppDone)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.DeletionAppDone
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class DeletionFinished(tenant: String,
                            author: String,
                            id: Long = NioEvent.gen.nextId(),
                            date: DateTime = DateTime.now(DateTimeZone.UTC),
                            payload: DeletionTask)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.DeletionFinished
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class ExtractionStarted(tenant: String,
                             author: String,
                             id: Long = NioEvent.gen.nextId(),
                             date: DateTime = DateTime.now(DateTimeZone.UTC),
                             payload: ExtractionTaskInfoPerApp)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.ExtractionStarted
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class ExtractionAppDone(tenant: String,
                             author: String,
                             id: Long = NioEvent.gen.nextId(),
                             date: DateTime = DateTime.now(DateTimeZone.UTC),
                             payload: AppDone)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.ExtractionAppDone
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class ExtractionAppFilesMetadataReceived(tenant: String,
                                              author: String,
                                              id: Long = NioEvent.gen.nextId(),
                                              date: DateTime =
                                                DateTime.now(DateTimeZone.UTC),
                                              payload: AppFilesMetadata)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.ExtractionAppFilesMetadataReceived
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}

case class ExtractionFinished(tenant: String,
                              author: String,
                              id: Long = NioEvent.gen.nextId(),
                              date: DateTime = DateTime.now(DateTimeZone.UTC),
                              payload: ExtractionTask)
    extends NioEvent {
  def shardId = payload.userId
  def tYpe = EventType.ExtractionFinished
  def asJson = Json.obj(
    "type" -> tYpe,
    "tenant" -> tenant,
    "author" -> author,
    "date" -> date.toString(DateUtils.utcDateFormatter),
    "id" -> id,
    "payload" -> payload.asJson
  )
}
