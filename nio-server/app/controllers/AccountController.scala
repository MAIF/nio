package controllers

import auth.AuthAction
import controllers.ErrorManager.ErrorManagerResult
import db.AccountMongoDataStore
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.mvc.ControllerComponents

import scala.concurrent.{ExecutionContext, Future}

class AccountController(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    val accountStore: AccountMongoDataStore,
    broker: KafkaMessageBroker)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[Account] = Account

  def find(tenant: String, accountId: String) = AuthAction.async {
    implicit req =>
      accountStore
        .findByAccountId(tenant, accountId)
        .map {
          case Some(account) =>
            renderMethod(account)
          case None =>
            "error.unknown.account".notFound()
        }
  }

  def findAll(tenant: String, page: Int, pageSize: Int) = AuthAction.async {
    implicit req =>
      accountStore
        .findAll(tenant, page, pageSize)
        .map(accounts => renderMethod(Accounts(accounts)))
  }

  def create(tenant: String) = AuthAction(bodyParser).async { implicit req =>
    req.body.read[Account] match {
      case Left(error) =>
        Logger.error(s"Invalid account format $error")
        Future.successful("error.invalid.account.format".badRequest())
      case Right(account) =>
        accountStore.findByAccountId(tenant, account.accountId).flatMap {
          case Some(_) =>
            Future.successful("error.account.id.already.used".conflict())
          case None =>
            // TODO add validation
            accountStore.create(tenant, account).map { _ =>
              broker.publish(
                AccountCreated(tenant,
                               req.authInfo.sub,
                               metadata = req.authInfo.metadatas,
                               payload = account)
              )
              renderMethod(account, Created)

            }
        }
    }
  }

  def update(tenant: String, accountId: String) =
    AuthAction(bodyParser).async { implicit req =>
      req.body.read[Account] match {
        case Left(error) =>
          Logger.error(s"Invalid account format $error")
          Future.successful("error.invalid.account.format".badRequest())
        case Right(account) if accountId == account.accountId =>
          accountStore.findByAccountId(tenant, account.accountId).flatMap {
            case Some(oldAccount) =>
              // TODO add validation
              accountStore.update(tenant, accountId, account).map { _ =>
                broker.publish(
                  AccountUpdated(tenant,
                                 author = req.authInfo.sub,
                                 payload = account,
                                 oldValue = oldAccount,
                                 metadata = req.authInfo.metadatas)
                )
                renderMethod(account)
              }
            case None =>
              Future.successful("error.account.not.found".notFound())
          }

        case Right(account) if accountId != account.accountId =>
          Future.successful("error.accountId.is.immutable".badRequest())
      }
    }

  def delete(tenant: String, accountId: String) = AuthAction.async {
    implicit req =>
      accountStore.findByAccountId(tenant, accountId).flatMap {
        case Some(account) =>
          accountStore
            .delete(tenant, accountId)
            .map(_ => {
              broker.publish(
                AccountDeleted(tenant,
                               author = req.authInfo.sub,
                               payload = account,
                               metadata = req.authInfo.metadatas)
              )
              Ok
            })
        case None =>
          Future.successful("error.account.not.found".notFound())
      }
  }
}
