package controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import auth.AuthAction
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult}
import db.{OrganisationMongoDataStore, UserExtractTaskDataStore}
import messaging.KafkaMessageBroker
import models.{
  UserExtract,
  UserExtractTask,
  UserExtractTaskAsked,
  UserExtractTasks
}
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc.{BodyParser, ControllerComponents}
import utils.FSUserExtractManager

import scala.concurrent.{ExecutionContext, Future}

class UserExtractController(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    userExtractTaskDataStore: UserExtractTaskDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    broker: KafkaMessageBroker,
    fSUserExtractManager: FSUserExtractManager)(
    implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {
  implicit val readable: ReadableEntity[UserExtract] = UserExtract

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

  def extractedData(tenant: String, orgKey: String) =
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
              .findByOrgKey(tenant, orgKey)
              .map { userExtractTasks =>
                renderMethod(UserExtractTasks(userExtractTasks), Ok)
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
            // Send file to S3

            // Send mail with public link to S3

            ???
        }
    }

  //
  //  def addToS3(): Flow[ByteString, String, NotUsed] =
  //    Flow[ByteString]
  //      .map(byteString => ???)
  //
  //  def sendMail(): Flow[(String, String), String, NotUsed] =
  //    Flow[(String, String)]
  //      .map((str1, str2) => ???)

  def streamFile: BodyParser[Source[ByteString, _]] =
    BodyParser { req =>
      Accumulator.source[ByteString].map(s => Right(s))
    }

}
