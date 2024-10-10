package controllers

import auth.SecuredAuthContext
import controllers.ErrorManager.ErrorManagerResult
import db.AccountMongoDataStore
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models._
import utils.NioLogger
import play.api.mvc.{Action, ActionBuilder, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class AccountController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val accountStore: AccountMongoDataStore,
    broker: KafkaMessageBroker
)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[Account] = Account

  def find(tenant: String, accountId: String): Action[AnyContent] = AuthAction.async { implicit req =>
    accountStore
      .findByAccountId(tenant, accountId)
      .map {
        case Some(account) => renderMethod(account)
        case None          => "error.unknown.account".notFound()
      }
  }

  def findAll(tenant: String, page: Int, pageSize: Int): Action[AnyContent] = AuthAction.async { implicit req =>
    accountStore
      .findAll(tenant, page, pageSize)
      .map(accounts => renderMethod(Accounts(accounts)))
  }

  def create(tenant: String): Action[XmlOrJson] = AuthAction(bodyParser).async { implicit req =>
    req.body.read[Account] match {
      case Left(error)    =>
        NioLogger.error(s"Invalid account format $error")
        Future.successful("error.invalid.account.format".badRequest())
      case Right(account) =>
        accountStore.findByAccountId(tenant, account.accountId).flatMap {
          case Some(_) =>
            Future.successful("error.account.id.already.used".conflict())
          case None    =>
            // TODO add validation
            accountStore.create(tenant, account).map { _ =>
              broker.publish(
                AccountCreated(tenant, req.authInfo.sub, metadata = req.authInfo.metadatas, payload = account)
              )
              renderMethod(account, Created)

            }
        }
    }
  }

  def update(tenant: String, accountId: String): Action[XmlOrJson] =
    AuthAction(bodyParser).async { implicit req =>
      req.body.read[Account] match {
        case Left(error)                                      =>
          NioLogger.error(s"Invalid account format $error")
          Future.successful("error.invalid.account.format".badRequest())
        case Right(account) if accountId == account.accountId =>
          accountStore.findByAccountId(tenant, account.accountId).flatMap {
            case Some(oldAccount) =>
              // TODO add validation
              accountStore.update(tenant, accountId, account).map { _ =>
                broker.publish(
                  AccountUpdated(
                    tenant,
                    author = req.authInfo.sub,
                    payload = account,
                    oldValue = oldAccount,
                    metadata = req.authInfo.metadatas
                  )
                )
                renderMethod(account)
              }
            case None =>
              Future.successful("error.account.not.found".notFound())
          }

        case Right(_)  =>
          Future.successful("error.accountId.is.immutable".badRequest())
      }
    }

  def delete(tenant: String, accountId: String): Action[AnyContent] = AuthAction.async { implicit req =>
    accountStore.findByAccountId(tenant, accountId).flatMap {
      case Some(account) =>
        accountStore
          .delete(tenant, accountId)
          .map { _ =>
            broker.publish(
              AccountDeleted(tenant, author = req.authInfo.sub, payload = account, metadata = req.authInfo.metadatas)
            )
            Ok
          }
      case None          =>
        Future.successful("error.account.not.found".notFound())
    }
  }
}
