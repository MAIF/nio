package controllers

import akka.actor.ActorSystem
import auth.{AuthAction, SecuredAction, SecuredAuthContext}
import db.{
  ConsentFactMongoDataStore,
  DeletionTaskMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.mvc.{ActionBuilder, AnyContent, ControllerComponents}
import ErrorManager.ErrorManagerResult
import ErrorManager.AppErrorManagerResult

import scala.concurrent.{ExecutionContext, Future}

class DeletionController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val userStore: UserMongoDataStore,
    val consentFactStore: ConsentFactMongoDataStore,
    val organisationStore: OrganisationMongoDataStore,
    val deletionTaskStore: DeletionTaskMongoDataStore,
    val broker: KafkaMessageBroker)(implicit val ec: ExecutionContext,
                                    system: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[AppIds] = AppIds

  def startDeletionTask(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(bodyParser) { implicit req =>
      req.body.read[AppIds] match {
        case Left(error) =>
          Logger.error(s"Unable to parse deletion task input due to $error")
          Future.successful(error.badRequest())
        case Right(o) =>
          val task = DeletionTask.newTask(orgKey, userId, o.appIds.toSet)
          deletionTaskStore.insert(tenant, task).map { _ =>
            task.appIds.foreach { appId =>
              broker.publish(
                DeletionStarted(
                  tenant = tenant,
                  author = req.authInfo.sub,
                  payload = DeletionTaskInfoPerApp(
                    orgKey = orgKey,
                    userId = userId,
                    appId = appId,
                    deletionTaskId = task._id
                  ),
                  metadata = req.authInfo.metadatas
                )
              )
            }
            renderMethod(task, Created)
          }
      }
    }

  def allDeletionTasksByOrgKey(tenant: String,
                               orgKey: String,
                               page: Int = 0,
                               pageSize: Int = 10) =
    AuthAction.async { implicit request =>
      deletionTaskStore.findAllByOrgKey(tenant, orgKey, page, pageSize).map {
        case (destroyTasks, count) =>
          val pagedDeletionTasks =
            PagedDeletionTasks(page, pageSize, count, destroyTasks)
          renderMethod(pagedDeletionTasks)
      }
    }

  def findDeletionTask(tenant: String, orgKey: String, deletionId: String) =
    AuthAction.async { implicit request =>
      deletionTaskStore.findById(tenant, deletionId).map {
        case None               => "error.deletion.task.not.found".notFound()
        case Some(deletionTask) => renderMethod(deletionTask)
      }
    }

  def updateDeletionTask(tenant: String,
                         orgKey: String,
                         deletionId: String,
                         appId: String) = AuthAction.async { implicit request =>
    deletionTaskStore.findById(tenant, deletionId).flatMap {
      case None =>
        Future.successful("error.deletion.task.not.found".notFound())
      case Some(deletionTask) if !deletionTask.appIds.contains(appId) =>
        Future.successful("error.unknown.appId".notFound())
      case Some(deletionTask) =>
        val updatedDeletionTask = deletionTask.copyWithAppDone(appId)
        deletionTaskStore
          .updateById(tenant, deletionId, updatedDeletionTask)
          .map { _ =>
            broker.publish(
              DeletionAppDone(
                tenant = tenant,
                author = request.authInfo.sub,
                metadata = request.authInfo.metadatas,
                payload = AppDone(orgKey, updatedDeletionTask.userId, appId)
              )
            )
            if (updatedDeletionTask.status == DeletionTaskStatus.Done) {
              broker.publish(
                DeletionFinished(
                  tenant = tenant,
                  author = request.authInfo.sub,
                  payload = updatedDeletionTask,
                  metadata = request.authInfo.metadatas
                )
              )
            }
            renderMethod(deletionTask)
          }
    }
  }

}
