package controllers

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString
import auth.{AuthInfo, SecuredAuthContext}
import configuration.Env
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult, ErrorWithStatusManagerResult}
import db.OrganisationMongoDataStore
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.Reads._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.{ActionBuilder, AnyContent, ControllerComponents}
import reactivemongo.bson.BSONObjectID
import service.{ConsentManagerService, OfferManagerService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class OrganisationOfferController(
	val env: Env,
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

  case class OfferConsentWithGroup(groupKey: String, consentKey: String)
  case class UserIdAndInitDate(id: String, date: String)

  def handleConsent(tenant: String, orgKey: String, authInfo: AuthInfo, offerKey: String, setToFalse: Option[Seq[OfferConsentWithGroup]], source: Source[UserIdAndInitDate, _]): Source[JsValue, _]= {
    source
      .mapAsync(env.config.dbConfig.batchSize) {
		  value =>
			  consentController.getConsentFactTemplate(tenant, orgKey, Some(value.id), authInfo.offerRestrictionPatterns)
				  .collect {
					  case Right(cf) =>
						  val consentOffers: Seq[ConsentOffer] = cf.offers match {
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
														  .copy(checked = setToFalse.forall(groupAndConsents => !groupAndConsents.exists(groupAndConsent => groupAndConsent.groupKey == group.key && groupAndConsent.consentKey == consent.key)))))))
									  val date = DateTime.parse(value.date)
									  offers.filter(off => off.key != offerKey) ++ Seq(offer.copy(lastUpdate = date))
							  }
						  }

						  cf.copy(_id = BSONObjectID.generate().stringify, lastUpdateSystem = DateTime.now(), userId = value.id, doneBy = DoneBy(authInfo.sub, "admin"), offers = Some(consentOffers))
				  }
	  }
        .mapAsync(env.config.dbConfig.batchSize)(consent => consentManagerService
            .saveConsents(tenant,
              authInfo.sub,
              authInfo.metadatas,
              orgKey,
              consent.userId,
              consent)
            .map(result => {
              Json.obj("userId" -> consent.userId, "status" -> result.isRight)
            })
        )
  }

	val newLineSplit: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString("\n"), 10000, allowTruncation = true)
	def jsonToIdAndDate: Flow[ByteString, UserIdAndInitDate, NotUsed] =
		Flow[ByteString] via newLineSplit map (_.utf8String) filterNot (_.isEmpty) map (l => Json.parse(l)) map (value => UserIdAndInitDate((value \ "id").as[String], (value \ "date").as[String]))

	def csvToIdAndDate(drop: Long, separator: String): Flow[ByteString, UserIdAndInitDate, NotUsed] =
		Flow[ByteString]
			.via(newLineSplit)
    		.drop(drop)
			.map(_.utf8String.trim)
			.map(_.split(separator).toList)
			.mapConcat {
				case id :: date :: Nil => List(UserIdAndInitDate(id, date))
				case other =>
					Logger.error(s"Oups $other")
					List.empty
			}

	val sourceBodyParser: BodyParser[Source[UserIdAndInitDate, _]] =
		BodyParser("Streaming BodyParser") { req =>
			val drop = req.getQueryString("drop").map(_.toLong).getOrElse(0l)
			val separator = req.getQueryString("separator").getOrElse(";")

			req.contentType match {
				case Some("application/json") => Accumulator.source[ByteString].map(s => Right(s.via(jsonToIdAndDate)))
				case Some("application/csv") => Accumulator.source[ByteString].map(s => Right(s.via(csvToIdAndDate(drop, separator))))
				case _ => Accumulator.source[ByteString].map(_ => Left(UnsupportedMediaType))
			}
		}

  def initializeOffer(tenant: String, orgKey: String, offerKey: String) = authAction(sourceBodyParser) { req =>
    val setToFalse: Option[Seq[OfferConsentWithGroup]] = req.queryString
        .get("setToFalse")
        .map(strings => strings.map(string => {
			val index = string.indexOf('.')
			OfferConsentWithGroup.apply _ tupled (string.take(index), string.drop(index + 1))
		}))

    val source = req.body
        .flatMapConcat(seq => handleConsent(tenant, orgKey, req.authInfo, offerKey, setToFalse, Source(seq)))
        .alsoTo(Sink.foreach(Json.stringify))
        .map(json => ByteString(Json.stringify(json)))
        .intersperse(ByteString("["), ByteString(","), ByteString("]"))
            .watchTermination(){(mt, d) =>
                d.onComplete {
                  case Success(_) =>
                  case Failure(exception) =>
                        Logger.error("Error processing stream", exception)
                }
                mt
            }

    Ok.sendEntity(HttpEntity.Streamed(source, None, Some("application/json")))
  }

}
