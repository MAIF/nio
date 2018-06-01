package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import auth.AuthAction
import db.{
  ConsentFactMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import javax.inject.Inject
import models._
import messaging.KafkaMessageBroker
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, ResponseHeader, Result}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.{ExecutionContext, Future}

class OrganisationController @Inject()(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    val ds: OrganisationMongoDataStore,
    val consentFactDataStore: ConsentFactMongoDataStore,
    val userDataStore: UserMongoDataStore,
    val broker: KafkaMessageBroker)(implicit val ec: ExecutionContext,
                                    system: ActorSystem)
    extends ControllerUtils(cc) {

  implicit val materializer = ActorMaterializer()(system)

  def create(tenant: String) = AuthAction.async(parse.anyContent) {
    implicit req =>
      val parsed: Either[String, Organisation] =
        parseMethod(Organisation)

      parsed match {
        case Left(error) =>
          Logger.error("Unable to parse organisation  " + error)
          Future.successful(BadRequest(error))
        case Right(receivedOrg) =>
          val o = receivedOrg.copy(version = VersionInfo())
          OrganisationValidator.validateOrganisation(o) match {
            case Left(error) =>
              Logger.error("Organisation is not valid  " + error)

              Future.successful(
                BadRequest(
                  Json.obj(
                    "messages" -> error
                  )
                )
              )
            case Right(_) =>
              // check for duplicate key
              ds.findByKey(tenant, o.key).flatMap {
                case None =>
                  ds.insert(tenant, o).map { _ =>
                    broker.publish(
                      OrganisationCreated(tenant = tenant,
                                          payload = o,
                                          author = req.authInfo.sub))

                    renderMethod(o, Created)
                  }
                case Some(_) =>
                  Future.successful(Conflict("error.key.already.used"))
              }
          }
      }
  }

  // update if exists
  def replaceDraftIfExists(tenant: String, orgKey: String) =
    AuthAction.async(parse.anyContent) { implicit req =>
      val parsed: Either[String, Organisation] =
        parseMethod(Organisation)

      parsed match {
        case Left(error) =>
          Logger.error("Unable to parse organisation  " + error)
          Future.successful(BadRequest(error))
        case Right(o) if o.key != orgKey =>
          Future.successful(BadRequest("error.invalid.organisation.key"))
        case Right(o) if o.key == orgKey =>
          OrganisationValidator.validateOrganisation(o) match {
            case Left(error) =>
              Logger.error("Organisation is not valid  " + error)

              Future.successful(
                BadRequest(
                  Json.obj(
                    "messages" -> error
                  )
                )
              )
            case Right(_) =>
              ds.findDraftByKey(tenant, orgKey).flatMap {
                case None =>
                  Future.successful(NotFound("error.organisation.not.found"))
                case Some(previousDraft) =>
                  val newDraft =
                    o.copy(_id = previousDraft._id,
                           version = previousDraft.version.copyUpdated)
                  ds.updateById(tenant, previousDraft._id, newDraft)
                    .map { _ =>
                      broker.publish(
                        OrganisationUpdated(tenant = tenant,
                                            oldValue = previousDraft,
                                            payload = newDraft,
                                            author = req.authInfo.sub))
                      Ok("true")
                    }
              }
          }
      }
    }

  def findAllReleasedByKey(tenant: String, orgKey: String) = AuthAction.async {
    implicit req =>
      ds.findDraftByKey(tenant, orgKey).flatMap {
        case None => Future.successful(NotFound("error.organisation.not.found"))
        case Some(_) =>
          ds.findAllReleasedByKey(tenant, orgKey).map { organisations =>
            renderMethod(Organisations(organisations))
          }
      }
  }

  def findLastReleasedByKey(tenant: String, orgKey: String) = AuthAction.async {
    implicit req =>
      ds.findLastReleasedByKey(tenant, orgKey).map {
        case None => NotFound("error.organisation.not.found")
        case Some(o) =>
          renderMethod(o)
      }
  }

  def findDraftByKey(tenant: String, orgKey: String) = AuthAction.async {
    implicit req =>
      ds.findDraftByKey(tenant, orgKey).map {
        case None => NotFound("error.organisation.not.found")
        case Some(org) =>
          renderMethod(org)
      }
  }

  def findReleasedByKeyAndVersionNum(tenant: String,
                                     orgKey: String,
                                     version: Int) = AuthAction.async {
    implicit req =>
      ds.findReleasedByKeyAndVersionNum(tenant, orgKey, version).map {
        case None => NotFound("error.organisation.not.found")
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
              )
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
              )
            )

          for {
            // find previous released to trace into kafka
            maybePreviousRelease <- ds.findLastReleasedByKey(tenant, orgKey)

            // update current draft with new version
            _ <- ds.updateById(tenant,
                               newOrganisationDraft._id,
                               newOrganisationDraft)

            // insert release
            _ <- ds.insert(tenant, currentOrganisationReleased)

            // update flag latest on the old organisation release
            _ <- maybePreviousRelease
              .map { previousRelease =>
                ds.updateById(tenant,
                              previousRelease._id,
                              previousRelease.copy(version =
                                previousRelease.version.copy(latest = false)))
              }
              .getOrElse(Future.successful(()))

            // publish new released event on kafka
            _ <- Future {
              broker.publish(
                OrganisationReleased(tenant = tenant,
                                     payload = currentOrganisationReleased,
                                     author = req.authInfo.sub))
            }
          } yield {
            renderMethod(currentOrganisationReleased)
          }

        case None =>
          Future.successful(NotFound("error.organisation.not.found"))
      }
    }

  def list(tenant: String) = AuthAction.async { implicit req =>
    ds.findAllLatestReleasesOrDrafts(tenant).map { orgas =>
      val lightOrgas = orgas.map(OrganisationLight.from)

      renderMethod(OrganisationsLights(lightOrgas))
    }
  }

  def delete(tenant: String, orgKey: String) = AuthAction.async {
    implicit req =>
      for {
        maybeLastRelease <- ds.findLastReleasedByKey(tenant, orgKey)
        maybeDraft <- ds.findDraftByKey(tenant, orgKey)
        res <- maybeLastRelease.orElse(maybeDraft) match {
          case Some(org) =>
            import cats.implicits._
            (consentFactDataStore.removeByOrgKey(tenant, orgKey),
             userDataStore.removeByOrgKey(tenant, orgKey),
             ds.removeByKey(tenant, orgKey)).mapN { (_, _, _) =>
              broker.publish(
                OrganisationDeleted(tenant = tenant,
                                    payload = org,
                                    author = req.authInfo.sub))
              Ok("true")
            }
          case None =>
            Future.successful(NotFound("error.organisation.not.found"))
        }
      } yield {
        res
      }
  }

  def download(tenant: String, from: String, to: String) = AuthAction.async {
    implicit req =>
      ds.streamAllLatestReleasesOrDraftsByDate(tenant, from, to).map { source =>
        val src = source
          .map(Json.stringify)
          .intersperse("", "\n", "\n")
          .map(ByteString.apply)
        Result(
          header = ResponseHeader(OK,
                                  Map(CONTENT_DISPOSITION -> "attachment",
                                      "filename" -> "organisations.ndjson")),
          body = HttpEntity.Streamed(src, None, Some("application/json"))
        )
      }
  }

}
