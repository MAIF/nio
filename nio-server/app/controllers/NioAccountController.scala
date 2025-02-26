package controllers

import org.apache.pekko.http.scaladsl.util.FastFuture
import auth.SecuredAuthContext
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult}
import db.NioAccountMongoDataStore
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models._
import play.api.mvc.{Action, ActionBuilder, AnyContent, ControllerComponents}
import utils.Sha

import scala.concurrent.{ExecutionContext, Future}

class NioAccountController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val nioAccountMongoDataStore: NioAccountMongoDataStore,
    broker: KafkaMessageBroker
)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[NioAccount] = NioAccount

  def create: Action[XmlOrJson] = AuthAction(bodyParser).async { implicit req =>
    req.authInfo.isAdmin match {
      case true  =>
        req.body.read[NioAccount] match {
          case Left(e)           =>
            Future.successful(e.badRequest())
          case Right(nioAccount) =>
            nioAccountMongoDataStore
              .findByEmail(nioAccount.email)
              .flatMap {
                case Some(_) =>
                  Future.successful(
                    s"error.account.email.already.used"
                      .conflict()
                  )
                case None                   =>
                  val accountToStore: NioAccount =
                    nioAccount.copy(password = Sha.hexSha512(nioAccount.password))

                  nioAccountMongoDataStore
                    .insertOne(accountToStore)
                    .map(_ => renderMethod(accountToStore, Created))
              }

        }
      case false =>
        FastFuture.successful("admin.action.forbidden".forbidden())
    }

  }

  def update(nioAccountId: String): Action[XmlOrJson] =
    AuthAction(bodyParser).async { implicit req =>
      implicit val readable: ReadableEntity[NioAccountUpdate] = NioAccountUpdate

      req.authInfo.isAdmin match {
        case true  =>
          req.body.read[NioAccountUpdate] match {
            case Left(e)           =>
              Future.successful(e.badRequest())
            case Right(nioAccount) =>
              nioAccountMongoDataStore.findById(nioAccountId).flatMap {
                case Some(nioAccountStored) =>
                  val nioAccountToStore =
                    nioAccountStored.copy(
                      isAdmin = nioAccount.isAdmin,
                      offerRestrictionPatterns = nioAccount.offerRestrictionPatterns
                    )
                  nioAccountMongoDataStore
                    .updateOne(nioAccountStored._id, nioAccountToStore)
                    .map(_ => renderMethod(nioAccountToStore))
                case None                   =>
                  Future.successful(s"error.account.id.$nioAccountId.not.found".notFound())
              }
          }
        case false =>
          FastFuture.successful("admin.action.forbidden".forbidden())
      }
    }

  def findAll(page: Int, pageSize: Int): Action[AnyContent] = AuthAction.async { implicit req =>
    nioAccountMongoDataStore
      .findManyPaginate(page = page, pageSize = pageSize)
      .map { res =>
        renderMethod(NioAccounts(page, pageSize, res._2, res._1))
      }
  }

  def find(nioAccountId: String): Action[AnyContent] = AuthAction.async { implicit req =>
    nioAccountMongoDataStore
      .findById(nioAccountId)
      .map {
        case Some(nioAccount) =>
          renderMethod(nioAccount)
        case None             =>
          s"error.account.id.$nioAccountId.not.found".notFound()
      }
  }

  def delete(nioAccountId: String): Action[AnyContent] = AuthAction.async { implicit req =>
    req.authInfo.isAdmin match {
      case true =>
        nioAccountMongoDataStore.findById(nioAccountId).flatMap {
          case Some(nioAccountStored) =>
            nioAccountMongoDataStore
              .deleteOne(nioAccountId)
              .map(_ => renderMethod(nioAccountStored))
          case None                   =>
            Future.successful(s"error.account.id.$nioAccountId.not.found".notFound())
        }

      case false =>
        FastFuture.successful("admin.action.forbidden".forbidden())
    }
  }

}
