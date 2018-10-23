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
import play.api.mvc.Results._
import reactivemongo.bson.BSONObjectID

class ConsentManagerService(
    lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    consentFactMongoDataStore: ConsentFactMongoDataStore,
    userMongoDataStore: UserMongoDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    accessibleOfferService: AccessibleOfferManagerService,
    kafkaMessageBroker: KafkaMessageBroker)(
    implicit val executionContext: ExecutionContext)
    extends ServiceUtils {

  def consentFactWithAccessibleOffers(
      consentFact: ConsentFact,
      maybePattern: Option[Seq[String]]): ConsentFact = {
    consentFact.copy(offers = consentFact.offers match {
      case None => None
      case Some(offers) =>
        Some(offers
          .filter(offer =>
            accessibleOfferService.accessibleOfferKey(offer.key, maybePattern)))
    })
  }

  private def createOrReplace(tenant: String,
                              author: String,
                              metadata: Option[Seq[(String, String)]],
                              organisation: Organisation,
                              consentFact: ConsentFact,
                              maybeLastConsentFact: Option[ConsentFact] = None)
    : Future[Either[AppErrorWithStatus, ConsentFact]] = {

    organisation.isValidWith(consentFact) match {
      case Some(error) =>
        Logger.error(
          s"invalid consent fact (compare with organisation ${organisation.key} version ${organisation.version}) : $error || ${consentFact.asJson}")
        toErrorWithStatus(error)
      case None =>
        val organisationKey: String = organisation.key
        val userId: String = consentFact.userId

        validateOffersStructures(tenant,
                                 organisationKey,
                                 userId,
                                 consentFact.offers).flatMap {
          case Left(e) =>
            FastFuture.successful(Left(e))
          case Right(_) =>
            maybeLastConsentFact match {
              // Create a new user, consent fact and last consent fact
              case None =>
                for {
                  _ <- consentFactMongoDataStore.insert(tenant, consentFact)
                  _ <- lastConsentFactMongoDataStore.insert(tenant, consentFact)
                  _ <- userMongoDataStore.insert(tenant,
                                                 User(
                                                   userId = userId,
                                                   orgKey = organisationKey,
                                                   orgVersion =
                                                     organisation.version.num,
                                                   latestConsentFactId =
                                                     consentFact._id
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
                  _ <- lastConsentFactMongoDataStore.update(
                    tenant,
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
                toErrorWithStatus(
                  "invalid.lastConsentFactStored.version.sup.consentFact.version")
            }
        }
    }
  }

  private def validateOffersStructures(
      tenant: String,
      orgKey: String,
      userId: String,
      maybeConsentOffersToCompare: Option[Seq[ConsentOffer]])
    : Future[Either[AppErrorWithStatus, Option[Seq[ConsentOffer]]]] =
    maybeConsentOffersToCompare match {
      case None =>
        FastFuture.successful(Right(None))
      case Some(offersToCompare) =>
        Future
          .sequence {
            offersToCompare.map(
              validateOfferStructureWithOrganisation(tenant, orgKey, userId, _))
          }
          .map(sequence)
          .map {
            case Left(errors) => {
              val messages: Seq[ErrorMessage] =
                errors.flatMap(_.appErrors.errors)
              Left(
                controllers.AppErrorWithStatus(AppErrors(messages), BadRequest))
            }
            case Right(_) => Right(Some(offersToCompare))
          }
    }

  private def validateOfferStructureWithOrganisation(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer)
    : Future[Either[AppErrorWithStatus, ConsentOffer]] =
    organisationMongoDataStore
      .findOffer(tenant, orgKey, offerToCompare.key)
      .flatMap {
        case Left(error) => toErrorWithStatus(error, NotFound)
        case Right(None) =>
          toErrorWithStatus(s"offer.${offerToCompare.key}.not.found", NotFound)
        case Right(Some(offer)) if offerToCompare.version > offer.version =>
          Logger.error(
            s"offer.${offerToCompare.key}.version.${offerToCompare.version} > ${offer.version}")
          toErrorWithStatus(
            s"offer.${offerToCompare.key}.version.${offerToCompare.version}.unavailable")
        case Right(Some(permissionOffer))
            if offerToCompare.version < permissionOffer.version =>
          validateOfferStructureWithConsent(tenant,
                                            orgKey,
                                            userId,
                                            offerToCompare)
        case Right(Some(permissionOffer))
            if offerToCompare.version == permissionOffer.version && compareConsentOfferWithPermissionOfferStructure(
              offerToCompare,
              permissionOffer) =>
          FastFuture.successful(Right(offerToCompare))
        case _ =>
          Logger.error(
            s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")
          toErrorWithStatus(
            s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")
      }

  private def validateOfferStructureWithConsent(tenant: String,
                                                orgKey: String,
                                                userId: String,
                                                offerToCompare: ConsentOffer)
    : Future[Either[AppErrorWithStatus, ConsentOffer]] =
    lastConsentFactMongoDataStore
      .findConsentOffer(tenant, orgKey, userId, offerToCompare.key)
      .flatMap {
        case Left(errors) => toErrorWithStatus(errors, BadRequest)
        case Right(None) =>
          Logger.error(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
          toErrorWithStatus(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
        case Right(Some(offer)) if offer.version != offerToCompare.version =>
          Logger.error(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.not.equal.to.${offer.version}")
          toErrorWithStatus(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
        case Right(Some(lastConsentOffer))
            if compareConsentOffersStructure(offerToCompare,
                                             lastConsentOffer) =>
          FastFuture.successful(Right(offerToCompare))
        case _ =>
          Logger.error(
            s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsent")
          toErrorWithStatus(
            s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsentfact")
      }

  private def convertConsentOfferToPermissionOffer(
      consentOffer: ConsentOffer): Offer = {
    Offer(
      key = consentOffer.key,
      label = consentOffer.label,
      version = consentOffer.version,
      groups = consentOffer.groups.map { g =>
        PermissionGroup(
          key = g.key,
          label = g.label,
          permissions = g.consents.map { p =>
            Permission(
              key = p.key,
              label = p.label
            )
          }
        )
      }
    )
  }

  private def compareConsentOffersStructure(
      offerToCompare: ConsentOffer,
      lastConsentOffer: ConsentOffer): Boolean = {
    val permissionOffer: Offer = convertConsentOfferToPermissionOffer(
      lastConsentOffer)
    compareConsentOfferWithPermissionOfferStructure(offerToCompare,
                                                    permissionOffer)
  }

  private def compareConsentOfferWithPermissionOfferStructure(
      consentOffer: ConsentOffer,
      permissionOffer: Offer): Boolean = {
    consentOffer.label == permissionOffer.label &&
    consentOffer.version == permissionOffer.version &&
    permissionOffer.groups.forall(
      permissionGroup =>
        consentOffer.groups
          .find(c => c.key == permissionGroup.key)
          .exists(
            consents =>
              consents.key == permissionGroup.key &&
                consents.label == permissionGroup.label &&
                consents.consents.forall(c =>
                  permissionGroup.permissions
                    .find(permission => permission.key == c.key)
                    .exists(permission =>
                      c.key == permission.key && c.label == permission.label))))
  }

  private def sequence[A, B](s: Seq[Either[A, B]]): Either[Seq[A], B] =
    s.foldLeft(Left(Nil): Either[List[A], B]) { (acc, e) =>
      for (xs <- acc.left; x <- e.left) yield x :: xs
    }

  def saveConsents(tenant: String,
                   author: String,
                   metadata: Option[Seq[(String, String)]],
                   organisationKey: String,
                   userId: String,
                   consentFact: ConsentFact)
    : Future[Either[AppErrorWithStatus, ConsentFact]] = {
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
                toErrorWithStatus("error.specified.org.never.released")

              case Some(organisation)
                  if organisation.version.num != consentFact.version =>
                Logger.error(
                  s"error.specified.version.not.latest : latest version ${organisation.version.num} -> version specified ${consentFact.version}")
                toErrorWithStatus("error.specified.version.not.latest")

              case Some(organisation)
                  if organisation.version.num == consentFact.version =>
                createOrReplace(tenant,
                                author,
                                metadata,
                                organisation,
                                consentFact)
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
                createOrReplace(tenant,
                                author,
                                metadata,
                                organisation,
                                consentFact,
                                Some(lastConsentFactStored))
              case None =>
                Logger.error(
                  s"error.unknow.specified.version : version specified ${lastConsentFactStored.version}")
                toErrorWithStatus("error.unknow.specified.version")
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
                toErrorWithStatus("error.specified.org.never.released")

              case Some(organisation)
                  if organisation.version.num < consentFact.version =>
                Logger.error(
                  s"error.version.higher.than.release : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}")

                toErrorWithStatus("error.version.higher.than.release")

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
          toErrorWithStatus("error.version.lower.than.stored")
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

  def delete(tenant: String,
             orgKey: String,
             userId: String,
             offerKey: String,
             author: String,
             metadata: Option[Seq[(String, String)]],
             lastConsentFactStored: ConsentFact)
    : Future[Either[AppErrorWithStatus, ConsentOffer]] = {
    import cats.implicits._
    for {
      removeResult <- lastConsentFactMongoDataStore.removeOfferById(tenant,
                                                                    orgKey,
                                                                    userId,
                                                                    offerKey)
      mayBelastConsentFactToStore <- lastConsentFactMongoDataStore
        .findByOrgKeyAndUserId(tenant, orgKey, userId)
      _ <- mayBelastConsentFactToStore match {
        case Some(lastConsentFactToStore) =>
          val lastConsentFactToStoreCopy =
            lastConsentFactToStore.copy(_id = BSONObjectID.generate().stringify)
          consentFactMongoDataStore.insert(tenant, lastConsentFactToStoreCopy) *>
            FastFuture.successful(
              kafkaMessageBroker.publish(
                ConsentFactUpdated(
                  tenant = tenant,
                  oldValue = lastConsentFactStored,
                  payload = lastConsentFactToStore,
                  author = author,
                  metadata = metadata
                )))
        case None => FastFuture.successful(())
      }

    } yield removeResult
  }
}
