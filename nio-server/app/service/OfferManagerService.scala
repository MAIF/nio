package service

import db.OrganisationMongoDataStore
import models.Offer
import utils.Result.AppErrors

import scala.concurrent.{ExecutionContext, Future}

class OfferManagerService(
    organisationMongoDataStore: OrganisationMongoDataStore,
    accessibleOfferManagerService: AccessibleOfferManagerService)(
    implicit val executionContext: ExecutionContext) {

  def getAll(tenant: String,
             orgKey: String,
             offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrors, Option[Seq[Offer]]]] = {
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
           offer: Offer): Future[Either[AppErrors, Offer]] = {
    ???
  }

  def delete(tenant: String,
             orgKey: String,
             offer: Offer): Future[Either[AppErrors, Offer]] = {
    ???
  }

}
