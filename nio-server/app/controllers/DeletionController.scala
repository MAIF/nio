package controllers

import akka.actor.ActorSystem
import auth.AuthAction
import db.{
  ConsentFactMongoDataStore,
  DeletionTaskMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import javax.inject.{Inject, Singleton}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.mvc.ControllerComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeletionController @Inject()(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    val userStore: UserMongoDataStore,
    val consentFactStore: ConsentFactMongoDataStore,
    val organisationStore: OrganisationMongoDataStore,
    val deletionTaskStore: DeletionTaskMongoDataStore,
    val broker: KafkaMessageBroker)(implicit val ec: ExecutionContext,
                                    system: ActorSystem)
    extends ControllerUtils(cc) {

  def startDeletionTask(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(parse.anyContent) { implicit req =>
      val parsed = parseMethod[DeletionTaskInput](DeletionTaskInput)
      parsed match {
        case Left(error) =>
          Logger.error(s"Unable to parse deletion task input due to $error")
          Future.successful(BadRequest(error))
        case Right(o) =>
          val task = DeletionTask.newTask(orgKey, userId, o.appIds.toSet)
          deletionTaskStore.insert(tenant, task).map { _ =>
            task.appIds.foreach { appId =>
              broker.publish(
                DeletionTaskStarted(tenant = tenant,
                                    author = req.authInfo.sub,
                                    payload = DeletionTaskInfoPerApp(
                                      orgKey = orgKey,
                                      userId = userId,
                                      appId = appId,
                                      deletionTaskId = task._id
                                    ))
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
        case None               => NotFound("error.deletion.task.not.found")
        case Some(deletionTask) => renderMethod(deletionTask)
      }
    }

  def updateDeletionTask(tenant: String,
                         orgKey: String,
                         deletionId: String,
                         appId: String) = AuthAction.async { implicit request =>
    deletionTaskStore.findById(tenant, deletionId).flatMap {
      case None =>
        Future.successful(NotFound("error.deletion.task.not.found"))
      case Some(deletionTask) if !deletionTask.appIds.contains(appId) =>
        Future.successful(NotFound("error.unknown.appId"))
      case Some(deletionTask) =>
        val updatedDeletionTask = deletionTask.copyWithAppDone(appId)
        deletionTaskStore
          .updateById(tenant, deletionId, updatedDeletionTask)
          .map { _ =>
            if (updatedDeletionTask.status == DeletionTaskStatus.Done) {
              broker.publish(
                DeletionTaskDone(
                  tenant = tenant,
                  author = request.authInfo.sub,
                  payload = updatedDeletionTask
                )
              )
            } else {
              broker.publish(
                DeletionTaskUpdated(
                  tenant = tenant,
                  author = request.authInfo.sub,
                  payload = updatedDeletionTask,
                  oldValue = deletionTask
                )
              )
            }
            renderMethod(deletionTask)
          }
    }
  }

}
