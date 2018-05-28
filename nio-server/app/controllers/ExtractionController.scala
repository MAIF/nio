package controllers

import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import auth.{AuthAction, ExtractionAction, ReqWithExtractionTask}
import com.amazonaws.services.s3.model.{CompleteMultipartUploadRequest, InitiateMultipartUploadRequest, UploadPartRequest, UploadPartResult}
import db.{ConsentFactMongoDataStore, ExtractionTaskMongoDataStore, OrganisationMongoDataStore, UserMongoDataStore}
import javax.inject.{Inject, Singleton}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.streams.Accumulator
import play.api.mvc.{BodyParser, ControllerComponents, Result}
import s3.{S3, S3Configuration, S3FileDataStore}
import utils.UploadTracker

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExtractionController @Inject()(val AuthAction: AuthAction,
                                     val cc: ControllerComponents,
                                     val userStore: UserMongoDataStore,
                                     val consentFactStore: ConsentFactMongoDataStore,
                                     val organisationStore: OrganisationMongoDataStore,
                                     val broker: KafkaMessageBroker,
                                     val s3Conf: S3Configuration,
                                     val s3: S3,
                                     val s3FileDataStore: S3FileDataStore)
                                    (implicit val ec: ExecutionContext, system: ActorSystem, store: ExtractionTaskMongoDataStore)
    extends ControllerUtils(cc) {

  implicit val mat = ActorMaterializer()(system)

  val fileBodyParser: BodyParser[Source[ByteString, _]] = BodyParser{ _ => Accumulator.source[ByteString].map(s => Right(s)) }

  def startExtractionTask(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(parse.anyContent) { implicit req =>
      val parsed = parseMethod[AppIds](AppIds)
      parsed match {
        case Left(error) =>
          Logger.error(s"Unable to parse extraction task input due to $error")
          Future.successful(BadRequest(error))
        case Right(o) =>
          val task = ExtractionTask.newFrom(orgKey, userId, o.appIds.toSet)
          store.insert(tenant, task).map { _ =>
            task.appIds.foreach { appId =>
              broker.publish(
                ExtractionStarted(
                  tenant = tenant,
                  author = req.authInfo.sub,
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

  def setExtractedFiles(tenant: String, orgKey: String, extractionTaskId: String, appId: String) = AuthAction.async(parse.anyContent) { implicit req =>
    parseMethod[FilesMetadata](FilesMetadata) match {
      case Left(error) =>
        Logger.error(s"Unable to parse extraction task input due to $error")
        Future.successful(BadRequest(error))
      case Right(extractedFiles) =>
        store.findById(tenant, extractionTaskId).flatMap {
          case Some(task) =>
            task.setExtractedFiles(tenant, appId, extractedFiles).map { _ =>
              renderMethod(task)
            }
          case None =>
            Future.successful(NotFound("error.unknown.appId"))
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

  def findExtractedTask(tenant: String, orgKey: String, extractionTaskId: String) =
    AuthAction.async { implicit request =>
      store.findById(tenant, extractionTaskId).map {
        case None               => NotFound("error.extraction.task.not.found")
        case Some(extractionTask) => renderMethod(extractionTask)
      }
    }

  def uploadFile(tenant: String, orgKey: String, extractionTaskId: String, appId: String, name: String) =
    ExtractionAction(tenant, extractionTaskId, fileBodyParser).async { implicit req =>
      if (!req.task.appIds.contains(appId)) {
        Future.successful(NotFound("error.unknown.app"))
      } else {
        if (s3Conf.v4auth) {
          upload(tenant, req.task, appId, name)
        } else {
          oldUpload(tenant, req.task, appId, name)
        }
      }
  }

  private def upload(tenant: String, task: ExtractionTask, appId: String, name: String)(implicit req: ReqWithExtractionTask[Source[ByteString, _]]) = {
    req.body.via(
      Flow[ByteString].map { chunk =>
        UploadTracker.incrementUploadedBytes(appId, chunk.size)
        chunk
      }
    ).runWith(
      s3FileDataStore.store(tenant, task.orgKey, task.userId, task._id, appId, name)
    ).flatMap { res =>
      task.handleFileUploaded(tenant, appId, req.authInfo.sub).map { _ =>
        Ok(Json.obj("location" -> res.location.toString()))
      }
    }
  }

  private def oldUpload(tenant: String, task: ExtractionTask, appId: String, name: String)(implicit req: ReqWithExtractionTask[Source[ByteString, _]]): Future[Result] = {
    val uploadKey = s"$tenant/${task.orgKey}/${task.userId}/${task._id}/$appId/$name"

    val s3Req = new InitiateMultipartUploadRequest(s3Conf.bucketName, uploadKey)

    val res = s3.client.initiateMultipartUpload(s3Req)

    val uploadId = res.getUploadId

    var partNumber = 1
    req.body.via(
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
            .withInputStream(new ByteArrayInputStream(groupedChunk.toArray[Byte]) {
              override def read(): Int = {
                // Notify upload progress
                UploadTracker.incrementUploadedBytes(appId, 1)
                super.read()
              }
              override def read(b: Array[Byte], off: Int, len: Int): Int = {
                // Notify upload progress
                UploadTracker.incrementUploadedBytes(appId, len)
                super.read(b, off, len)
              }
            })
          partNumber = partNumber + 1
          s3.client.uploadPart(uploadPart)
        }
    ).runFold(Seq[UploadPartResult]()){ (results, uploadResult) =>
      results :+ uploadResult
    }.flatMap { results =>
      val tags = scala.collection.JavaConverters.seqAsJavaList(results.map(_.getPartETag))
      val completeRequest = new CompleteMultipartUploadRequest(s3Conf.bucketName, uploadKey, uploadId, tags)
      val result = s3.client.completeMultipartUpload(completeRequest)
      task.handleFileUploaded(tenant, appId, req.authInfo.sub).map { _ =>
        Ok(result.getLocation)
      }
    }
  }

}
