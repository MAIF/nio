package controllers

import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import auth.{AuthAction, ExtractionAction, ReqWithExtractionTask}
import com.amazonaws.services.s3.model.{
  CompleteMultipartUploadRequest,
  InitiateMultipartUploadRequest,
  UploadPartRequest,
  UploadPartResult
}
import db.ExtractionTaskMongoDataStore
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc.{BodyParser, ControllerComponents}
import s3.{S3, S3Configuration, S3FileDataStore}
import utils.UploadTracker

import scala.concurrent.{ExecutionContext, Future}

import ErrorManager.ErrorManagerResult
import ErrorManager.AppErrorManagerResult

class ExtractionController(val AuthAction: AuthAction,
                           val cc: ControllerComponents,
                           val s3Conf: S3Configuration,
                           val s3: S3,
                           val s3FileDataStore: S3FileDataStore)(
                            implicit val ec: ExecutionContext,
                            val system: ActorSystem,
                            val store: ExtractionTaskMongoDataStore,
                            val broker: KafkaMessageBroker)
  extends ControllerUtils(cc) {

  implicit val mat = ActorMaterializer()(system)

  val fileBodyParser: BodyParser[Source[ByteString, _]] = BodyParser { _ =>
    Accumulator.source[ByteString].map(s => Right(s))
  }
  implicit val readable: ReadableEntity[AppIds] = AppIds
  implicit val readableFileMetadata: ReadableEntity[FilesMetadata] =
    FilesMetadata

  def startExtractionTask(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(bodyParser) { implicit req =>
      req.body.read[AppIds] match {
        case Left(error) =>
          Logger.error(s"Unable to parse extraction task input due to $error")
          Future.successful(error.badRequest())
        case Right(o) =>
          val task = ExtractionTask.newFrom(orgKey, userId, o.appIds.toSet)
          store.insert(tenant, task).map { _ =>
            task.appIds.foreach { appId =>
              broker.publish(
                ExtractionStarted(tenant = tenant,
                  author = req.authInfo.sub,
                  metadata = req.authInfo.metadatas,
                  payload = ExtractionTaskInfoPerApp(
                    orgKey = orgKey,
                    userId = userId,
                    appId = appId,
                    extractionTaskId = task._id
                  ))
              )
            }
            renderMethod(task, Created)
          }
      }
    }

  def setFilesMetadata(tenant: String,
                       orgKey: String,
                       extractionTaskId: String,
                       appId: String) = AuthAction.async(bodyParser) {
    implicit req =>
      req.body.read[FilesMetadata] match {
        case Left(error) =>
          Logger.error(s"Unable to parse extraction task input due to $error")
          Future.successful(error.badRequest())
        case Right(extractedFiles) =>
          store.findById(tenant, extractionTaskId).flatMap {
            case None =>
              Future.successful("error.unknown.extractionTaskId".notFound())
            case Some(task) if !task.appIds.contains(appId) =>
              Future.successful("error.unknown.appId".notFound())
            case Some(task) =>
              val updatedTask =
                task.copyWithUpdatedAppState(appId, extractedFiles)
              store.updateById(tenant, task._id, updatedTask).map { _ =>
                broker.publish(
                  ExtractionAppFilesMetadataReceived(
                    tenant = tenant,
                    author = req.authInfo.sub,
                    metadata = req.authInfo.metadatas,
                    payload = AppFilesMetadata(
                      orgKey,
                      task.userId,
                      appId,
                      extractedFiles.files
                    )
                  )
                )
                renderMethod(updatedTask)
              }
          }
      }
  }

  def allExtractionTasksByOrgKey(tenant: String,
                                 orgKey: String,
                                 page: Int = 0,
                                 pageSize: Int = 10) =
    AuthAction.async { implicit request =>
      store.findAllByOrgKey(tenant, orgKey, page, pageSize).map {
        case (extractionTasks, count) =>
          val pagedExtractionTasks =
            PagedExtractionTasks(page, pageSize, count, extractionTasks)
          renderMethod(pagedExtractionTasks)
      }
    }

  def findExtractedTask(tenant: String,
                        orgKey: String,
                        extractionTaskId: String) =
    AuthAction.async { implicit request =>
      store.findById(tenant, extractionTaskId).map {
        case None => "error.extraction.task.not.found".notFound()
        case Some(extractionTask) => renderMethod(extractionTask)
      }
    }

  def uploadFile(tenant: String,
                 orgKey: String,
                 extractionTaskId: String,
                 appId: String,
                 name: String) =
    ExtractionAction(tenant, extractionTaskId, fileBodyParser).async {
      implicit req =>
        if (!req.task.appIds.contains(appId)) {
          Future.successful("error.unknown.app".notFound())
        } else {
          val appState = req.task.states.find(_.appId == appId).get
          appState.files.find(_.name == name) match {
            case None =>
              Future.successful("error.unknown.appId".notFound())
            case Some(fileMetadata) =>
              (if (s3Conf.v4auth) {
                upload(tenant, req.task, appId, name)
              } else {
                oldUpload(tenant, req.task, appId, name)
              }).flatMap { location =>
                // Check all files are done for this app
                if (req.task.allFilesDone(appId)) {
                  val updatedTask =
                    req.task.copyWithFileUploadHandled(appId, appState)
                  updatedTask
                    .storeAndEmitEvents(tenant,
                      appId,
                      req.authInfo.sub,
                      req.authInfo.metadatas)
                    .map { _ =>
                      Ok(location)
                    }
                } else {
                  Future.successful(Ok(location))
                }
              }
          }
        }
    }

  private def upload(
                      tenant: String,
                      task: ExtractionTask,
                      appId: String,
                      name: String)(implicit req: ReqWithExtractionTask[Source[ByteString, _]])
  : Future[String] = {
    req.body
      .via(
        Flow[ByteString].map { chunk =>
          UploadTracker.incrementUploadedBytes(task._id, appId, chunk.size)
          chunk
        }
      )
      .runWith(
        s3FileDataStore
          .store(tenant, task.orgKey, task.userId, task._id, appId, name)
      )
      .map { res =>
        res.location.toString()
      }
  }

  private def oldUpload(
                         tenant: String,
                         task: ExtractionTask,
                         appId: String,
                         name: String)(implicit req: ReqWithExtractionTask[Source[ByteString, _]])
  : Future[String] = {
    val uploadKey =
      s"$tenant/${task.orgKey}/${task.userId}/${task._id}/$appId/$name"

    val s3Req = new InitiateMultipartUploadRequest(s3Conf.bucketName, uploadKey)

    val res = s3.client.initiateMultipartUpload(s3Req)

    val uploadId = res.getUploadId

    var partNumber = 1
    req.body
      .via(
        Flow[ByteString]
          .grouped(s3Conf.chunkSizeInMb)
          .map { chunks =>
            val groupedChunk = chunks.reduce(_ ++ _)
            val uploadPart = new UploadPartRequest()
              .withBucketName(s3Conf.bucketName)
              .withKey(uploadKey)
              .withUploadId(uploadId)
              .withPartNumber(partNumber)
              .withPartSize(groupedChunk.size)
              .withInputStream(new ByteArrayInputStream(
                groupedChunk.toArray[Byte]) {
                override def read(): Int = {
                  // Notify upload progress
                  UploadTracker.incrementUploadedBytes(task._id, appId, 1)
                  super.read()
                }

                override def read(b: Array[Byte], off: Int, len: Int): Int = {
                  // Notify upload progress
                  UploadTracker.incrementUploadedBytes(task._id, appId, len)
                  super.read(b, off, len)
                }
              })
            partNumber = partNumber + 1
            s3.client.uploadPart(uploadPart)
          }
      )
      .runFold(Seq[UploadPartResult]()) { (results, uploadResult) =>
        results :+ uploadResult
      }
      .map { results =>
        val tags = scala.collection.JavaConverters
          .seqAsJavaList(results.map(_.getPartETag))
        val completeRequest =
          new CompleteMultipartUploadRequest(s3Conf.bucketName,
            uploadKey,
            uploadId,
            tags)
        val result = s3.client.completeMultipartUpload(completeRequest)
        result.getLocation
      }
  }

}
