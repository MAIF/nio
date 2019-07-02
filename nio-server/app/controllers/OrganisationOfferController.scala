package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Framing, Sink, Source}
import akka.util.ByteString
import auth.{AuthInfo, SecuredAuthContext}
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult, ErrorWithStatusManagerResult}
import db.OrganisationMongoDataStore
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models.{ConsentFact, ConsentOffer, DoneBy, Offer, OfferValidator, Offers}
import org.joda.time.DateTime
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.{ActionBuilder, AnyContent, ControllerComponents}
import reactivemongo.bson.BSONObjectID
import service.{ConsentManagerService, OfferManagerService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class OrganisationOfferController(
    val authAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val consentController: ConsentController,
    val consentManagerService: ConsentManagerService,
    val offerManagerService: OfferManagerService,
    val organisationMongoDataStore: OrganisationMongoDataStore,
    val kafkaMessageBroker: KafkaMessageBroker)(
    implicit val ec: ExecutionContext,
    actorSystem: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[Offer] = Offer
  implicit val materializer: ActorMaterializer =
    ActorMaterializer()(actorSystem)

  def findAll(tenant: String, orgKey: String) =
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

  import play.api.mvc._

  private def preSave(tenant: String,
                      orgKey: String,
                      saveAction: (String,
                                   String,
                                   SecuredAuthContext[XmlOrJson],
                                   Offer,
                                   Option[Offer]) => Future[Result])(
      implicit req: SecuredAuthContext[XmlOrJson]) = {
    req.body.read[Offer] match {
      case Left(error) =>
        Logger.error("Unable to parse offer  " + error)
        FastFuture.successful(error.badRequest())
      case Right(offer) =>
        OfferValidator.validateOffer(offer) match {
          case Right(_) =>
            organisationMongoDataStore
              .findOffer(tenant, orgKey, offer.key)
              .flatMap {
                case Left(e) =>
                  FastFuture.successful(e.notFound())
                case Right(maybeOffer) =>
                  saveAction(tenant, orgKey, req, offer, maybeOffer)
              }
          case Left(e) =>
            FastFuture.successful(e.badRequest())
        }

    }
  }

  def add(tenant: String, orgKey: String) =
    authAction.async(bodyParser) { implicit req =>
      val addOffer: (String,
                     String,
                     SecuredAuthContext[XmlOrJson],
                     Offer,
                     Option[Offer]) => Future[Result] =
        (tenant, orgKey, req, offer, maybeOffer) => {
          implicit val request: SecuredAuthContext[XmlOrJson] = req
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

      preSave(
        tenant,
        orgKey,
        addOffer
      )
    }

  def update(tenant: String, orgKey: String, offerKey: String) =
    authAction.async(bodyParser) { implicit req =>
      val updateOffer: (String,
                        String,
                        SecuredAuthContext[XmlOrJson],
                        Offer,
                        Option[Offer]) => Future[Result] =
        (tenant, orgKey, req, offer, maybeOffer) => {
          implicit val request: SecuredAuthContext[XmlOrJson] = req
          maybeOffer match {
            case Some(previousOffer) =>
              offerManagerService
                .save(tenant,
                      orgKey,
                      Some(offerKey),
                      offer.copy(version = previousOffer.version + 1),
                      req.authInfo.offerRestrictionPatterns)
                .map {
                  case Left(e) =>
                    e.renderError()
                  case Right(value) =>
                    renderMethod(value)
                }
            case None =>
              FastFuture.successful(s"offer.${offer.key}.not.found".notFound())
          }
        }
      preSave(
        tenant,
        orgKey,
        updateOffer
      )
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

  val sourceBodyParser: BodyParser[Source[ByteString, _]] = BodyParser("Streaming BodyParser") { _ =>
    Accumulator.source[ByteString].map(Right.apply)
  }

  def handleConsent(tenant: String, orgKey: String, authInfo: AuthInfo, offerKey: String, par: Int, setToFalse: Option[Seq[OfferConsentWithGroup]], source: Source[(String, String), _]): Source[JsValue, _]= {
    source
      .mapAsync(par) {
        case (userId, date) =>
          consentController.getConsentFactTemplate(tenant, orgKey, Some(userId), authInfo.offerRestrictionPatterns)
            .collect {
              case Right(cf) =>
                val consentOffer: Seq[ConsentOffer] = cf.offers match {
                  case None => Seq.empty
                  case Some(offers) => offers.find(co => co.key == offerKey) match {
                    case None => Seq.empty
                    case Some(o) =>
                      val offer: ConsentOffer = o
                          .copy(groups = o
                              .groups
                              .map(group => group
                                  .copy(consents = group
                                      .consents
                                      .map(consent => consent
                                          .copy(checked = setToFalse.forall(groupAndConsents => groupAndConsents.exists(groupAndConsent => groupAndConsent.groupKey == group.key && groupAndConsent.consentKey == consent.key)))))))
                      Seq(offer.copy(lastUpdate = DateTime.parse(date)))
                  }
                }

                val realOffers = cf.offers match {
                  case None => consentOffer
                  case Some(existingsOffers) => existingsOffers ++ consentOffer
                }
                cf.copy(_id = BSONObjectID.generate().stringify, lastUpdateSystem = DateTime.now(),userId = userId, doneBy = DoneBy(authInfo.sub, "admin"), offers = Some(realOffers))
            }
      }
        .mapAsync(par)(consent => consentManagerService
            .saveConsents(tenant,
              authInfo.sub,
              authInfo.metadatas,
              orgKey,
              consent.userId,
              consent)
            .map(result => {
              Json.obj(consent.userId -> result.isRight)
            })
        )
  }

  case class OfferConsentWithGroup(groupKey: String, consentKey: String)

  def initializeOffer(tenant: String, orgKey: String, offerKey: String) = authAction(sourceBodyParser) { req =>
    val setToFalse: Option[Seq[OfferConsentWithGroup]] = req.queryString
        .get("setToFalse")
        .map(strings => strings.map(string => OfferConsentWithGroup.apply _ tupled string.splitAt(string.indexOf('.'))))

    val par = req.getQueryString("par").map(_.toInt).getOrElse(2)


    val source = req.body
        .via(Framing.delimiter(ByteString("\n"), 1000000, allowTruncation = true))
        .drop(req.getQueryString("drop").map(_.toLong).getOrElse(0l))
        .map(_.utf8String.trim)
        .map(_.split(req.getQueryString("separator").getOrElse(";")).toList)
        .mapConcat {
          case id :: date :: Nil => List((id, date))
          case other =>
                Logger.error(s"Oups $other")
                List.empty
        }
        .grouped(req.getQueryString("group_by").map(_.toInt).getOrElse(0))
        .flatMapConcat(seq => handleConsent(tenant, orgKey, req.authInfo, offerKey, par, setToFalse, Source(seq)))
        .alsoTo(Sink.foreach(Json.stringify))
        .map(json => ByteString(Json.stringify(json)))
        .intersperse(ByteString("["), ByteString(","), ByteString("]"))
            .watchTermination(){(mt, d) =>
                d.onComplete {
                  case Success(_) =>
                  case Failure(exception) =>
                        Logger.error("Error processing CSV", exception)
                }
                mt
            }

    Ok.sendEntity(HttpEntity.Streamed(source, None, Some("application/json")))
  }

}
