package service

import akka.http.scaladsl.util.FastFuture
import cats.data.OptionT
import controllers.AppErrorWithStatus
import db.{
  ConsentFactMongoDataStore,
  LastConsentFactMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}

class ConsentManagerService(
    lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    consentFactMongoDataStore: ConsentFactMongoDataStore,
    userMongoDataStore: UserMongoDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    kafkaMessageBroker: KafkaMessageBroker)(
    implicit val executionContext: ExecutionContext) {

  def createOrReplace(tenant: String,
                      author: String,
                      metadata: Option[Seq[(String, String)]],
                      organisation: Organisation,
                      consentFact: ConsentFact,
                      maybeLastConsentFact: Option[ConsentFact] = None)
    : Future[Either[AppErrors, ConsentFact]] = {
    organisation.isValidWith(consentFact) match {
      case Some(error) =>
        Logger.error(
          s"invalid consent fact (compare with organisation ${organisation.key} version ${organisation.version}) : $error || ${consentFact.asJson}")
        FastFuture.successful(Left(AppErrors.error(error)))
      case None =>
        val organisationKey: String = organisation.key
        val userId: String = consentFact.userId

        maybeLastConsentFact match {
          // Create a new user, consent fact and last consent fact
          case None =>
            for {
              _ <- consentFactMongoDataStore.insert(tenant, consentFact)
              _ <- lastConsentFactMongoDataStore.insert(tenant, consentFact)
              _ <- userMongoDataStore.insert(
                tenant,
                User(
                  userId = userId,
                  orgKey = organisationKey,
                  orgVersion = organisation.version.num,
                  latestConsentFactId = consentFact._id
                ))
              _ <- FastFuture.successful(
                kafkaMessageBroker.publish(
                  ConsentFactCreated(
                    tenant = tenant,
                    payload = consentFact,
                    author = author,
                    metadata = metadata
                  )
                )
              )
            } yield Right(consentFact)
          // Update user, consent fact and last consent fact
          case Some(lastConsentFactStored)
              if consentFact.version >= lastConsentFactStored.version =>
            val lastConsentFactToStore =
              consentFact.copy(lastConsentFactStored._id)
            for {
              _ <- lastConsentFactMongoDataStore.update(tenant,
                                                        organisationKey,
                                                        userId,
                                                        lastConsentFactToStore)
              _ <- consentFactMongoDataStore.insert(tenant, consentFact)
              _ <- FastFuture.successful(
                kafkaMessageBroker.publish(
                  ConsentFactUpdated(
                    tenant = tenant,
                    oldValue = lastConsentFactStored,
                    payload = lastConsentFactToStore,
                    author = author,
                    metadata = metadata
                  )
                )
              )
            } yield Right(consentFact)

          case Some(lastConsentFactStored) =>
            Logger.error(
              s"lastConsentFactStored.version > consentFact.version (${lastConsentFactStored.version} > ${consentFact.version}) for ${lastConsentFactStored._id}")
            FastFuture.successful(Left(AppErrors.error(
              "invalid.lastConsentFactStored.version.sup.consentFact.version")))
        }
    }
  }

  def testOfferStrucureWithLastConsent(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer): Future[Either[AppErrors, ConsentOffer]] =
    lastConsentFactMongoDataStore
      .findConsentOffer(tenant, orgKey, userId, offerToCompare.key)
      .flatMap {
        case Left(errors) => FastFuture.successful(Left(errors))
        case Right(None) =>
          Logger.error(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
          FastFuture.successful(Left(AppErrors(Seq(ErrorMessage(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")))))
        case Right(Some(offer)) if offer.version != offerToCompare.version =>
          Logger.error(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.not.equal.to.${offer.version}")
          FastFuture.successful(Left(AppErrors(Seq(ErrorMessage(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")))))
        case Right(Some(lastConsentOffer)) =>
          if (offerToCompare.label == lastConsentOffer.label &&
              offerToCompare.version == lastConsentOffer.version &&
              lastConsentOffer.groups.forall(lastConsentGroup =>
                offerToCompare.groups
                  .find(c => c.key == lastConsentGroup.key)
                  .exists(consentGroup =>
                    consentGroup.key == lastConsentGroup.key &&
                      consentGroup.label == lastConsentGroup.label &&
                      consentGroup.consents.forall(c =>
                        lastConsentGroup.consents
                          .find(consent => consent.key == c.key)
                          .exists(consent =>
                            c.key == consent.key && c.label == consent.label)))))
            FastFuture.successful(Right(offerToCompare))
          else {
            Logger.error(
              s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsent")
            FastFuture.successful(Left(AppErrors(Seq(ErrorMessage(
              s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsentfact")))))
          }
      }

  //Fixme: on a deja fait l'appel a organisationMongoDataStore.findLastReleasedByKey
  def testOfferStructureWithOrganisation(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer): Future[Either[AppErrors, ConsentOffer]] =
    organisationMongoDataStore
      .findOffer(tenant, orgKey, offerToCompare.key)
      .flatMap {
        case Left(error) => FastFuture.successful(Left(error))
        case Right(None) =>
          FastFuture.successful(
            Left(AppErrors(
              Seq(ErrorMessage(s"offer.${offerToCompare.key}.not.found")))))
        case Right(Some(offer)) if offerToCompare.version > offer.version =>
          Logger.error(
            s"offer.${offerToCompare.key}.version.${offerToCompare.version} > ${offer.version}")
          FastFuture.successful(Left(AppErrors(Seq(ErrorMessage(
            s"offer.${offerToCompare.key}.version.${offerToCompare.version}.unavailable")))))
        case Right(Some(permissionOffer))
            if offerToCompare.version < permissionOffer.version =>
          testOfferStrucureWithLastConsent(tenant,
                                           orgKey,
                                           userId,
                                           offerToCompare)
        case Right(Some(permissionOffer))
            if offerToCompare.version == permissionOffer.version =>
          if (offerToCompare.label == permissionOffer.label &&
              offerToCompare.version == permissionOffer.version &&
              permissionOffer.groups.forall(
                permissionGroup =>
                  offerToCompare.groups
                    .find(c => c.key == permissionGroup.key)
                    .exists(consents =>
                      consents.key == permissionGroup.key &&
                        consents.label == permissionGroup.label &&
                        consents.consents.forall(c =>
                          permissionGroup.permissions
                            .find(permission => permission.key == c.key)
                            .exists(permission =>
                              c.key == permission.key && c.label == permission.label)))))
            FastFuture.successful(Right(offerToCompare))
          else {
            Logger.error(
              s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")
            FastFuture.successful(Left(AppErrors(Seq(ErrorMessage(
              s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")))))
          }
      }

  def testOffersStructure(
      tenant: String,
      orgKey: String,
      userId: String,
      maybeConsentOffersToCompare: Option[Seq[ConsentOffer]])
    : Future[Either[AppErrors, Option[Seq[ConsentOffer]]]] =
    maybeConsentOffersToCompare match {
      case None =>
        FastFuture.successful(Right(None))
      case Some(offersToCompare) =>
        Future
          .sequence {
            offersToCompare.map(
              testOfferStructureWithOrganisation(tenant, orgKey, userId, _))
          }
          .map(sequence)
          .map {
            case Left(errors) => Left(AppErrors(errors.flatMap(x => x.errors)))
            case Right(_)     => Right(Some(offersToCompare))
          }
    }

  def sequence[A, B](s: Seq[Either[A, B]]): Either[Seq[A], B] =
    s.foldLeft(Left(Nil): Either[List[A], B]) { (acc, e) =>
      for (xs <- acc.left; x <- e.left) yield x :: xs
    }

  def saveConsents(
      tenant: String,
      author: String,
      metadata: Option[Seq[(String, String)]],
      organisationKey: String,
      userId: String,
      consentFact: ConsentFact): Future[Either[AppErrors, ConsentFact]] = {
    lastConsentFactMongoDataStore
      .findByOrgKeyAndUserId(tenant, organisationKey, userId)
      .flatMap {
        // Insert a new user, last consent fact, historic consent fact
        case None =>
          organisationMongoDataStore
            .findLastReleasedByKey(tenant, organisationKey)
            .flatMap {
              case None =>
                Logger.error(
                  s"error.specified.org.never.released for organisation key $organisationKey")
                FastFuture.successful(
                  Left(AppErrors.error("error.specified.org.never.released")))

              case Some(organisation)
                  if organisation.version.num != consentFact.version =>
                Logger.error(
                  s"error.specified.version.not.latest : latest version ${organisation.version.num} -> version specified ${consentFact.version}")
                FastFuture.successful(
                  Left(AppErrors.error("error.specified.version.not.latest")))

              case Some(organisation)
                  if organisation.version.num == consentFact.version =>
                testOffersStructure(tenant,
                                    organisationKey,
                                    userId,
                                    consentFact.offers).flatMap {
                  case Left(error) => FastFuture.successful(Left(error))
                  case Right(_) =>
                    createOrReplace(tenant,
                                    author,
                                    metadata,
                                    organisation,
                                    consentFact)
                }
            }

        // Update consent fact with the same organisation version
        case Some(lastConsentFactStored)
            if lastConsentFactStored.version == consentFact.version =>
          organisationMongoDataStore
            .findReleasedByKeyAndVersionNum(tenant,
                                            organisationKey,
                                            lastConsentFactStored.version)
            .flatMap {
              case Some(organisation) =>
                testOffersStructure(tenant,
                                    organisationKey,
                                    userId,
                                    consentFact.offers).flatMap {
                  case Left(error) => FastFuture.successful(Left(error))
                  case Right(_) =>
                    createOrReplace(tenant,
                                    author,
                                    metadata,
                                    organisation,
                                    consentFact,
                                    Some(lastConsentFactStored))
                }
              case None =>
                Logger.error(
                  s"error.unknow.specified.version : version specified ${lastConsentFactStored.version}")
                FastFuture.successful(
                  Left(AppErrors.error("error.unknow.specified.version")))
            }

        // Update consent fact with the new organisation version
        case Some(lastConsentFactStored)
            if lastConsentFactStored.version <= consentFact.version =>
          organisationMongoDataStore
            .findLastReleasedByKey(tenant, organisationKey)
            .flatMap {
              case None =>
                Logger.error(
                  s"error.specified.org.never.released for organisation key $organisationKey")
                FastFuture.successful(
                  Left(AppErrors.error("error.specified.org.never.released")))

              case Some(organisation)
                  if organisation.version.num < consentFact.version =>
                Logger.error(
                  s"error.version.higher.than.release : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}")
                FastFuture.successful(
                  Left(AppErrors.error("error.version.higher.than.release")))

              case Some(organisation) =>
                createOrReplace(tenant,
                                author,
                                metadata,
                                organisation,
                                consentFact,
                                Option(lastConsentFactStored))
            }

        // Cannot rollback and update a consent fact to an old organisation version
        case Some(lastConsentFactStored)
            if lastConsentFactStored.version >= consentFact.version =>
          Logger.error(
            s"error.version.lower.than.stored : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}")
          FastFuture.successful(
            Left(AppErrors.error("error.version.lower.than.stored")))
      }
  }

  def mergeTemplateWithConsentFact(
      tenant: String,
      orgKey: String,
      orgVersion: Int,
      template: ConsentFact,
      maybeUserId: Option[String]): Future[ConsentFact] = {

    import cats.implicits._

    // format: off
    val res: OptionT[Future, ConsentFact] = for {
      userId: String <- OptionT.fromOption[Future](maybeUserId)
      _ = Logger.info(s"userId is defined with $userId")
      consentFact <- OptionT(lastConsentFactMongoDataStore.findByOrgKeyAndUserId(tenant, orgKey, userId))
      built <- OptionT.pure[Future](buildTemplate(orgKey, orgVersion, template, consentFact, userId))
    } yield built
    // format: on
    res.getOrElse(template)
  }

  private def buildTemplate(orgKey: String,
                            orgVersion: Int,
                            template: ConsentFact,
                            consentFact: ConsentFact,
                            userId: String) = {
    Logger.info(s"consent fact exist")

    val mergeConsentGroup =
      (maybeGroup: Option[ConsentGroup], group: ConsentGroup) =>
        maybeGroup
          .map { consentGroup =>
            val groups: Seq[Consent] = group.consents.map { consent =>
              val maybeConsent =
                consentGroup.consents.find(c =>
                  c.key == consent.key && c.label == consent.label)
              maybeConsent match {
                case Some(consentValue) =>
                  consent.copy(checked = consentValue.checked)
                case None =>
                  consent
              }
            }
            group.copy(consents = groups)
          }
          .getOrElse(group)

    val groupsUpdated: Seq[ConsentGroup] =
      template.groups.map(
        group => {
          val maybeGroup = consentFact.groups.find(cg =>
            cg.key == group.key && cg.label == group.label)

          mergeConsentGroup(maybeGroup, group)
        }
      )

    val offersUpdated: Option[Seq[ConsentOffer]] =
      template.offers.map(
        offers =>
          offers.map { offer =>
            val maybeOffer = consentFact.offers
              .getOrElse(Seq.empty)
              .find(co => co.key == offer.key)

            maybeOffer
              .map { cOffer =>
                val groups: Seq[ConsentGroup] = offer.groups.map(
                  group => {
                    val maybeGroup = cOffer.groups.find(cg =>
                      cg.key == group.key && cg.label == group.label)

                    mergeConsentGroup(maybeGroup, group)
                  }
                )
                offer.copy(groups = groups)
              }
              .getOrElse(offer)
        }
      )

    ConsentFact
      .template(orgVerNum = orgVersion,
                groups = groupsUpdated,
                offers = offersUpdated,
                orgKey = orgKey)
      .copy(userId = userId)
  }

  def delete(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String): Future[Either[AppErrorWithStatus, ConsentOffer]] = {
    lastConsentFactMongoDataStore.removeOfferById(tenant,
                                                  orgKey,
                                                  userId,
                                                  offerKey)
  }
}
