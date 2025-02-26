package service


import org.apache.pekko.http.scaladsl.util.FastFuture
import controllers.AppErrorWithStatus
import db.OrganisationMongoDataStore
import models.{Offer, Organisation}
import utils.NioLogger
import play.api.libs.json.Json
import play.api.mvc.Results._
import utils.Result.{AppErrors, ErrorMessage}
import scala.collection.Seq

import scala.concurrent.{ExecutionContext, Future}

class AccessibleOfferManagerService(organisationMongoDataStore: OrganisationMongoDataStore)(implicit
    executionContext: ExecutionContext
) {

  def accessibleOfferKey(offerKey: String, offerRestrictionPatterns: Option[Seq[String]]): Boolean = {
    NioLogger.info(s"ask if $offerKey is accessible with pattern $offerRestrictionPatterns")
    offerRestrictionPatterns match {
      case None                => false
      case Some(offerPatterns) =>
        offerPatterns.exists {
          case "*"   => true
          case other => offerKey.matches(other)
        }
    }
  }

  def accessibleOfferByOrganisation(
      tenant: String,
      orgKey: String,
      offerRestrictionPatterns: Option[Seq[String]]
  ): Future[Either[AppErrors, Option[Seq[Offer]]]] =
    offerRestrictionPatterns match {
      case None    =>
        FastFuture.successful(Right(None))
      case Some(_) =>
        organisationMongoDataStore.findLastReleasedByKey(tenant, orgKey).map {
          case Some(organisation) =>
            NioLogger.info(s"existing offers ${organisation.offers.map(_.map(o => Json.stringify(o.asJson())))}")
            val maybeOffers: Option[Seq[Offer]] = organisation.offers.map(offers =>
              offers
                .filter(offer => accessibleOfferKey(offer.key, offerRestrictionPatterns))
            )
            Right(maybeOffers)
          case None               =>
            Left(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
        }
    }

  def organisationWithAccessibleOffer(
      tenant: String,
      orgKey: String,
      offerRestrictionPatterns: Option[Seq[String]]
  ): Future[Either[AppErrorWithStatus, Option[Organisation]]] =
    organisationMongoDataStore.findLastReleasedByKey(tenant, orgKey).map {
      case Some(organisation) =>
        NioLogger.info(s"existing offers ${organisation.offers.map(_.map(o => Json.stringify(o.asJson())))}")
        val filteredOrganisation = organisation.copy(
          offers = organisation.offers.map(offers =>
            offers
              .filter(offer => accessibleOfferKey(offer.key, offerRestrictionPatterns))
          )
        )
        Right(Some(filteredOrganisation))
      case None               =>
        Left(AppErrorWithStatus(AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))), NotFound))
    }
}
