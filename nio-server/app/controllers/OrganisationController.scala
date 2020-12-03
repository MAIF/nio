package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import auth.{AuthAction, SecuredAction, SecuredAuthContext}
import db._
import models._
import messaging.KafkaMessageBroker
import utils.NioLogger
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.api.bson.BSONObjectID

import scala.concurrent.{ExecutionContext, Future}
import ErrorManager.ErrorManagerResult
import ErrorManager.AppErrorManagerResult

class OrganisationController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val ds: OrganisationMongoDataStore,
    val consentFactDataStore: ConsentFactMongoDataStore,
    val lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    val userDataStore: UserMongoDataStore,
    val tenantDataStore: TenantMongoDataStore,
    val broker: KafkaMessageBroker
)(implicit val ec: ExecutionContext, system: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val readable: ReadableEntity[Organisation] = OrganisationDraft
  implicit val materializer: Materializer             = Materializer(system)

  def create(tenant: String) = AuthAction.async(bodyParser) { implicit req =>
    tenantDataStore.findByKey(tenant).flatMap {
      case Some(_) =>
        req.body.read[Organisation] match {
          case Left(error)        =>
            NioLogger.error("Unable to parse organisation  " + error)
            Future.successful(error.badRequest())
          case Right(receivedOrg) =>
            // Exclude offers
            val o = receivedOrg.copy(version = VersionInfo())
            OrganisationValidator.validateOrganisation(o) match {
              case Left(error) =>
                NioLogger.error("Organisation is not valid  " + error)
                Future.successful(error.badRequest())
              case Right(_)    =>
                // check for duplicate key
                ds.findByKey(tenant, o.key).flatMap {
                  case None    =>
                    ds.insert(tenant, o).map { _ =>
                      broker.publish(
                        OrganisationCreated(
                          tenant = tenant,
                          payload = o,
                          author = req.authInfo.sub,
                          metadata = req.authInfo.metadatas
                        )
                      )

                      renderMethod(o, Created)
                    }
                  case Some(_) =>
                    Future.successful("error.key.already.used".conflict())
                }
            }
        }
      case None    =>
        Future.successful("error.tenant.not.found".notFound())
    }
  }

  // update if exists
  def replaceDraftIfExists(tenant: String, orgKey: String) =
    AuthAction.async(bodyParser) { implicit req =>
      req.body.read[Organisation] match {
        case Left(error)                 =>
          NioLogger.error("Unable to parse organisation  " + error)
          Future.successful(error.badRequest())
        case Right(o) if o.key != orgKey =>
          Future.successful("error.invalid.organisation.key".badRequest())
        case Right(o) if o.key == orgKey =>
          OrganisationValidator.validateOrganisation(o) match {
            case Left(error) =>
              NioLogger.error("Organisation is not valid  " + error)

              Future.successful(error.badRequest())
            case Right(_)    =>
              ds.findDraftByKey(tenant, orgKey).flatMap {
                case None                =>
                  Future.successful("error.organisation.not.found".notFound())
                case Some(previousDraft) =>
                  val newDraft =
                    o.copy(_id = previousDraft._id, version = previousDraft.version.copyUpdated)
                  ds.updateById(tenant, previousDraft._id, newDraft)
                    .map { _ =>
                      broker.publish(
                        OrganisationUpdated(
                          tenant = tenant,
                          oldValue = previousDraft,
                          payload = newDraft,
                          author = req.authInfo.sub,
                          metadata = req.authInfo.metadatas
                        )
                      )
                      renderMethod(newDraft)
                    }
              }
          }
      }
    }

  def findAllReleasedByKey(tenant: String, orgKey: String) = AuthAction.async { implicit req =>
    ds.findDraftByKey(tenant, orgKey).flatMap {
      case None    =>
        Future.successful("error.organisation.not.found".notFound())
      case Some(_) =>
        ds.findAllReleasedByKey(tenant, orgKey).map { organisations =>
          renderMethod(Organisations(organisations))
        }
    }
  }

  def findLastReleasedByKey(tenant: String, orgKey: String) = AuthAction.async { implicit req =>
    ds.findLastReleasedByKey(tenant, orgKey).map {
      case None    => "error.organisation.not.found".notFound()
      case Some(o) =>
        renderMethod(o)
    }
  }

  def findDraftByKey(tenant: String, orgKey: String) = AuthAction.async { implicit req =>
    ds.findDraftByKey(tenant, orgKey).map {
      case None      => "error.organisation.not.found".notFound()
      case Some(org) =>
        renderMethod(org)
    }
  }

  def findReleasedByKeyAndVersionNum(tenant: String, orgKey: String, version: Int) = AuthAction.async { implicit req =>
    ds.findReleasedByKeyAndVersionNum(tenant, orgKey, version).map {
      case None      => "error.organisation.not.found".notFound()
      case Some(org) =>
        renderMethod(org)
    }
  }

  def releaseDraft(tenant: String, orgKey: String) =
    AuthAction.async { implicit req =>
      ds.findDraftByKey(tenant, orgKey).flatMap {
        case Some(previousOrganisationDraft) =>
          // configure the new organisation draft (update version, change flag never released to false)
          val newOrganisationDraft: Organisation =
            previousOrganisationDraft.copy(
              version = VersionInfo(
                num = previousOrganisationDraft.version.num + 1,
                neverReleased = Some(false),
                lastUpdate = previousOrganisationDraft.version.lastUpdate
              ),
              offers = None
            )

          // configure the current organisation release (keep organisation draft version number, change status to RELEASED, update flag latest to true)
          val currentOrganisationReleased: Organisation =
            previousOrganisationDraft.copy(
              _id = BSONObjectID.generate().stringify,
              version = VersionInfo(
                num = previousOrganisationDraft.version.num,
                status = "RELEASED",
                latest = true,
                neverReleased = None
              ),
              offers = None
            )

          for {

            // get current offers for this organisation
            maybeOffers          <- ds
                                      .findOffers(tenant, orgKey)
                                      .map {
                                        case Right(offers) =>
                                          offers
                                        case Left(_)       =>
                                          None
                                      }

            // find previous released to trace into kafka
            maybePreviousRelease <- ds.findLastReleasedByKey(tenant, orgKey)

            // update current draft with new version
            _ <- ds.updateById(tenant, newOrganisationDraft._id, newOrganisationDraft)

            // add already exist offers to organisation
            organisationReleased = currentOrganisationReleased.copy(offers = maybeOffers)

            // insert release
            _ <- ds.insert(tenant, organisationReleased)

            // update flag latest on the old organisation release
            _ <- maybePreviousRelease
                   .map { previousRelease =>
                     ds.updateById(
                       tenant,
                       previousRelease._id,
                       previousRelease.copy(version = previousRelease.version.copy(latest = false))
                     )
                   }
                   .getOrElse(Future.successful(()))

            // publish new released event on kafka
            _ <- Future {
                   broker.publish(
                     OrganisationReleased(
                       tenant = tenant,
                       payload = currentOrganisationReleased,
                       author = req.authInfo.sub,
                       metadata = req.authInfo.metadatas
                     )
                   )
                 }
          } yield renderMethod(organisationReleased)

        case None =>
          Future.successful("error.organisation.not.found".notFound())
      }
    }

  def list(tenant: String) = AuthAction.async { implicit req =>
    ds.findAllLatestReleasesOrDrafts(tenant).map { orgas =>
      val lightOrgas = orgas.map(OrganisationLight.from)

      renderMethod(OrganisationsLights(lightOrgas))
    }
  }

  def delete(tenant: String, orgKey: String) = AuthAction.async { implicit req =>
    for {
      maybeLastRelease <- ds.findLastReleasedByKey(tenant, orgKey)
      maybeDraft       <- ds.findDraftByKey(tenant, orgKey)
      res              <- maybeLastRelease.orElse(maybeDraft) match {
                            case Some(org) =>
                              import cats.implicits._
                              (
                                consentFactDataStore.removeByOrgKey(tenant, orgKey),
                                lastConsentFactMongoDataStore.removeByOrgKey(tenant, orgKey),
                                userDataStore.removeByOrgKey(tenant, orgKey),
                                ds.removeByKey(tenant, orgKey)
                              ).mapN { (_, _, _, _) =>
                                broker.publish(
                                  OrganisationDeleted(
                                    tenant = tenant,
                                    payload = org,
                                    author = req.authInfo.sub,
                                    metadata = req.authInfo.metadatas
                                  )
                                )
                                Ok
                              }
                            case None      =>
                              Future.successful("error.organisation.not.found".notFound())
                          }
    } yield res
  }

  def download(tenant: String, from: String, to: String) = AuthAction.async { implicit req =>
    ds.streamAllLatestReleasesOrDraftsByDate(tenant, from, to).map { source =>
      val src = source
        .map(Json.stringify)
        .intersperse("", "\n", "\n")
        .map(ByteString.apply)
      Result(
        header = ResponseHeader(OK, Map(CONTENT_DISPOSITION -> "attachment", "filename" -> "organisations.ndjson")),
        body = HttpEntity.Streamed(src, None, Some("application/json"))
      )
    }
  }

}
