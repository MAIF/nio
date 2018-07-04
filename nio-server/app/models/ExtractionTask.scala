package models

import controllers.ReadableEntity
import db.ExtractionTaskMongoDataStore
import messaging.KafkaMessageBroker
import models.ExtractionTaskStatus.ExtractionTaskStatus
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Reads._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import utils.{DateUtils, UploadTracker}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.Elem
import XmlUtil.XmlCleaner

object ExtractionTaskStatus extends Enumeration {
  type ExtractionTaskStatus = Value
  val Running, Done, Expired /* all files from s3 are removed */, Unknown =
    Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)

  implicit val extractionTaskStatusReads = new Reads[ExtractionTaskStatus] {
    def reads(json: JsValue) =
      JsSuccess(ExtractionTaskStatus.withName(json.as[String]))
  }
}

case class FileMetadata(name: String, contentType: String, size: Long) {
  def asJson = Json.obj(
    "name" -> name,
    "contentType" -> contentType,
    "size" -> size
  )

  def asXml = {
    <fileMetadata>
      <name>{name}</name>
      <contentType>{contentType}</contentType>
      <size>{size}</size>
    </fileMetadata>.clean()
  }
}

object FileMetadata {
  implicit val fileMetadataFormats = Json.format[FileMetadata]

  def fromXml(xml: Elem) = {
    val name = (xml \ "name").head.text
    val contentType = (xml \ "contentType").head.text
    val size = (xml \ "size").head.text.toLong
    FileMetadata(name, contentType, size)
  }
}

case class FilesMetadata(files: Seq[FileMetadata]) {
  def asJson = FilesMetadata.filesMetadataFormats.writes(this)

  def asXml =
    <filesMetadata>
      {files.map(_.asXml)}
    </filesMetadata>.clean()
}

object FilesMetadata extends ReadableEntity[FilesMetadata] {
  implicit val filesMetadataFormats = Json.format[FilesMetadata]

  def fromXml(xml: Elem) = {
    Try {
      val files = xml.child.collect {
        case e: Elem => FileMetadata.fromXml(e)
      }
      FilesMetadata(files)
    } match {
      case Success(value) => Right(value)
      case Failure(throwable) => {
        Left(throwable.getMessage)
      }
    }
  }

  def fromJson(json: JsValue) = {
    json.validate[FilesMetadata](filesMetadataFormats) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(errors.mkString(", "))
    }
  }
}

case class AppState(appId: String,
                    files: Seq[FileMetadata],
                    totalBytes: Long,
                    status: ExtractionTaskStatus) {

  def uploadedBytes(taskId: String) =
    UploadTracker.getUploadedBytes(taskId, appId)

  def progress(taskId: String) = {
    val bytes = uploadedBytes(taskId)
    if (bytes == 0l) {
      0l
    } else {
      (bytes * 100L) / totalBytes
    }
  }

  def asJson = AppState.appStateFormats.writes(this)

  def asXml = {
    <appState>
      <appId>{appId}</appId>
      <files>{files.map(_.asXml)}</files>
      <totalBytes>{totalBytes}</totalBytes>
      <status>{status.toString}</status>
    </appState>.clean()
  }
}

object AppState {
  implicit val appStateFormats = Json.format[AppState]
}

// TODO : add expiration date and notfound when reached
case class ExtractionTask(_id: String,
                          orgKey: String,
                          userId: String,
                          startedAt: DateTime,
                          appIds: Set[String],
                          states: Set[AppState],
                          status: ExtractionTaskStatus,
                          lastUpdate: DateTime,
                          done: Int = 0)
    extends ModelTransformAs {

  def asJson = Json.obj(
    "id" -> _id,
    "orgKey" -> orgKey,
    "userId" -> userId,
    "startedAt" -> startedAt.toString(DateUtils.utcDateFormatter),
    "appIds" -> appIds,
    "states" -> states.map(_.asJson),
    "status" -> status.toString,
    "progress" -> progress,
    "lastUpdate" -> lastUpdate.toString(DateUtils.utcDateFormatter),
    "done" -> done
  )

  def asXml = {
    <extractionTask>
      <id>{_id}</id>
      <orgKey>{orgKey}</orgKey>
      <userId>{userId}</userId>
      <startedAt>{startedAt.toString(DateUtils.utcDateFormatter)}</startedAt>
      <appIds>{appIds.map{appId => <appId>{appId}</appId>}}</appIds>
      <states>{states.map{_.asXml}}</states>
      <status>{status}</status>
      <progress>{progress}</progress>
      <lastUpdate>{lastUpdate.toString(DateUtils.utcDateFormatter)}</lastUpdate>
      <done>{done}</done>
    </extractionTask>.clean()
  }

  def progress: Double = {
    if (done == appIds.size) {
      100
    } else {
      states.size match {
        case 0 => 0.0
        case size =>
          states.toSeq.map(_.progress(this._id)).foldLeft(0.0)(_ + _) / size
      }
    }
  }

  def allFilesDone(appId: String) =
    states
      .collectFirst {
        case x if x.appId == appId => x.uploadedBytes(this._id) == x.totalBytes
      }
      .contains(true)

  def copyWithUpdatedAppState(
      appId: String,
      appExtractedFiles: FilesMetadata): ExtractionTask = {
    val appState = this.states.find(_.appId == appId).get
    val sizeOfAllFiles = appExtractedFiles.files.foldLeft(0l) { (z, i) =>
      z + i.size
    }
    val newAppState = appState.copy(files = appExtractedFiles.files,
                                    totalBytes = sizeOfAllFiles)

    UploadTracker.addApp(this._id, appId)
    copy(states = states.filterNot(_.appId == appId) + newAppState,
         lastUpdate = DateTime.now(DateTimeZone.UTC))
  }

  def copyWithFileUploadHandled(appId: String, appState: AppState) = {
    // All files are uploaded for this app
    val newAppState = appState.copy(status = ExtractionTaskStatus.Done)
    val allWillBeDone = this.done + 1 == appIds.size
    copy(
      states = states.filterNot(_.appId == appId) + newAppState,
      done = done + 1,
      status =
        if (allWillBeDone) ExtractionTaskStatus.Done
        else ExtractionTaskStatus.Running,
      lastUpdate = DateTime.now(DateTimeZone.UTC)
    )
  }

  def storeAndEmitEvents(tenant: String,
                         appId: String,
                         author: String,
                         metadata: Option[Seq[(String, String)]] = None)(
      implicit ec: ExecutionContext,
      store: ExtractionTaskMongoDataStore,
      broker: KafkaMessageBroker) = {
    // Store updated task then emit events
    store.updateById(tenant, this._id, this).map { _ =>
      broker.publish(
        ExtractionAppDone(tenant = tenant,
                          author = author,
                          metadata = metadata,
                          payload = AppDone(orgKey, userId, appId)))

      if (this.done == appIds.size) {
        UploadTracker.removeApp(this._id, appId)
        broker.publish(
          ExtractionFinished(tenant = tenant,
                             author = author,
                             metadata = metadata,
                             payload = this))
      }
    }
  }

  def expire(tenant: String)(implicit ec: ExecutionContext,
                             store: ExtractionTaskMongoDataStore) =
    store.updateById(tenant,
                     this._id,
                     this.copy(status = ExtractionTaskStatus.Expired))
}

case class ExtractionTaskInfoPerApp(orgKey: String,
                                    userId: String,
                                    appId: String,
                                    extractionTaskId: String) {
  def asJson = Json.obj(
    "orgKey" -> orgKey,
    "userId" -> userId,
    "appId" -> appId,
    "extractionTaskId" -> extractionTaskId
  )
}

object ExtractionTask {

  implicit val dateFormats = DateUtils.utcDateTimeFormats

  implicit val fmt = Json.format[ExtractionTask]

  def newFrom(orgKey: String, userId: String, appIds: Set[String]) = {
    val now = DateTime.now(DateTimeZone.UTC)
    ExtractionTask(
      _id = BSONObjectID.generate().stringify,
      orgKey = orgKey,
      userId = userId,
      startedAt = now,
      appIds = appIds,
      states = appIds.map(
        appId =>
          AppState(appId,
                   Seq.empty[FileMetadata],
                   0l,
                   ExtractionTaskStatus.Running)),
      status = ExtractionTaskStatus.Running,
      lastUpdate = now
    )
  }
}

case class PagedExtractionTasks(page: Int,
                                pageSize: Int,
                                count: Int,
                                items: Seq[ExtractionTask])
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
