package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import auth.SecuredAuthContext
import controllers.ErrorManager.{AppErrorManagerResult, ErrorManagerResult, ErrorWithStatusManagerResult}
import db.{ConsentFactMongoDataStore, LastConsentFactMongoDataStore, OrganisationMongoDataStore, UserMongoDataStore}
import libs.io.IO
import libs.io._
import libs.xmlorjson.XmlOrJson
import messaging.KafkaMessageBroker
import models.{ConsentFact, _}
import utils.NioLogger
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.api.Cursor
import reactivemongo.api.bson.BSONDocument
import service.{AccessibleOfferManagerService, ConsentManagerService}
import utils.BSONUtils
import utils.Result.{AppErrors, ErrorMessage}

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

class ConsentController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val userStore: UserMongoDataStore,
    val consentFactStore: ConsentFactMongoDataStore,
    val lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    val organisationStore: OrganisationMongoDataStore,
    consentManagerService: ConsentManagerService,
    val accessibleOfferService: AccessibleOfferManagerService,
    val broker: KafkaMessageBroker
)(implicit val ec: ExecutionContext, system: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[ConsentFact] = ConsentFact
  implicit val readablePartial: ReadableEntity[PartialConsentFact] = PartialConsentFact

  implicit val materializer = Materializer(system)

  def getTemplate(tenant: String, orgKey: String, maybeUserId: Option[String], maybeOfferKeys: Option[Seq[String]]) =
    AuthAction.async { implicit req =>
      import cats.data._
      import cats.implicits._

      EitherT(
        getConsentFactTemplate(tenant, orgKey, maybeUserId, maybeOfferKeys, req.authInfo.offerRestrictionPatterns)
      )
        .fold(error => error.renderError(), consentFact => renderMethod(consentFact))

    }

  def getConsentFactTemplate(
      tenant: String,
      orgKey: String,
      maybeUserId: Option[String],
      maybeOfferKeys: Option[Seq[String]],
      offerRestrictionPatterns: Option[Seq[String]]
  ): Future[Either[AppErrorWithStatus, ConsentFact]] =
    accessibleOfferService
      .organisationWithAccessibleOffer(tenant, orgKey, offerRestrictionPatterns)
      .flatMap {
        case Left(errors)             =>
          FastFuture.successful(Left(errors))
        case Right(maybeOrganisation) =>
          maybeOrganisation match {
            case None               =>
              NioLogger.error(s"Organisation $orgKey not found")
              Future.successful(
                Left(
                  AppErrorWithStatus(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))), Results.NotFound)
                )
              )
            case Some(organisation) =>
              val toConsentGroup = (pg: PermissionGroup) =>
                ConsentGroup(
                  key = pg.key,
                  label = pg.label,
                  consents = pg.permissions.map { p =>
                    Consent(key = p.key, label = p.label, checked = p.checkDefault())
                  }
                )

              val groups = organisation.groups.map {
                toConsentGroup(_)
              }

              val offers: Option[Seq[ConsentOffer]] =
                organisation.offers.map(
                  _.filter(o =>
                    maybeOfferKeys match {
                      case Some(offerKeys) if offerKeys.nonEmpty =>
                        offerKeys.contains(o.key)
                      case _                                     =>
                        true
                    }
                  ).map(offer =>
                    ConsentOffer(
                      key = offer.key,
                      label = offer.label,
                      version = offer.version,
                      groups = offer.groups.map {
                        toConsentGroup(_)
                      }
                    )
                  )
                )

              val template =
                ConsentFact.template(
                  orgVerNum = organisation.version.num,
                  groups = groups,
                  offers = offers,
                  orgKey = orgKey
                )

              consentManagerService
                .mergeTemplateWithConsentFact(tenant, orgKey, organisation.version.num, template, maybeUserId)
                .map(Right(_))
          }

      }

  def find(tenant: String, orgKey: String, userId: String) = AuthAction.async { implicit req =>
    import cats.data._
    import cats.implicits._

    EitherT(findConsentFacts(tenant, orgKey, userId, req.authInfo.offerRestrictionPatterns))
      .fold(error => error.renderError(), consentFact => renderMethod(consentFact))
  }

  def findConsentFacts(
      tenant: String,
      orgKey: String,
      userId: String,
      offerRestrictionPatterns: Option[Seq[String]]
  ): Future[Either[AppErrorWithStatus, ConsentFact]] =
    lastConsentFactMongoDataStore
      .findByOrgKeyAndUserId(tenant, orgKey, userId)
      .map {
        case None              =>
          Left(
            AppErrorWithStatus(
              AppErrors(Seq(ErrorMessage(s"error.unknown.user.$userId.or.organisation.$orgKey"))),
              Results.NotFound
            )
          )
        case Some(consentFact) =>
          // TODO: later handle here new version template version check
          Right(consentManagerService.consentFactWithAccessibleOffers(consentFact, offerRestrictionPatterns))
      }

  def getConsentFactHistory(tenant: String, orgKey: String, userId: String, page: Int = 0, pageSize: Int = 10) =
    AuthAction.async { implicit req =>
      consentFactStore
        .findAllByUserId(tenant, userId, page, pageSize)
        .map { case (consentsFacts, count) =>
          val pagedConsentFacts =
            PagedConsentFacts(
              page,
              pageSize,
              count,
              consentsFacts
                .map(consentManagerService.consentFactWithAccessibleOffers(_, req.authInfo.offerRestrictionPatterns))
            )

          renderMethod(pagedConsentFacts)
        }
    }

  // create or replace if exists
  def createOrReplaceIfExists(tenant: String, orgKey: String, userId: String): Action[XmlOrJson] =
    AuthAction.async(bodyParser) { implicit req =>
      req.body.read[ConsentFact] match {
        case Left(error)                                        =>
          NioLogger.error(s"Unable to parse consentFact: $error")
          Future.successful(error.badRequest())
        case Right(o) if o.userId != userId                     =>
          NioLogger.error(s"error.userId.is.immutable : userId in path $userId // userId on body ${o.userId}")
          Future.successful("error.userId.is.immutable".badRequest())
        case Right(consentFact) if consentFact.userId == userId =>
          val cf: ConsentFact = ConsentFact.addOrgKey(consentFact, orgKey)

          (cf.offers, req.authInfo.offerRestrictionPatterns) match {
            // case ask create or update offers but no pattern allowed
            case (Some(offers), None)          =>
              val errorMessages =
                offers.map(o => ErrorMessage(s"offer.${o.key}.not.authorized"))
              NioLogger.error(s"not authorized : ${errorMessages.map(_.message)}")
              Future.successful(AppErrors(errorMessages).unauthorized())

            // case create or update consents without offers
            case (None, _)                     =>
              consentManagerService
                .saveConsents(tenant, req.authInfo.sub, req.authInfo.metadatas, orgKey, userId, cf, Json.toJson(cf))
                .fold (
                  error => {
                    NioLogger.error(s"error during consent fact saving $error")
                    error.renderError()
                  },
                  consentFactSaved => renderMethod(consentFactSaved)
                )
            // case create or update offers and some patterns are specified
            case (Some(offers), Some(_)) =>
              // validate offers key are accessible
              offers
                .filterNot(o =>
                  accessibleOfferService.accessibleOfferKey(o.key, req.authInfo.offerRestrictionPatterns)
                ) match {
                // case all offers in consent (body) are accessible
                case Nil                =>
                  consentManagerService
                    .saveConsents(tenant, req.authInfo.sub, req.authInfo.metadatas, orgKey, userId, cf, Json.toJson(cf))
                    .fold(
                      error => {
                        NioLogger.error(s"error during consent fact saving $error")
                        error.renderError()
                      },
                      consentFactSaved => renderMethod(consentFactSaved)
                    )

                // case one or more offers are not accessible
                case unauthorizedOffers =>
                  val errorMessages = unauthorizedOffers.map(o => ErrorMessage(s"offer.${o.key}.not.authorized"))
                  NioLogger.error(s"not authorized : ${errorMessages.map(_.message)}")
                  FastFuture.successful(AppErrors(errorMessages).unauthorized())
              }
          }
      }
    }

  def partialUpdate(tenant: String, orgKey: String, userId: String): Action[XmlOrJson]=
    AuthAction.async(bodyParser) { implicit req =>
      (for {
        patchCommand      <- IO.fromEither(req.body.read[PartialConsentFact]).mapError { error => NioLogger.error(s"Unable to parse consentFact: $error") ; error.badRequest() }
        _                 <- if (patchCommand.userId.isDefined && !patchCommand.userId.contains(userId)) IO.error("error.userId.is.immutable".badRequest())
                             else IO.succeed(patchCommand)
        command           = patchCommand.copy(orgKey = Some(orgKey))
        result            <- patchCommand.offers match {
          case Some(offers) =>
            for {
              _ <- IO.fromOption(req.authInfo.offerRestrictionPatterns, { val errorMessages = offers.map(o => ErrorMessage(s"offer.${o.key}.not.authorized")) ; NioLogger.error(s"not authorized : ${errorMessages.map(_.message)}"); AppErrors(errorMessages).unauthorized()})
              _ <- IO.succeed[Result](offers.filterNot(o => accessibleOfferService.accessibleOfferKey(o.key, req.authInfo.offerRestrictionPatterns)))
                .keep(offer => offer.isEmpty, { unauthorizedOffers =>
                  val errorMessages = unauthorizedOffers.map(o => ErrorMessage(s"offer.${o.key}.not.authorized"))
                  NioLogger.error(s"not authorized : ${errorMessages.map(_.message)}")
                  AppErrors(errorMessages).unauthorized()
                })
              consentFactSaved <- consentManagerService
                .partialUpdate(tenant, req.authInfo.sub, req.authInfo.metadatas, orgKey, userId, command, Json.toJson(patchCommand))
                .mapError { error =>
                    NioLogger.error(s"error during consent fact saving $error")
                    error.renderError()
                }
            } yield renderMethod(consentFactSaved)
          case None         =>
            consentManagerService
              .partialUpdate(tenant, req.authInfo.sub, req.authInfo.metadatas, orgKey, userId, command, Json.toJson(patchCommand))
              .mapError { _.renderError() }
              .map { consentFactSaved => renderMethod(consentFactSaved) }
        }
      } yield result).merge
  }

  lazy val defaultPageSize: Int =
    sys.env.get("DEFAULT_PAGE_SIZE").map(_.toInt).getOrElse(200)
  lazy val defaultParSize: Int  =
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
      header = ResponseHeader(OK, Map(CONTENT_DISPOSITION -> "attachment", "filename" -> "consents.ndjson")),
      body = HttpEntity.Streamed(src, None, Some("application/json"))
    )
  }

  def downloadBulked(tenant: String) = AuthAction { implicit req =>
    NioLogger.info(s"Downloading consents (using bulked reads) from tenant $tenant")
    import reactivemongo.akkastream.cursorProducer

    val src = Source
      .futureSource {
        lastConsentFactMongoDataStore.storedBSONCollection(tenant).map { col =>
          col
            .find(reactivemongo.api.bson.BSONDocument())
            .cursor[BSONDocument]()
            .bulkSource(
              maxDocs = 300,
              err = Cursor.FailOnError((_, e) => NioLogger.error(s"Error while streaming worker", e))
            )
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
      header = ResponseHeader(OK, Map(CONTENT_DISPOSITION -> "attachment", "filename" -> "consents.ndjson")),
      body = HttpEntity.Streamed(src, None, Some("application/json"))
    )
  }

  def deleteOffer(tenant: String, orgKey: String, userId: String, offerKey: String) = AuthAction.async { implicit req =>
    req.authInfo.offerRestrictionPatterns match {
      case Some(_) if !accessibleOfferService.accessibleOfferKey(offerKey, req.authInfo.offerRestrictionPatterns) =>
        NioLogger.error(s"offer $offerKey unauthorized")
        Future.successful(s"error.offer.$offerKey.unauthorized".unauthorized())
      case Some(_)                                                                                                =>
        lastConsentFactMongoDataStore
          .findByOrgKeyAndUserId(tenant, orgKey, userId)
          .flatMap {
            case None              =>
              // if this occurs it means a user is known but it has no consents, this is a BUG
              Future.successful(
                s"error.unknown.user.$userId.or.organisation.$orgKey"
                  .notFound()
              )
            case Some(consentFact) =>
              consentFact.offers.flatMap(offers => offers.find(o => o.key == offerKey)) match {
                case None        =>
                  NioLogger.error(s"offer $offerKey not found")
                  Future.successful(s"error.offer.$offerKey.not.found".notFound())
                case Some(offer) =>
                  consentManagerService
                    .delete(tenant, orgKey, userId, offer.key, req.authInfo.sub, req.authInfo.metadatas, consentFact)
                    .fold(
                      e => e.renderError(),
                      o => renderMethod(o)
                    )
              }
          }
    }
  }
}
