package service

import akka.http.scaladsl.util.FastFuture
import cats.data.OptionT
import controllers.AppErrorWithStatus
import db.{ConsentFactMongoDataStore, LastConsentFactMongoDataStore, OrganisationMongoDataStore, UserMongoDataStore}
import messaging.KafkaMessageBroker
import models._
import utils.NioLogger
import play.api.libs.json.Json
import play.api.mvc.Results._
import reactivemongo.api.bson.BSONObjectID
import utils.Result.{AppErrors, ErrorMessage}
import scala.collection.Seq

import scala.concurrent.{ExecutionContext, Future}

class ConsentManagerService(
    lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    consentFactMongoDataStore: ConsentFactMongoDataStore,
    userMongoDataStore: UserMongoDataStore,
    organisationMongoDataStore: OrganisationMongoDataStore,
    accessibleOfferService: AccessibleOfferManagerService,
    kafkaMessageBroker: KafkaMessageBroker
)(implicit val executionContext: ExecutionContext)
    extends ServiceUtils {

  def consentFactWithAccessibleOffers(consentFact: ConsentFact, maybePattern: Option[Seq[String]]): ConsentFact =
    consentFact.copy(offers = consentFact.offers match {
      case None         => None
      case Some(offers) =>
        Some(
          offers
            .filter(offer => accessibleOfferService.accessibleOfferKey(offer.key, maybePattern))
        )
    })

  private def createOrReplace(
      tenant: String,
      author: String,
      metadata: Option[Seq[(String, String)]],
      organisation: Organisation,
      consentFact: ConsentFact,
      maybeLastConsentFact: Option[ConsentFact] = None
  ): Future[Either[AppErrorWithStatus, ConsentFact]] =
    organisation.isValidWith(consentFact) match {
      case Some(error) =>
        NioLogger.error(
          s"invalid consent fact (compare with organisation ${organisation.key} version ${organisation.version}) : $error || ${consentFact.asJson()}"
        )
        toErrorWithStatus(error)
      case None        =>
        val organisationKey: String = organisation.key
        val userId: String          = consentFact.userId

        validateOffersStructures(tenant, organisationKey, userId, consentFact.offers).flatMap {
          case Left(e)  =>
            NioLogger.error(s"validate offers structure $e")
            FastFuture.successful(Left(e))
          case Right(_) =>
            maybeLastConsentFact match {
              // Create a new user, consent fact and last consent fact
              case None =>
                for {
                  _ <- consentFactMongoDataStore.insert(tenant, consentFact.notYetSendToKafka())
                  _ <- lastConsentFactMongoDataStore.insert(tenant, consentFact)
                  _ <- userMongoDataStore.insert(
                         tenant,
                         User(
                           userId = userId,
                           orgKey = organisationKey,
                           orgVersion = organisation.version.num,
                           latestConsentFactId = consentFact._id
                         )
                       )
                  _ <- publishAndUpdateConsent(
                         tenant,
                         ConsentFactCreated(
                           tenant = tenant,
                           payload = consentFact,
                           author = author,
                           metadata = metadata
                         ),
                         consentFact
                       )
                } yield Right(consentFact)
              // Update user, consent fact and last consent fact
              case Some(lastConsentFactStored)
                  if consentFact.version >= lastConsentFactStored.version
                    && !consentFact.lastUpdate.isBefore(lastConsentFactStored.lastUpdate)
                    && !isOffersUpdateConflict(lastConsentFactStored.offers, consentFact.offers) =>
                val offers             = consentFact.offers match {
                  case Some(newOffers) =>
                    lastConsentFactStored.offers match {
                      case Some(lastOffers) =>
                        Some(newOffers ++ lastOffers.filterNot(o => newOffers.exists(no => no.key == o.key)))
                      case None             => Some(newOffers)
                    }
                  case None            => lastConsentFactStored.offers
                }
                val consentFactToStore = consentFact.copy(offers = offers)

                val lastConsentFactToStore =
                  consentFactToStore.copy(lastConsentFactStored._id)

                for {
                  _ <- lastConsentFactMongoDataStore.update(tenant, organisationKey, userId, lastConsentFactToStore)
                  _ <- consentFactMongoDataStore.insert(tenant, consentFactToStore.notYetSendToKafka())
                  _ <- publishAndUpdateConsent(
                         tenant,
                         ConsentFactUpdated(
                           tenant = tenant,
                           oldValue = lastConsentFactStored,
                           payload = lastConsentFactToStore,
                           author = author,
                           metadata = metadata
                         ),
                         consentFactToStore
                       )
                } yield Right(consentFactToStore)

              case Some(lastConsentFactStored) if consentFact.lastUpdate.isBefore(lastConsentFactStored.lastUpdate) =>
                NioLogger.error(
                  s"consentFact.lastUpdate < lastConsentFactStored.lastUpdate (${consentFact.lastUpdate} < ${lastConsentFactStored.lastUpdate}) for ${lastConsentFactStored._id}"
                )
                toErrorWithStatus("the.specified.update.date.must.be.greater.than.the.saved.update.date", Conflict)
              case Some(lastConsentFactStored)
                  if isOffersUpdateConflict(lastConsentFactStored.offers, consentFact.offers) =>
                val conflictedOffers =
                  conflictedOffersByLastUpdate(lastConsentFactStored.offers, consentFact.offers).get
                NioLogger.error(s"offer.lastUpdate < lastOfferStored.lastUpdate (${conflictedOffers
                  .map(o => s"${o._1.key} -> ${o._2.lastUpdate} < ${o._1.lastUpdate}")
                  .mkString(", ")})")
                toErrorWithStatus(
                  "the.specified.offer.update.date.must.be.greater.than.the.saved.offer.update.date",
                  Conflict
                )
              case Some(lastConsentFactStored)                                                                      =>
                NioLogger.error(
                  s"lastConsentFactStored.version > consentFact.version (${lastConsentFactStored.version} > ${consentFact.version}) for ${lastConsentFactStored._id}"
                )
                toErrorWithStatus("invalid.lastConsentFactStored.version.sup.consentFact.version")
            }
        }
    }

  private def conflictedOffersByLastUpdate(
      lastOffers: Option[Seq[ConsentOffer]],
      offers: Option[Seq[ConsentOffer]]
  ): Option[Seq[(ConsentOffer, ConsentOffer)]] =
    for {
      lastOffers <- lastOffers
      offers     <- offers
    } yield offers.foldRight(Seq.empty[(ConsentOffer, ConsentOffer)])((offer, acc) =>
      lastOffers.find(o => o.key == offer.key) match {
        case Some(lastOffer) if offer.lastUpdate.isBefore(lastOffer.lastUpdate) =>
          acc ++ Seq((lastOffer, offer))
        case Some(_)                                                            => acc
        case None                                                               => acc
      }
    )

  private def isOffersUpdateConflict(lastOffers: Option[Seq[ConsentOffer]], offers: Option[Seq[ConsentOffer]]) =
    (for {
      lastOffers <- lastOffers
      offers     <- offers
    } yield offers.foldRight(false)((offer, acc) =>
      lastOffers.find(o => o.key == offer.key) match {
        case Some(lastOffer) =>
          acc || offer.lastUpdate.isBefore(lastOffer.lastUpdate)
        case None            => acc
      }
    )).getOrElse(false)

  private def publishAndUpdateConsent(tenant: String, event: NioEvent, consentFact: ConsentFact) = {
    kafkaMessageBroker
      .publish(event)
      .flatMap(_ =>
        consentFactMongoDataStore
          .updateOne(tenant, consentFact._id, consentFact.nowSendToKafka())
      )

    FastFuture.successful(true)
  }

  private def validateOffersStructures(
      tenant: String,
      orgKey: String,
      userId: String,
      maybeConsentOffersToCompare: Option[Seq[ConsentOffer]]
  ): Future[Either[AppErrorWithStatus, Option[Seq[ConsentOffer]]]] =
    maybeConsentOffersToCompare match {
      case None                                             =>
        FastFuture.successful(Right(None))
      case Some(offersToCompare) if offersToCompare.isEmpty =>
        FastFuture.successful(Right(None))
      case Some(offersToCompare)                            =>
        Future
          .traverse(offersToCompare) {
            validateOfferStructureWithOrganisation(tenant, orgKey, userId, _)
          }
          .map(sequence)
          .map {
            case Left(errors) =>
              val messages: Seq[ErrorMessage] =
                errors.flatMap(_.appErrors.errors)
              Left(controllers.AppErrorWithStatus(AppErrors(messages), BadRequest))
            case Right(_)     => Right(Some(offersToCompare))
          }
    }

  private def validateOfferStructureWithOrganisation(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer
  ): Future[Either[AppErrorWithStatus, ConsentOffer]] =
    organisationMongoDataStore
      .findOffer(tenant, orgKey, offerToCompare.key)
      .flatMap {
        case Left(error)                                                                      => toErrorWithStatus(error, NotFound)
        case Right(None)                                                                      =>
          toErrorWithStatus(s"offer.${offerToCompare.key}.not.found", NotFound)
        case Right(Some(offer)) if offerToCompare.version > offer.version                     =>
          NioLogger.error(s"offer.${offerToCompare.key}.version.${offerToCompare.version} > ${offer.version}")
          toErrorWithStatus(s"offer.${offerToCompare.key}.version.${offerToCompare.version}.unavailable")
        case Right(Some(permissionOffer)) if offerToCompare.version < permissionOffer.version =>
          validateOfferStructureWithConsent(tenant, orgKey, userId, offerToCompare)
        case Right(Some(permissionOffer))
            if offerToCompare.version == permissionOffer.version && compareConsentOfferWithPermissionOfferStructure(
              offerToCompare,
              permissionOffer
            ) =>
          FastFuture.successful(Right(offerToCompare))
        case _                                                                                =>
          NioLogger.error(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")
          toErrorWithStatus(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")
      }

  private def validateOfferStructureWithConsent(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer
  ): Future[Either[AppErrorWithStatus, ConsentOffer]] =
    lastConsentFactMongoDataStore
      .findConsentOffer(tenant, orgKey, userId, offerToCompare.key)
      .flatMap {
        case Left(errors)                                                                                     => toErrorWithStatus(errors, BadRequest)
        case Right(None)                                                                                      =>
          NioLogger.error(s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
          toErrorWithStatus(s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
        case Right(Some(offer)) if offer.version != offerToCompare.version                                    =>
          NioLogger.error(
            s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.not.equal.to.${offer.version}"
          )
          toErrorWithStatus(s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
        case Right(Some(lastConsentOffer)) if compareConsentOffersStructure(offerToCompare, lastConsentOffer) =>
          FastFuture.successful(Right(offerToCompare))
        case _                                                                                                =>
          NioLogger.error(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsent")
          toErrorWithStatus(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsentfact")
      }

  private def convertConsentOfferToPermissionOffer(consentOffer: ConsentOffer): Offer =
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

  private def compareConsentOffersStructure(offerToCompare: ConsentOffer, lastConsentOffer: ConsentOffer): Boolean = {
    val permissionOffer: Offer = convertConsentOfferToPermissionOffer(lastConsentOffer)
    compareConsentOfferWithPermissionOfferStructure(offerToCompare, permissionOffer)
  }

  private def compareConsentOfferWithPermissionOfferStructure(
      consentOffer: ConsentOffer,
      permissionOffer: Offer
  ): Boolean =
    consentOffer.label == permissionOffer.label &&
    consentOffer.version == permissionOffer.version &&
    permissionOffer.groups.forall(permissionGroup =>
      consentOffer.groups
        .find(c => c.key == permissionGroup.key)
        .exists(consents =>
          consents.key == permissionGroup.key &&
          consents.label == permissionGroup.label &&
          consents.consents.forall(c =>
            permissionGroup.permissions
              .find(permission => permission.key == c.key)
              .exists(permission => c.key == permission.key && c.label == permission.label)
          )
        )
    )

  def saveConsents(
      tenant: String,
      author: String,
      metadata: Option[Seq[(String, String)]],
      organisationKey: String,
      userId: String,
      consentFact: ConsentFact
  ): Future[Either[AppErrorWithStatus, ConsentFact]] =
    lastConsentFactMongoDataStore
      .findByOrgKeyAndUserId(tenant, organisationKey, userId)
      .flatMap {
        // Insert a new user, last consent fact, historic consent fact
        case None                                                                                =>
          organisationMongoDataStore
            .findLastReleasedByKey(tenant, organisationKey)
            .flatMap {
              case None =>
                NioLogger.error(s"error.specified.org.never.released for organisation key $organisationKey")
                toErrorWithStatus("error.specified.org.never.released")

              case Some(organisation) if organisation.version.num != consentFact.version =>
                NioLogger.error(
                  s"error.specified.version.not.latest : latest version ${organisation.version.num} -> version specified ${consentFact.version}"
                )
                toErrorWithStatus("error.specified.version.not.latest")

              case Some(organisation) if organisation.version.num == consentFact.version =>
                createOrReplace(tenant, author, metadata, organisation, consentFact)
            }

        // Update consent fact with the same organisation version
        case Some(lastConsentFactStored) if lastConsentFactStored.version == consentFact.version =>
          organisationMongoDataStore
            .findReleasedByKeyAndVersionNum(tenant, organisationKey, lastConsentFactStored.version)
            .flatMap {
              case Some(organisation) =>
                createOrReplace(tenant, author, metadata, organisation, consentFact, Some(lastConsentFactStored))
              case None               =>
                NioLogger.error(s"error.unknow.specified.version : version specified ${lastConsentFactStored.version}")
                toErrorWithStatus("error.unknow.specified.version")
            }

        // Update consent fact with the new organisation version
        case Some(lastConsentFactStored) if lastConsentFactStored.version <= consentFact.version =>
          organisationMongoDataStore
            .findLastReleasedByKey(tenant, organisationKey)
            .flatMap {
              case None =>
                NioLogger.error(s"error.specified.org.never.released for organisation key $organisationKey")
                toErrorWithStatus("error.specified.org.never.released")

              case Some(organisation) if organisation.version.num < consentFact.version =>
                NioLogger.error(
                  s"error.version.higher.than.release : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}"
                )

                toErrorWithStatus("error.version.higher.than.release")

              case Some(organisation) =>
                createOrReplace(tenant, author, metadata, organisation, consentFact, Option(lastConsentFactStored))
            }

        // Cannot rollback and update a consent fact to an old organisation version
        case Some(lastConsentFactStored) if lastConsentFactStored.version >= consentFact.version =>
          NioLogger.error(
            s"error.version.lower.than.stored : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}"
          )
          toErrorWithStatus("error.version.lower.than.stored")
      }

  def mergeTemplateWithConsentFact(
      tenant: String,
      orgKey: String,
      orgVersion: Int,
      template: ConsentFact,
      maybeUserId: Option[String]
  ): Future[ConsentFact] = {

    import cats.implicits._

    // format: off
    val res: OptionT[Future, ConsentFact] = for {
      userId: String <- OptionT.fromOption[Future](maybeUserId)
      _ = NioLogger.info(s"userId is defined with $userId")
      consentFact <- OptionT(lastConsentFactMongoDataStore.findByOrgKeyAndUserId(tenant, orgKey, userId))
      _ = NioLogger.info(s"consentfact existing for  $userId -> ${consentFact._id}")
      built <- OptionT.pure[Future](buildTemplate(orgKey, orgVersion, template, consentFact, userId))
    } yield built
    // format: on
    res.getOrElse(template)
  }

  private def buildTemplate(
      orgKey: String,
      orgVersion: Int,
      template: ConsentFact,
      consentFact: ConsentFact,
      userId: String
  ) = {
    NioLogger.info(s"consent fact exist")

    val mergeConsentGroup =
      (maybeGroup: Option[ConsentGroup], group: ConsentGroup) =>
        maybeGroup
          .map { consentGroup =>
            val groups: Seq[Consent] = group.consents.map { consent =>
              val maybeConsent =
                consentGroup.consents.find(c => c.key == consent.key && c.label == consent.label)
              maybeConsent match {
                case Some(consentValue) =>
                  consent.copy(checked = consentValue.checked)
                case None               =>
                  consent
              }
            }
            group.copy(consents = groups)
          }
          .getOrElse(group)

    val groupsUpdated: Seq[ConsentGroup] =
      template.groups.map { group =>
        val maybeGroup = consentFact.groups.find(cg => cg.key == group.key && cg.label == group.label)

        mergeConsentGroup(maybeGroup, group)
      }

    val offersUpdated: Option[Seq[ConsentOffer]] =
      template.offers.map(offers =>
        offers.map { offer =>
          val maybeOffer = consentFact.offers
            .getOrElse(Seq.empty)
            .find(co => co.key == offer.key)

          maybeOffer
            .map { cOffer =>
              val groups: Seq[ConsentGroup] = offer.groups.map { group =>
                val maybeGroup = cOffer.groups.find(cg => cg.key == group.key && cg.label == group.label)

                mergeConsentGroup(maybeGroup, group)
              }
              offer.copy(groups = groups)
            }
            .getOrElse(offer)
        }
      )

    ConsentFact
      .template(orgVerNum = orgVersion, groups = groupsUpdated, offers = offersUpdated, orgKey = orgKey)
      .copy(userId = userId)
  }

  def delete(
      tenant: String,
      orgKey: String,
      userId: String,
      offerKey: String,
      author: String,
      metadata: Option[Seq[(String, String)]],
      lastConsentFactStored: ConsentFact
  ): Future[Either[AppErrorWithStatus, ConsentOffer]] = {
    import cats.implicits._
    for {
      removeResult                <- lastConsentFactMongoDataStore.removeOfferById(tenant, orgKey, userId, offerKey)
      mayBelastConsentFactToStore <- lastConsentFactMongoDataStore
                                       .findByOrgKeyAndUserId(tenant, orgKey, userId)
      _                           <- mayBelastConsentFactToStore match {
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
                                             )
                                           )
                                         )
                                       case None                         => FastFuture.successful(())
                                     }

    } yield removeResult
  }
}
