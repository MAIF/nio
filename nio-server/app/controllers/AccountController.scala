package controllers

import auth.AuthAction
import db.AccountMongoDataStore
import javax.inject.{Inject, Singleton}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.mvc.ControllerComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject()(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    val accountStore: AccountMongoDataStore,
    broker: KafkaMessageBroker)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  def find(tenant: String, accountId: String) = AuthAction.async {
    implicit req =>
      accountStore
        .findByAccountId(tenant, accountId)
        .map {
          case Some(account) =>
            renderMethod(account)
          case None =>
            NotFound("error.unknown.account")
        }
  }

  def findAll(tenant: String, page: Int, pageSize: Int) = AuthAction.async {
    implicit req =>
      accountStore
        .findAll(tenant, page, pageSize)
        .map(accounts => renderMethod(Accounts(accounts)))
  }

  def create(tenant: String) = AuthAction(parse.anyContent).async {
    implicit req =>
      parseMethod[Account](Account) match {
        case Left(error) =>
          Logger.error(s"Invalid account format $error")
          Future.successful(BadRequest("error.invalid.account.format"))
        case Right(account) =>
          accountStore.findByAccountId(tenant, account.accountId).flatMap {
            case Some(_) =>
              Future.successful(Conflict("error.account.id.already.used"))
            case None =>
              // TODO add validation
              accountStore.create(tenant, account).map { _ =>
                broker.publish(
                  AccountCreated(tenant, payload = account)
                )
                renderMethod(account, Created)

              }
          }
      }
  }

  def update(tenant: String, accountId: String) =
    AuthAction(parse.anyContent).async { implicit req =>
      parseMethod[Account](Account) match {
        case Left(error) =>
          Logger.error(s"Invalid account format $error")
          Future.successful(BadRequest("error.invalid.account.format"))
        case Right(account) if accountId == account.accountId =>
          accountStore.findByAccountId(tenant, account.accountId).flatMap {
            case Some(oldAccount) =>
              // TODO add validation
              accountStore.update(tenant, accountId, account).map { _ =>
                broker.publish(
                  AccountUpdated(tenant,
                                 payload = account,
                                 oldValue = oldAccount)
                )
                renderMethod(account)
              }
            case None =>
              Future.successful(NotFound("error.account.not.found"))
          }

        case Right(account) if accountId != account.accountId =>
          Future.successful(BadRequest("error.accountId.is.immutable"))
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
                AccountDeleted(tenant, payload = account)
              )
              Ok
            })
        case None =>
          Future.successful(NotFound("error.account.not.found"))
      }
  }
}
