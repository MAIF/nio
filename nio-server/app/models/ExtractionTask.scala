package models

import controllers.ReadableEntity
import db.ExtractionTaskMongoDataStore
import messaging.KafkaMessageBroker
import models.ExtractionTaskStatus.ExtractionTaskStatus
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import utils.{DateUtils, UploadTracker}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class AppIds(appIds: Seq[String]) {
  def asXml = {
      <appIds>
        {appIds.map(appId => <appId>{appId}</appId>)}
      </appIds>
  }
}
object AppIds extends ReadableEntity[AppIds] {
  implicit val extractionTaskInputFormats = Json.format[AppIds]

  def fromXml(xml: Elem) = {
    Try {
      val appIds = (xml \\ "appId").map(_.head.text)
      AppIds(appIds)
    } match {
      case Success(value) => Right(value)
      case Failure(throwable) => {
        Left(throwable.getMessage)
      }
    }
  }

  def fromJson(json: JsValue) = {
    json.validate[AppIds](extractionTaskInputFormats) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(errors.mkString(", "))
    }
  }
}

object ExtractionTaskStatus extends Enumeration {
  type ExtractionTaskStatus = Value
  val Running, Done, Expired /* all files from s3 are removed */, Unknown = Value

  def from(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(Unknown)

  implicit val extractionTaskStatusReads = new Reads[ExtractionTaskStatus] {
    def reads(json: JsValue) = JsSuccess(ExtractionTaskStatus.withName(json.as[String]))
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
    </fileMetadata>
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

case class FilesMetadata(files: Seq[FileMetadata]){
  def asXml =
    <filesMetadata>
      {files.map(_.asXml)}
    </filesMetadata>
}
object FilesMetadata extends ReadableEntity[FilesMetadata] {
  implicit val appExtractedFilesFormats = Json.format[FilesMetadata]

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
    json.validate[FilesMetadata](appExtractedFilesFormats) match {
      case JsSuccess(o, _) => Right(o)
      case JsError(errors) => Left(errors.mkString(", "))
    }
  }
}

case class AppState(appId: String, files: Seq[FileMetadata], totalBytes: Long, status: ExtractionTaskStatus) {
  def uploadedBytes = UploadTracker.getUploadedBytes(appId)

  def progress = if (uploadedBytes == 0l) { 0l } else { (uploadedBytes *  100L) / totalBytes }

  def asJson = AppState.appStateFormats.writes(this)

  def asXml = {
    <appState>
      <appId>{appId}</appId>
      <files>{files.map(_.asXml)}</files>
      <totalBytes>{totalBytes}</totalBytes>
      <status>{status.toString}</status>
    </appState>
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
    "lastUpdate" -> lastUpdate.toString(DateUtils.utcDateFormatter)
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
    </extractionTask>
  }

  def progress: Double = states.size match {
    case 0 => 0.0
    case size => states.map(_.progress).foldLeft(0.0)(_ + _) / size
  }

  def allAppsDone = done == appIds.size - 1

  def allFilesDone(appId: String) = states.collectFirst{case x if x.appId == appId => x.uploadedBytes == x.totalBytes}

//  def notifyAppsExtractTaskStarted(tenant: String)(implicit ec: ExecutionContext, dataStores: DataStores, broker: MessageBroker[NioEvent]) = {
//    Logger.info(s"Starting extraction task ${this._id}")
//    pushToAllApps(tenant, (app, link) =>  ExtractionStartedEvent(_id, app.clientId, link.appInternalUserId, "whatever")) // TODO : change whatever to actual chosen type
//  }

  def setExtractedFiles(tenant: String, appId: String, appExtractedFiles: FilesMetadata)(implicit ec: ExecutionContext, store: ExtractionTaskMongoDataStore): Future[Boolean] = {
    this.states.find(_.appId == appId) match {
      case Some(appState) =>
        val sizeOfAllFiles = appExtractedFiles.files.foldLeft(0l){ (z,i) => z+i.size }
        val newAppState = appState.copy(files = appExtractedFiles.files, totalBytes = sizeOfAllFiles)

        UploadTracker.addApp(appId)
        val newTask = copy(
          states = states.filterNot(_.appId == appId) + newAppState,
          lastUpdate = DateTime.now(DateTimeZone.UTC))
        store.updateById(tenant, this._id, newTask)
      case None =>
        Future.failed(new RuntimeException(s"Unknown app $appId"))
    }
  }

  def handleFileUploaded(tenant: String, appId: String, author: String)(implicit ec: ExecutionContext, store: ExtractionTaskMongoDataStore, broker: KafkaMessageBroker) = {
    this.states.find(_.appId == appId) match {
      case Some(appState) =>
        // Check all files are done for this app
        if (allFilesDone(appId).contains(true)) {
          // All files are uploaded for this app
          UploadTracker.removeApp(appId)
          val newAppState = appState.copy(status = ExtractionTaskStatus.Done)
          val allDone = allAppsDone
          val newTask = copy(
            states =  states.filterNot(_.appId == appId) + newAppState,
            done = done + 1,
            status = if (allDone) ExtractionTaskStatus.Done else ExtractionTaskStatus.Running,
            lastUpdate = DateTime.now(DateTimeZone.UTC))
          // Store updated task then emit events
          store.updateById(tenant, this._id, newTask).map { _ =>
            broker.publish(ExtractionAppDone(tenant = tenant,
              author = "",
              payload = AppDone(orgKey, userId, appId)))
            if (allDone) {
              broker.publish(ExtractionFinished(tenant = tenant, author = "", payload = this))
            }
          }
        } else {
          // Awaiting next file ...
          Future.successful(())
        }
      case None =>
        Future.failed(new RuntimeException(s"Unknown app $appId"))
    }
  }

  def expire(tenant: String)(implicit ec: ExecutionContext, store: ExtractionTaskMongoDataStore) = store.updateById(tenant, this._id, this.copy(status = ExtractionTaskStatus.Expired))
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

  def newFrom(orgKey: String,
              userId: String,
              appIds: Set[String]) = {
    val now = DateTime.now(DateTimeZone.UTC)
    ExtractionTask(
      _id = BSONObjectID.generate().stringify,
      orgKey = orgKey,
      userId = userId,
      startedAt = now,
      appIds = appIds,
      states = appIds.map(appId => AppState(appId, Seq.empty[FileMetadata], 0l, ExtractionTaskStatus.Running)),
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
    </pagedDeletionTasks>

}
