package controllers

import auth.AuthAction
import db.{OrganisationMongoDataStore, UserExtractTaskDataStore}
import messaging.KafkaMessageBroker
import models.{UserExtractTask, UserExtractTaskAsked}
import play.api.mvc.ControllerComponents
import controllers.ErrorManager.ErrorManagerResult

import scala.concurrent.{ExecutionContext, Future}

class UserExtractController(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    userExtractTaskDataStore: UserExtractTaskDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    broker: KafkaMessageBroker)(implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {

  def extractData(tenant: String, orgKey: String, userId: String) =
    AuthAction.async { implicit req =>
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
                    UserExtractTask.instance(tenant, orgKey, userId)

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
