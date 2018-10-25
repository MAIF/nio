package service

import akka.http.scaladsl.util.FastFuture
import controllers.AppErrorWithStatus
import db.{LastConsentFactMongoDataStore, OrganisationMongoDataStore}
import models.{Offer, Organisation}
import play.api.mvc.Results._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.{ExecutionContext, Future}

class OfferManagerService(
    organisationMongoDataStore: OrganisationMongoDataStore,
    lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    accessibleOfferManagerService: AccessibleOfferManagerService)(
    implicit val executionContext: ExecutionContext)
    extends ServiceUtils {

  def getAll(tenant: String,
             orgKey: String,
             offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrorWithStatus, Option[Seq[Offer]]]] = {
    accessibleOfferManagerService
      .accessibleOfferByOrganisation(tenant, orgKey, offerRestrictionPatterns)
      .map {
        case Left(_) =>
          Right(Some(Seq.empty))
        case Right(maybeOffers) =>
          Right(maybeOffers)
      }
  }

  def save(tenant: String,
           orgKey: String,
           maybeOfferKey: Option[String],
           offer: Offer,
           offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrorWithStatus, Offer]] = {

    maybeOfferKey match {
      case Some(offerKey) if offerKey != offer.key =>
        toErrorWithStatus(s"offer.key.${offer.key}.must.be.equals.to.$offerKey",
                          BadRequest)
      case Some(offerKey) if offerKey == offer.key =>
        save(tenant, orgKey, offer, offerRestrictionPatterns)
      case None =>
        save(tenant, orgKey, offer, offerRestrictionPatterns)
    }
  }

  private def save(tenant: String,
                   orgKey: String,
                   offer: Offer,
                   offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrorWithStatus, Offer]] = {
    accessibleOfferManagerService.accessibleOfferKey(
      offer.key,
      offerRestrictionPatterns) match {
      case true =>
        organisationMongoDataStore
          .findOffer(tenant, orgKey, offer.key)
          .flatMap {
            case Left(e) =>
              toErrorWithStatus(e, NotFound)
            case Right(maybeOffer) =>
              maybeOffer match {
                case Some(_) =>
                  updateOrganisation(tenant, orgKey).flatMap {
                    case Right(_) =>
                      organisationMongoDataStore
                        .updateOffer(tenant, orgKey, offer.key, offer)
                        .map(Right(_))

                    case Left(e) =>
                      toErrorWithStatus(e)
                  }
                case None =>
                  updateOrganisation(tenant, orgKey).flatMap {
                    case Right(_) =>
                      organisationMongoDataStore
                        .addOffer(tenant, orgKey, offer)
                        .map(Right(_))

                    case Left(e) =>
                      toErrorWithStatus(e)
                  }
              }
          }
      case false =>
        toErrorWithStatus(s"offer.${offer.key}.not.accessible", Unauthorized)
    }
  }

  def delete(tenant: String,
             orgKey: String,
             offerKey: String,
             offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrorWithStatus, Offer]] = {

    accessibleOfferManagerService.accessibleOfferKey(
      offerKey,
      offerRestrictionPatterns) match {
      case true =>
        updateOrganisation(tenant, orgKey).flatMap {
          case Left(e) =>
            FastFuture.successful(Left(e))
          case Right(_) =>
            organisationMongoDataStore
              .deleteOffer(tenant, orgKey, offerKey)
              .flatMap { o =>
                // clean existing consent fact
                lastConsentFactMongoDataStore
                  .removeOffer(tenant, orgKey, offerKey)
                  .map {
                    case Left(errors) =>
                      Left(errors)
                    case Right(_) =>
                      o
                  }
              }
        }
      case false =>
        toErrorWithStatus(s"offer.$offerKey.not.accessible", Unauthorized)
    }
  }

  private def updateOrganisation(tenant: String, orgKey: String)
    : Future[Either[AppErrorWithStatus, Option[Organisation]]] = {
    organisationMongoDataStore.findLastReleasedByKey(tenant, orgKey).flatMap {
      case Some(lastExistingOrganisation) =>
        val oldOrganisation = lastExistingOrganisation.copy(
          version = lastExistingOrganisation.version.copy(latest = false))
        val newOrganisation = oldOrganisation.copy(
          _id = BSONObjectID.generate().stringify,
          version = oldOrganisation.version.copy(latest = true))

        for {
          _ <- organisationMongoDataStore
            .updateById(tenant, lastExistingOrganisation._id, oldOrganisation)
          _ <- organisationMongoDataStore.insert(tenant, newOrganisation)
          lastOrganisation <- organisationMongoDataStore
            .findLastReleasedByKey(tenant, lastExistingOrganisation.key)
        } yield Right(lastOrganisation)
      case None =>
        toErrorWithStatus(s"released.of.organisation.$orgKey.not.found",
                          NotFound)
    }
  }

}
