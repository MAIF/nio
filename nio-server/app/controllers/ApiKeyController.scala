package controllers

import org.apache.pekko.http.scaladsl.util.FastFuture
import auth.SecuredAuthContext
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult}
import db.ApiKeyMongoDataStore
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models.{ApiKey, ApiKeyUpdate, ApiKeyValidator, ApiKeys}
import play.api.mvc.{Action, ActionBuilder, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class ApiKeyController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val apiKeyMongoDataStore: ApiKeyMongoDataStore,
    broker: KafkaMessageBroker)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[ApiKey] = ApiKey

  def create: Action[XmlOrJson] = AuthAction(bodyParser).async { implicit req =>
    req.authInfo.isAdmin match {
      case true =>
        req.body.read[ApiKey] match {
          case Left(e) =>
            Future.successful(e.badRequest())
          case Right(apiKey) =>
            apiKeyMongoDataStore
              .findByClientId(apiKey.clientId)
              .flatMap {
                case Some(_) =>
                  Future.successful(
                    s"error.api.key.client.id.already.used"
                      .conflict())

                case None =>
                  ApiKeyValidator.validateApiKey(apiKey) match {
                    case Right(_) =>
                      apiKeyMongoDataStore
                        .insertOne(apiKey)
                        .map(_ => renderMethod(apiKey, Created))
                    case Left(e) =>
                      FastFuture.successful(e.badRequest())
                  }
              }
        }
      case false =>
        FastFuture.successful("admin.action.forbidden".forbidden())
    }

  }

  def update(apiKeyId: String): Action[XmlOrJson] =
    AuthAction(bodyParser).async { implicit req =>
      implicit val readable: ReadableEntity[ApiKeyUpdate] = ApiKeyUpdate

      req.authInfo.isAdmin match {
        case true =>
          req.body.read[ApiKeyUpdate] match {
            case Left(e) =>
              Future.successful(e.badRequest())
            case Right(apiKey) =>
              apiKeyMongoDataStore.findById(apiKeyId).flatMap {
                case Some(apiKeyStored) =>
                  val apiKeyToStore =
                    apiKeyStored.copy(offerRestrictionPatterns =
                      apiKey.offerRestrictionPatterns)
                  apiKeyMongoDataStore
                    .updateOne(apiKeyStored._id, apiKeyToStore)
                    .map(_ => renderMethod(apiKeyToStore))
                case None =>
                  Future.successful(
                    s"error.api.key.$apiKeyId.not.found".notFound())
              }
          }
        case false =>
          FastFuture.successful("admin.action.forbidden".forbidden())
      }
    }

  def findAll(page: Int, pageSize: Int): Action[AnyContent] = AuthAction.async {
    implicit req =>
      apiKeyMongoDataStore
        .findManyPaginate(page = page, pageSize = pageSize)
        .map { res =>
          renderMethod(ApiKeys(page, pageSize, res._2, res._1))
        }
  }

  def find(apiKeyId: String): Action[AnyContent] = AuthAction.async {
    implicit req =>
      apiKeyMongoDataStore
        .findById(apiKeyId)
        .map {
          case Some(apiKey) =>
            renderMethod(apiKey)
          case None =>
            s"error.api.key.$apiKeyId.not.found".notFound()
        }
  }

  def delete(apiKeyId: String): Action[AnyContent] = AuthAction.async {
    implicit req =>
      req.authInfo.isAdmin match {
        case true =>
          apiKeyMongoDataStore.findById(apiKeyId).flatMap {
            case Some(apiKeyStored) =>
              apiKeyMongoDataStore
                .deleteOne(apiKeyId)
                .map(_ => renderMethod(apiKeyStored))
            case None =>
              Future.successful(s"error.api.key.$apiKeyId.not.found".notFound())
          }

        case false =>
          FastFuture.successful("admin.action.forbidden".forbidden())
      }
  }

}
