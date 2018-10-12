package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import auth.AuthAction
import controllers.ErrorManager.{
  AppErrorManagerResult,
  ErrorManagerResult,
  ErrorWithStatusManagerResult
}
import db.OrganisationMongoDataStore
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models.{Offer, Offers}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import service.OfferManagerService

import scala.concurrent.ExecutionContext

class OrganisationOfferController(
    val authAction: AuthAction,
    val cc: ControllerComponents,
    val offerManagerService: OfferManagerService,
    val organisationMongoDataStore: OrganisationMongoDataStore,
    val kafkaMessageBroker: KafkaMessageBroker)(
    implicit val ec: ExecutionContext,
    actorSystem: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[Offer] = Offer
  implicit val materializer: ActorMaterializer =
    ActorMaterializer()(actorSystem)

  def findAll(tenant: String, orgKey: String): Action[AnyContent] =
    authAction.async { implicit req =>
      Logger.info(s"get offers for $orgKey")
      offerManagerService
        .getAll(tenant, orgKey, req.authInfo.offerRestrictionPatterns)
        .map {
          case Left(e) => e.renderError()
          case Right(offers) =>
            Logger.info(
              s"all offers = ${offers.map(_.map(o => Json.stringify(o.asJson())))}")
            renderMethod(Offers(offers))
        }
    }

  def add(tenant: String, orgKey: String): Action[XmlOrJson] =
    authAction.async(bodyParser) { implicit req =>
      req.body.read[Offer] match {
        case Left(error) =>
          Logger.error("Unable to parse offer  " + error)
          FastFuture.successful(error.badRequest())
        case Right(offer) =>
          organisationMongoDataStore
            .findOffer(tenant, orgKey, offer.key)
            .flatMap {
              case Left(e) =>
                FastFuture.successful(e.notFound())
              case Right(maybeOffer) =>
                maybeOffer match {
                  case Some(_) =>
                    FastFuture.successful(
                      s"offer.with.key.${offer.key}.on.organisation.$orgKey.already.exist"
                        .conflict())
                  case None =>
                    offerManagerService
                      .save(tenant,
                            orgKey,
                            None,
                            offer.copy(version = 1),
                            req.authInfo.offerRestrictionPatterns)
                      .map {
                        case Left(e) =>
                          e.renderError()
                        case Right(value) =>
                          renderMethod(value, Created)
                      }
                }

            }

      }
    }

  def update(tenant: String,
             orgKey: String,
             offerKey: String): Action[XmlOrJson] =
    authAction.async(bodyParser) { implicit req =>
      req.body.read[Offer] match {
        case Left(error) =>
          Logger.error("Unable to parse offer  " + error)
          FastFuture.successful(error.badRequest())
        case Right(offer) =>
          organisationMongoDataStore
            .findOffer(tenant, orgKey, offer.key)
            .flatMap {
              case Left(e) =>
                FastFuture.successful(e.badRequest())
              case Right(maybeOffer) =>
                maybeOffer match {
                  case Some(_) =>
                    offerManagerService
                      .save(tenant,
                            orgKey,
                            Some(offerKey),
                            offer.copy(version = offer.version + 1),
                            req.authInfo.offerRestrictionPatterns)
                      .map {
                        case Left(e) =>
                          e.renderError()
                        case Right(value) =>
                          renderMethod(value)
                      }
                  case None =>
                    FastFuture.successful(
                      s"offer.${offer.key}.not.found".notFound())
                }

            }

      }
    }

  def delete(tenant: String,
             orgKey: String,
             offerKey: String): Action[AnyContent] =
    authAction.async { implicit req =>
      offerManagerService
        .delete(tenant, orgKey, offerKey, req.authInfo.offerRestrictionPatterns)
        .map {
          case Left(e) =>
            e.renderError()
          case Right(offer) =>
            renderMethod(offer)
        }
    }

}
