package service

import akka.http.scaladsl.util.FastFuture
import db.OrganisationMongoDataStore
import models.Offer
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}

class AccessibleOfferManagerService(
    organisationMongoDataStore: OrganisationMongoDataStore)(
    implicit executionContext: ExecutionContext) {

  def accessibleOfferByOrganisation(
      tenant: String,
      orgKey: String,
      offerRestrictionPatterns: Option[Seq[String]])
    : Future[Either[AppErrors, Option[Seq[Offer]]]] = {
    offerRestrictionPatterns match {
      case None =>
        FastFuture.successful(Right(None))
      case Some(patterns) =>
        organisationMongoDataStore.findByKey(tenant, orgKey).map {
          case Some(organisation) =>
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
