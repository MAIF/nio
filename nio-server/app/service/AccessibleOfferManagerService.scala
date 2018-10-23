package service

import akka.http.scaladsl.util.FastFuture
import controllers.AppErrorWithStatus
import db.OrganisationMongoDataStore
import models.{Offer, Organisation}
import play.api.Logger
import play.api.libs.json.Json
import utils.Result.{AppErrors, ErrorMessage}
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}

class AccessibleOfferManagerService(
    organisationMongoDataStore: OrganisationMongoDataStore)(
    implicit executionContext: ExecutionContext) {

  def accessibleOfferKey(
      offerKey: String,
      offerRestrictionPatterns: Option[Seq[String]]): Boolean = {
    Logger.info(
      s"ask if $offerKey is accessible with pattern $offerRestrictionPatterns")
    offerRestrictionPatterns match {
      case None => false
      case Some(offerPatterns) =>
        offerPatterns.exists(p => offerKey.matches(p))
    }
  }

  def accessibleOfferByOrganisation(
      tenant: String,
      orgKey: String,
      offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrors, Option[Seq[Offer]]]] = {
    offerRestrictionPatterns match {
      case None =>
        FastFuture.successful(Right(None))
      case Some(patterns) =>
        organisationMongoDataStore.findLastReleasedByKey(tenant, orgKey).map {
          case Some(organisation) =>
            Logger.info(s"existing offers ${organisation.offers.map(_.map(o =>
              Json.stringify(o.asJson())))}")
            val maybeOffers: Option[Seq[Offer]] = organisation.offers.map(
              offers =>
                offers
                  .filter(offer => patterns.exists(p => offer.key.matches(p)))
            )
            Right(maybeOffers)
          case None =>
            Left(
              AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))))
        }
    }
  }

  def organisationWithAccessibleOffer(
      tenant: String,
      orgKey: String,
      offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrorWithStatus, Option[Organisation]]] = {
    offerRestrictionPatterns match {
      case None =>
        FastFuture.successful(Right(None))
      case Some(patterns) =>
        organisationMongoDataStore.findLastReleasedByKey(tenant, orgKey).map {
          case Some(organisation) =>
            Logger.info(s"existing offers ${organisation.offers.map(_.map(o =>
              Json.stringify(o.asJson())))}")
            val filteredOrganisation = organisation.copy(
              offers = organisation.offers.map(
                offers =>
                  offers
                    .filter(offer => patterns.exists(p => offer.key.matches(p)))
              ))
            Right(Some(filteredOrganisation))
          case None =>
            Left(
              AppErrorWithStatus(
                AppErrors(Seq(ErrorMessage(s"organisation.$orgKey.not.found"))),
                NotFound))
        }
    }
  }

  def accessibleOfferKeyByOrganisation(
      tenant: String,
      orgKey: String,
      offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrors, Option[Seq[String]]]] = {
    accessibleOfferByOrganisation(tenant, orgKey, offerRestrictionPatterns)
      .map(maybeOffers => {
        maybeOffers.right.map(_.map(_.map(_.key)))
      })
  }
//
//  def validateAccessibleOffers(tenant: String,
//                               orgKey: String,
//                               offersKeys: Seq[String],
//                               offerRestrictionPatterns: Option[Seq[String]])
//    : Future[Either[AppErrors, Seq[String]]] = {
//    accessibleOfferKeyByOrganisation(tenant, orgKey, offerRestrictionPatterns)
//      .map {
//        case Some(accessibleOffers)
//            if accessibleOffers.length == offersKeys.length && accessibleOffers.sorted == offersKeys.sorted =>
//          Right(accessibleOffers)
//
//        case Some(accessibleOffers)
//            if accessibleOffers.length != offersKeys.length || (accessibleOffers.length == offersKeys.length && accessibleOffers.sorted != offersKeys.sorted) =>
//          val notSpecified: Seq[String] = accessibleOffers.diff(offersKeys)
//          val notAllowed: Seq[String] = offersKeys.diff(accessibleOffers)
//
//          Left(
//            AppErrors(
//              toSeqErrorMessageNotAllowed(orgKey, notAllowed) ++ toSeqErrorMessageNotSpecified(
//                orgKey,
//                notSpecified)))
//
//        case None =>
//          Left(toAppErrors(orgKey, offersKeys))
//      }
//  }
//
//  private def toSeqErrorMessageNotAllowed(orgKey: String,
//                                          offers: Seq[String]) = {
//    offers.map(
//      k =>
//        ErrorMessage(
//          s"access.to.the.offer.$k.of.the.$orgKey.organisation.is.not.allowed"))
//  }
//
//  private def toSeqErrorMessageNotSpecified(orgKey: String,
//                                            offers: Seq[String]) = {
//    offers.map(k =>
//      ErrorMessage(s"offer.$k.of.the.$orgKey.organisation.is.required"))
//  }
//
//  private def toAppErrors(
//      orgKey: String,
//      offersNotAllowed: Seq[String],
//      toErrorMessage: (String, Seq[String]) => Seq[ErrorMessage] =
//        toSeqErrorMessageNotAllowed): AppErrors = {
//    AppErrors(toSeqErrorMessageNotAllowed(orgKey, offersNotAllowed))
//  }
}
