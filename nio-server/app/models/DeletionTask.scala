package models

import models.DeletionTaskStatus.DeletionTaskStatus

import java.time.{Clock, LocalDateTime}
import play.api.libs.json._
import reactivemongo.api.bson.BSONObjectID
import utils.DateUtils
import libs.xml.XmlUtil.XmlCleaner

import scala.collection.Seq
import scala.xml.Elem

object DeletionTaskStatus extends Enumeration {
  type DeletionTaskStatus = Value
  val Running, Done, Unknown = Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)

  implicit val deletionTaskStatusReads: Reads[DeletionTaskStatus] = (json: JsValue) => JsSuccess(DeletionTaskStatus.withName(json.as[String]))
}

case class AppDeletionState(appId: String, status: DeletionTaskStatus) {

  def asJson(): JsObject = Json.obj("appId" -> appId, "status" -> status.toString)

  def asXml(): Elem = <appDestroyState>
      <appId>{appId}</appId>
      <status>{status.toString}</status>
    </appDestroyState>.clean()
}
object AppDeletionState {
  implicit val appDeletionStateFormats: OFormat[AppDeletionState] = Json.format[AppDeletionState]
}

case class DeletionTaskInfoPerApp(orgKey: String, userId: String, appId: String, deletionTaskId: String) {
  def asJson(): JsObject = Json.obj(
    "orgKey"         -> orgKey,
    "userId"         -> userId,
    "appId"          -> appId,
    "deletionTaskId" -> deletionTaskId
  )
}

case class DeletionTask(
    _id: String,
    orgKey: String,
    userId: String,
    startedAt: LocalDateTime,
    appIds: Set[String],
    states: Set[AppDeletionState],
    status: DeletionTaskStatus,
    lastUpdate: LocalDateTime
) extends ModelTransformAs {

  def copyWithAppDone(appId: String): DeletionTask = {
    val newStates = states.filterNot(_.appId == appId) + AppDeletionState(appId, DeletionTaskStatus.Done)
    if (newStates.forall(_.status == DeletionTaskStatus.Done)) {
      this.copy(states = newStates, status = DeletionTaskStatus.Done)
    } else {
      this.copy(states = newStates)
    }
  }

  def asJson(): JsObject = Json.obj(
    "id"         -> _id,
    "orgKey"     -> orgKey,
    "userId"     -> userId,
    "startedAt"  -> startedAt.format(DateUtils.utcDateFormatter),
    "appIds"     -> appIds,
    "states"     -> states.map(_.asJson()),
    "status"     -> status.toString,
    "lastUpdate" -> startedAt.format(DateUtils.utcDateFormatter)
  )

  def asXml(): Elem = <destroyTask>
      <orgKey>{orgKey}</orgKey>
      <userId>{userId}</userId>
      <startedAt>{startedAt.format(DateUtils.utcDateFormatter)}</startedAt>
      <appIds>{appIds.map(_ => <appId>appId</appId>)}</appIds>
      <states>{states.map(_.asXml())}</states>
      <status>{status.toString}</status>
      <lastUpdate>{lastUpdate.format(DateUtils.utcDateFormatter)}</lastUpdate>
    </destroyTask>.clean()
}

object DeletionTask {

  implicit val dateTimeFormats: Format[LocalDateTime] = DateUtils.utcDateTimeFormats
  implicit val deletionTaskFormats: OFormat[DeletionTask] = Json.format[DeletionTask]

  def newTask(orgKey: String, userId: String, appIds: Set[String]): DeletionTask = {
    val now = LocalDateTime.now(Clock.systemUTC)
    DeletionTask(
      BSONObjectID.generate().stringify,
      orgKey,
      userId,
      now,
      appIds,
      appIds.map(appId => AppDeletionState(appId, DeletionTaskStatus.Running)),
      DeletionTaskStatus.Running,
      now
    )
  }
}

case class PagedDeletionTasks(page: Int, pageSize: Int, count: Long, items: Seq[DeletionTask])
    extends ModelTransformAs {

  def asJson(): JsObject =
    Json.obj("page" -> page, "pageSize" -> pageSize, "count" -> count, "items" -> JsArray(items.map(_.asJson())))

  def asXml(): Elem = <pagedDeletionTasks>
      <page>{page}</page>
      <pageSize>{pageSize}</pageSize>
      <count>{count}</count>
      <items>{items.map(_.asXml())}</items>
    </pagedDeletionTasks>.clean()

}
