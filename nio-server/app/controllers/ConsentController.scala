package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import auth.AuthAction
import com.codahale.metrics.{MetricRegistry, Timer}
import controllers.ErrorManager.{
  AppErrorManagerResult,
  ErrorManagerResult,
  ErrorWithStatusManagerResult
}
import db.{
  ConsentFactMongoDataStore,
  LastConsentFactMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import messaging.KafkaMessageBroker
import models.{ConsentFact, _}
import play.api.Logger
import play.api.http.HttpEntity
import play.api.mvc.{ControllerComponents, ResponseHeader, Result}
import reactivemongo.api.{Cursor, QueryOpts}
import reactivemongo.bson.BSONDocument
import service.{AccessibleOfferManagerService, ConsentManagerService}
import utils.BSONUtils
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}

class ConsentController(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    val userStore: UserMongoDataStore,
    val consentFactStore: ConsentFactMongoDataStore,
    val lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    val organisationStore: OrganisationMongoDataStore,
    consentManagerService: ConsentManagerService,
    val accessibleOfferService: AccessibleOfferManagerService,
    val metrics: MetricRegistry,
    val broker: KafkaMessageBroker)(implicit val ec: ExecutionContext,
                                    system: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[ConsentFact] = ConsentFact

  implicit val materializer = ActorMaterializer()(system)

  private lazy val timerGetConsentFact: Timer = metrics.timer("consentFact.get")
  private lazy val timerGetConsentFactTemplate: Timer =
    metrics.timer("consentFact.getTemplate")
  private lazy val timerPutConsentFact: Timer = metrics.timer("consentFact.put")

  def getTemplate(tenant: String,
                  orgKey: String,
                  maybeUserId: Option[String],
                  offerKeys: Option[Seq[String]]) =
    AuthAction.async { implicit req =>
      {
        val context: Timer.Context = timerGetConsentFactTemplate.time()

        accessibleOfferService
          .organisationWithAccessibleOffer(
            tenant,
            orgKey,
            req.authInfo.offerRestrictionPatterns)
          .flatMap {
            case Left(errors) =>
              FastFuture.successful(NotFound("")) //FIXME: use renderError instead
            case Right(maybeOrganisation) =>
              maybeOrganisation match {
                case None =>
                  Logger.error(s"Organisation $orgKey not found")
                  Future.successful("error.unknown.organisation".notFound())
                case Some(organisation) =>
                  val toConsentGroup = (pg: PermissionGroup) =>
                    ConsentGroup(
                      key = pg.key,
                      label = pg.label,
                      consents = pg.permissions.map { p =>
                        Consent(key = p.key, label = p.label, checked = false)
                      })

                  val groups = organisation.groups.map {
                    toConsentGroup(_)
                  }

                  val offers: Option[Seq[ConsentOffer]] = organisation.offers
                    .map { offers =>
                      offers
                        .map(
                          offer =>
                            ConsentOffer(
                              key = offer.key,
                              label = offer.label,
                              version = offer.version,
                              groups = offer.groups.map {
                                toConsentGroup(_)
                              }
                          ))
                    }

                  Logger.info(
                    s"Using default consents template in organisation $orgKey")

                  val template =
                    ConsentFact.template(orgVerNum = organisation.version.num,
                                         groups = groups,
                                         offers = offers,
                                         orgKey = orgKey)

                  consentManagerService
                    .mergeTemplateWithConsentFact(tenant,
                                                  orgKey,
                                                  organisation.version.num,
                                                  template,
                                                  maybeUserId)
                    .map { consentFact =>
                      context.stop()
                      renderMethod(consentFact)
                    }
              }

          }
      }
    }

  def find(tenant: String, orgKey: String, userId: String) = AuthAction.async {
    implicit req =>
      {
        val context: Timer.Context = timerGetConsentFact.time()
        lastConsentFactMongoDataStore
          .findByOrgKeyAndUserId(tenant, orgKey, userId)
          .map {
            case None =>
              // if this occurs it means a user is known but it has no consents, this is a BUG
              context.stop()
              "error.unknown.user.or.organisation".notFound()
            case Some(consentFact) =>
              // TODO: later handle here new version template version check
              val restrictedConsentFact = ConsentFact.withRestriction(
                consentFact,
                req.authInfo.offerRestrictionPatterns)

              context.stop()
              renderMethod(restrictedConsentFact)
          }
      }
  }

  def getConsentFactHistory(tenant: String,
                            orgKey: String,
                            userId: String,
                            page: Int = 0,
                            pageSize: Int = 10) =
    AuthAction.async { implicit req =>
      consentFactStore
        .findAllByUserId(tenant, userId, page, pageSize)
        .map {
          case (consentsFacts, count) =>
            val pagedConsentFacts =
              PagedConsentFacts(
                page,
                pageSize,
                count,
                consentsFacts.map(ConsentFact
                  .withRestriction(_, req.authInfo.offerRestrictionPatterns)))

            renderMethod(pagedConsentFacts)
        }
    }

  // create or replace if exists
  def createOrReplaceIfExists(tenant: String, orgKey: String, userId: String) =
    AuthAction.async(bodyParser) { implicit req =>
      req.body.read[ConsentFact] match {
        case Left(error) =>
          Logger.error(s"Unable to parse consentFact: $error")
          Future.successful(error.badRequest())
        case Right(o) if o.userId != userId =>
          Logger.error(
            s"error.userId.is.immutable : userId in path $userId // userId on body ${o.userId}")
          Future.successful("error.userId.is.immutable".badRequest())
        case Right(consentFact) if consentFact.userId == userId =>
          val cf: ConsentFact = ConsentFact.addOrgKey(consentFact, orgKey)

          (cf.offers, req.authInfo.offerRestrictionPatterns) match {
            case (Some(offers), None) =>
              val errorMessages =
                offers.map(o => ErrorMessage(s"offer.${o.key}.not.authorized"))
              Logger.error(s"not authorized : ${errorMessages.map(_.message)}")
              Future.successful("error.no.authorised.offers".unauthorized())
            case (None, _) =>
              consentManagerService
                .saveConsents(tenant,
                              req.authInfo.sub,
                              req.authInfo.metadatas,
                              orgKey,
                              userId,
                              cf)
                .map {
                  case Right(consentFactSaved) =>
                    renderMethod(consentFactSaved)
                  case Left(error) =>
                    Logger.error(
                      s"not authorized : ${error.errors.map(_.message)}")
                    error.badRequest()
                }
            case (Some(offers), Some(pattern)) =>
              offers
                .filterNot(offer => pattern.exists(p => offer.key.matches(p)))
                .toList match {
                case Nil =>
                  consentManagerService
                    .saveConsents(tenant,
                                  req.authInfo.sub,
                                  req.authInfo.metadatas,
                                  orgKey,
                                  userId,
                                  cf)
                    .map {
                      case Right(consentFactSaved) =>
                        renderMethod(consentFactSaved)
                      case Left(error) =>
                        Logger.error(
                          s"not authorized : ${error.errors.map(_.message)}")
                        error.badRequest()
                    }
                case unauthorizedOffers =>
                  val errorMessages = unauthorizedOffers.map(o =>
                    ErrorMessage(s"offer.${o.key}.not.authorized"))
                  Logger.error(
                    s"not authorized : ${errorMessages.map(_.message)}")
                  FastFuture.successful(AppErrors(errorMessages).unauthorized())
              }
          }
      }
    }

  lazy val defaultPageSize: Int =
    sys.env.get("DEFAULT_PAGE_SIZE").map(_.toInt).getOrElse(200)
  lazy val defaultParSize: Int =
    sys.env.get("DEFAULT_PAR_SIZE").map(_.toInt).getOrElse(6)

  def download(tenant: String) = AuthAction { implicit req =>
    val src = lastConsentFactMongoDataStore
      .streamAllBSON(
        tenant,
        req.getQueryString("pageSize").map(_.toInt).getOrElse(defaultPageSize),
        req.getQueryString("par").map(_.toInt).getOrElse(defaultParSize)
      )
      .intersperse(ByteString.empty, ByteString("\n"), ByteString("\n"))

    Result(
      header = ResponseHeader(OK,
                              Map(CONTENT_DISPOSITION -> "attachment",
                                  "filename" -> "consents.ndjson")),
      body = HttpEntity.Streamed(src, None, Some("application/json"))
    )
  }

  def downloadBulked(tenant: String) = AuthAction { implicit req =>
    Logger.info(
      s"Downloading consents (using bulked reads) from tenant $tenant")
    import reactivemongo.akkastream.cursorProducer

    val src = Source
      .fromFutureSource {
        lastConsentFactMongoDataStore.storedBSONCollection(tenant).map { col =>
          col
            .find(reactivemongo.bson.BSONDocument())
            .options(QueryOpts(batchSizeN = 300))
            .cursor[BSONDocument]()
            .bulkSource(err = Cursor.FailOnError((_, e) =>
              Logger.error(s"Error while streaming worker", e)))
        }
      }
      .mapAsync(10) { d =>
        Future {
          val bld = new StringBuilder()
          d.foreach { doc =>
            bld.append(s"${BSONUtils.stringify(doc)}\n")
          }
          ByteString(bld.toString())
        }
      }
    Result(
      header = ResponseHeader(OK,
                              Map(CONTENT_DISPOSITION -> "attachment",
                                  "filename" -> "consents.ndjson")),
      body = HttpEntity.Streamed(src, None, Some("application/json"))
    )
  }

  /**
    * DELETE /api/:tenant/organisations/:orgKey/users/:userId/offers/:offerKey
    * description : suppression d’une offre dans les consentements d’un utilisateur donné.
    * La suppression d’une offre doit être tracé afin qu’elle ne soit plus proposée par la suite.
    * erreur :
    * code 404 : le tenant, l’organisation, l’utilisateur ou l’offre n’existe pas.
    * code 404 : l’offre n’est pas accessible par le contexte utilisateur
    */
  def deleteOffer(tenant: String,
                  orgKey: String,
                  userId: String,
                  offerKey: String) = AuthAction.async { implicit req =>
    lastConsentFactMongoDataStore
      .findByOrgKeyAndUserId(tenant, orgKey, userId)
      .flatMap {
        case None =>
          // if this occurs it means a user is known but it has no consents, this is a BUG
          Future.successful("error.unknown.user.or.organisation".notFound())
        case Some(consentFact) =>
          consentFact.offers.flatMap(offers =>
            offers.find(o => o.key == offerKey)) match {
            case None =>
              Logger.error(s"offer $offerKey not found")
              Future.successful("error.offer.not.found".notFound())
            case Some(offer) =>
              consentManagerService
                .delete(tenant, orgKey, userId, offer.key)
                .flatMap {
                  case Left(e) =>
                    Future.successful(e.renderError())
                  case Right(o) =>
                    Future.successful(renderMethod(o))
                }
          }
      }
  }
}
