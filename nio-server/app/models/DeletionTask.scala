package models

import models.DeletionTaskStatus.DeletionTaskStatus
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import utils.DateUtils
import libs.xml.XmlUtil.XmlCleaner

object DeletionTaskStatus extends Enumeration {
  type DeletionTaskStatus = Value
  val Running, Done, Unknown = Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)

  implicit val deletionTaskStatusReads = new Reads[DeletionTaskStatus] {
    def reads(json: JsValue) =
      JsSuccess(DeletionTaskStatus.withName(json.as[String]))
  }
}

case class AppDeletionState(appId: String, status: DeletionTaskStatus) {

  def asJson = Json.obj("appId" -> appId, "status" -> status.toString)

  def asXml = {
    <appDestroyState>
      <appId>{appId}</appId>
      <status>{status.toString}</status>
    </appDestroyState>.clean()
  }
}
object AppDeletionState {
  implicit val appDeletionStateFormats = Json.format[AppDeletionState]
}

case class DeletionTaskInfoPerApp(orgKey: String,
                                  userId: String,
                                  appId: String,
                                  deletionTaskId: String) {
  def asJson = Json.obj(
    "orgKey" -> orgKey,
    "userId" -> userId,
    "appId" -> appId,
    "deletionTaskId" -> deletionTaskId
  )
}

case class DeletionTask(_id: String,
                        orgKey: String,
                        userId: String,
                        startedAt: DateTime,
                        appIds: Set[String],
                        states: Set[AppDeletionState],
                        status: DeletionTaskStatus,
                        lastUpdate: DateTime)
    extends ModelTransformAs {

  def copyWithAppDone(appId: String) = {
    val newStates = states.filterNot(_.appId == appId) + AppDeletionState(
      appId,
      DeletionTaskStatus.Done)
    if (newStates.forall(_.status == DeletionTaskStatus.Done)) {
      this.copy(states = newStates, status = DeletionTaskStatus.Done)
    } else {
      this.copy(states = newStates)
    }
  }

  def asJson = Json.obj(
    "id" -> _id,
    "orgKey" -> orgKey,
    "userId" -> userId,
    "startedAt" -> startedAt.toString(DateUtils.utcDateFormatter),
    "appIds" -> appIds,
    "states" -> states.map(_.asJson),
    "status" -> status.toString,
    "lastUpdate" -> startedAt.toString(DateUtils.utcDateFormatter)
  )

  def asXml = {
    <destroyTask>
      <orgKey>{orgKey}</orgKey>
      <userId>{userId}</userId>
      <startedAt>{startedAt.toString(DateUtils.utcDateFormatter)}</startedAt>
      <appIds>{appIds.map(appId => <appId>appId</appId>)}</appIds>
      <states>{states.map(_.asXml)}</states>
      <status>{status.toString}</status>
      <lastUpdate>{lastUpdate.toString(DateUtils.utcDateFormatter)}</lastUpdate>
    </destroyTask>.clean()
  }
}

object DeletionTask {

  implicit val dateTimeFormats = DateUtils.utcDateTimeFormats
  implicit val deletionTaskFormats = Json.format[DeletionTask]

  def newTask(orgKey: String, userId: String, appIds: Set[String]) = {
    val now = DateTime.now(DateTimeZone.UTC)
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

case class PagedDeletionTasks(page: Int,
                              pageSize: Int,
                              count: Int,
                              items: Seq[DeletionTask])
    extends ModelTransformAs {

  def asJson =
    Json.obj("page" -> page,
             "pageSize" -> pageSize,
             "count" -> count,
             "items" -> JsArray(items.map(_.asJson)))

  def asXml =
    <pagedDeletionTasks>
      <page>{page}</page>
      <pageSize>{pageSize}</pageSize>
      <count>{count}</count>
      <items>{items.map(_.asXml)}</items>
    </pagedDeletionTasks>.clean()

}
