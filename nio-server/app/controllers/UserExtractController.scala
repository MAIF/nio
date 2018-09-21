package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import auth.AuthAction
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult}
import db.{OrganisationMongoDataStore, UserExtractTaskDataStore}
import messaging.KafkaMessageBroker
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.streams.Accumulator
import play.api.mvc.{BodyParser, ControllerComponents}
import utils.{FSUserExtractManager, S3ExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class UserExtractController(
    actorSystem: ActorSystem,
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    userExtractTaskDataStore: UserExtractTaskDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    broker: KafkaMessageBroker,
    fSUserExtractManager: FSUserExtractManager)(
    implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {
  implicit val readable: ReadableEntity[UserExtract] = UserExtract

  implicit val s3ExecutionContext: S3ExecutionContext = S3ExecutionContext(
    actorSystem.dispatchers.lookup("S3-dispatcher"))

  def extractData(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(bodyParser) { implicit req =>
      // Read body
      req.body.read[UserExtract] match {
        case Left(error) =>
          Logger.error("Unable to parse user extract  " + error)
          Future.successful(error.badRequest())
        case Right(userExtract) =>
          // control if an organisation with the orgkey exist
          organisationMongoDataStore
            .findByKey(tenant, orgKey)
            .flatMap {
              case Some(_) =>
                // control if an extract task already exist
                userExtractTaskDataStore
                  .find(tenant, orgKey, userId)
                  .flatMap {
                    // if an extract has been already asked we send a conflict status
                    case Some(_) =>
                      Future.successful(
                        s"extract.for.user.$userId.already.asked".conflict())
                    case None =>
                      val userExtractTask: UserExtractTask =
                        UserExtractTask.instance(tenant,
                                                 orgKey,
                                                 userId,
                                                 userExtract.email)

                      // Insert userExtractTask on Db
                      userExtractTaskDataStore
                        .create(userExtractTask)
                        .map { _ =>
                          // publish associate event to kafka
                          broker.publish(
                            UserExtractTaskAsked(
                              tenant = tenant,
                              author = req.authInfo.sub,
                              metadata = req.authInfo.metadatas,
                              payload = userExtractTask
                            )
                          )

                          renderMethod(userExtractTask, Ok)
                        }
                  }
              // if the organisation doesn't exist
              case None =>
                Future.successful(s"organisation.$orgKey.not.found".notFound())
            }
      }
    }

  def extractedData(tenant: String, orgKey: String, page: Int, pageSize: Int) =
    AuthAction.async { implicit req =>
      // control if an organisation with the orgkey exist
      organisationMongoDataStore
        .findByKey(tenant, orgKey)
        .flatMap {
          // if the organisation doesn't exist
          case None =>
            Future.successful(s"organisation.$orgKey.not.found".notFound())
          case Some(_) =>
            userExtractTaskDataStore
              .findByOrgKey(tenant, orgKey, page, pageSize)
              .map { res =>
                renderMethod(UserExtractTasks(page = page,
                                              pageSize = pageSize,
                                              count = res._2,
                                              items = res._1),
                             Ok)
              }
        }

    }
  def userExtractedData(tenant: String,
                        orgKey: String,
                        userId: String,
                        page: Int,
                        pageSize: Int) =
    AuthAction.async { implicit req =>
      // control if an organisation with the orgkey exist
      organisationMongoDataStore
        .findByKey(tenant, orgKey)
        .flatMap {
          // if the organisation doesn't exist
          case None =>
            Future.successful(s"organisation.$orgKey.not.found".notFound())
          case Some(_) =>
            userExtractTaskDataStore
              .findByOrgKeyAndUserId(tenant, orgKey, userId, page, pageSize)
              .map { res =>
                renderMethod(UserExtractTasks(page = page,
                                              pageSize = pageSize,
                                              count = res._2,
                                              items = res._1),
                             Ok)
              }
        }

    }

  def uploadFile(tenant: String, orgKey: String, userId: String, name: String) =
    AuthAction.async(streamFile) { implicit req =>
      // control if an extract task for this user/organisation/tenant exist
      userExtractTaskDataStore
        .find(tenant, orgKey, userId)
        .flatMap {
          case None =>
            Future.successful(
              s"user.extract.task.for.user.$userId.and.organisation.$orgKey.not.found"
                .notFound())
          case Some(extractTask) =>
            val taskUpdateUploadDate: UserExtractTask = extractTask.copy(
              uploadStartedAt = Some(DateTime.now(DateTimeZone.UTC)))

            // specified upload started date
            userExtractTaskDataStore
              .update(extractTask._id, taskUpdateUploadDate)
              .flatMap { _ =>
                // Send file to S3
                fSUserExtractManager
                  .userExtractUpload(tenant,
                                     orgKey,
                                     userId,
                                     extractTask._id,
                                     name,
                                     req.body)
                  .flatMap { locationAddress =>
                    val task: UserExtractTask = taskUpdateUploadDate.copy(
                      endedAt = Some(DateTime.now(DateTimeZone.UTC)))

                    // Update extract task with endedAt date
                    userExtractTaskDataStore
                      .update(extractTask._id, task)
                      .map { _ =>
                        // publish associate event to kafka
                        broker.publish(
                          UserExtractTaskCompleted(
                            tenant = tenant,
                            author = req.authInfo.sub,
                            metadata = req.authInfo.metadatas,
                            payload = task
                          )
                        )
                        // TODO Send mail with public link to S3
                        Ok(Json.obj("url" -> locationAddress))
                      }
                  }
              }
        }
    }

  def streamFile: BodyParser[Source[ByteString, _]] =
    BodyParser { req =>
      Accumulator.source[ByteString].map(s => Right(s))
    }

}
