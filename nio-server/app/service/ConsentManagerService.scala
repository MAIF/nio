package service

import cats.Foldable
import cats.data.OptionT
import cats.implicits._
import controllers.AppErrorWithStatus
import db.{ConsentFactMongoDataStore, LastConsentFactMongoDataStore, OrganisationMongoDataStore, UserMongoDataStore}
import messaging.KafkaMessageBroker
import models._
import utils.NioLogger
import play.api.mvc.Results._
import reactivemongo.api.bson.BSONObjectID

import scala.collection.Seq
import libs.io._
import play.api.libs.json.{JsValue, Json}

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
    consentFact.copy(offers = consentFact.offers.map(_.filter(offer => accessibleOfferService.accessibleOfferKey(offer.key, maybePattern))))

  def partialUpdate(tenant: String,
                            author: String,
                            metadata: Option[Seq[(String, String)]],
                            organisationKey: String,
                            userId: String,
                            partialConsentFact: PartialConsentFact,
                            command: JsValue
                         ): IO[AppErrorWithStatus, ConsentFact] = {
    for {
      lastConsent  <- IO.fromFutureOption(lastConsentFactMongoDataStore.findByOrgKeyAndUserId(tenant, organisationKey, userId), AppErrorWithStatus(s"consentfact.${userId}.not.found", NotFound))
      organisation <- IO.fromFutureOption(organisationMongoDataStore.findLastReleasedByKey(tenant, organisationKey), {NioLogger.error(s"error.specified.org.never.released for organisation key $organisationKey"); AppErrorWithStatus("error.specified.org.never.released")})
      consentFact  = partialConsentFact.applyTo(lastConsent, organisation)
      result       <- createOrReplace(tenant, author, metadata, organisation, consentFact, Some(lastConsent), command = command)
    } yield result
  }

  private def createOrReplace(
      tenant: String,
      author: String,
      metadata: Option[Seq[(String, String)]],
      organisation: Organisation,
      consentFactInput: ConsentFact,
      maybeLastConsentFact: Option[ConsentFact] = None,
      command: JsValue
  ): IO[AppErrorWithStatus, ConsentFact] =
    for {
      _                       <- IO.fromEither(organisation.isValidWith(consentFactInput, maybeLastConsentFact)).mapError(m => AppErrorWithStatus(m))
      consentFact: ConsentFact = consentFactInput.setUpValidityPeriods(organisation)
      organisationKey: String  = organisation.key
      userId: String           = consentFact.userId
      _                        <- validateOffersStructures(tenant, organisationKey, userId, consentFact.offers).doOnError{ e => NioLogger.error(s"validate offers structure $e") }
      res                      <- maybeLastConsentFact match {
        // Create a new user, consent fact and last consent fact
        case None =>
          for {
            _ <- consentFactMongoDataStore.insert(tenant, consentFact.notYetSendToKafka()).io[AppErrorWithStatus]
            _ <- lastConsentFactMongoDataStore.insert(tenant, consentFact).io[AppErrorWithStatus]
            _ <- userMongoDataStore.insert(tenant, User(userId = userId, orgKey = organisationKey, orgVersion = organisation.version.num, latestConsentFactId = consentFact._id)).io[AppErrorWithStatus]
            _ <- publishAndUpdateConsent(tenant, ConsentFactCreated(tenant = tenant, payload = consentFact, author = author, metadata = metadata, command = command), consentFact)
          } yield consentFact
        // Update user, consent fact and last consent fact
        case Some(lastConsentFactStored)
          if consentFact.version >= lastConsentFactStored.version
            && !consentFact.lastUpdate.isBefore(lastConsentFactStored.lastUpdate)
            && !isOffersUpdateConflict(lastConsentFactStored.offers, consentFact.offers) =>

          val offers = consentFact.offers.fold(lastConsentFactStored.offers) ( newOffers =>
              lastConsentFactStored.offers.fold(newOffers) ( lastOffers =>
                newOffers ++ lastOffers.filterNot(o => newOffers.exists(no => no.key == o.key))
              ).some
          )

          val consentFactToStore = consentFact.copy(offers = offers)
          val lastConsentFactToStore = consentFactToStore.copy(lastConsentFactStored._id)

          for {
            _ <- lastConsentFactMongoDataStore.update(tenant, organisationKey, userId, lastConsentFactToStore).io[AppErrorWithStatus]
            _ <- consentFactMongoDataStore.insert(tenant, consentFactToStore.notYetSendToKafka()).io[AppErrorWithStatus]
            _ <- publishAndUpdateConsent(tenant, ConsentFactUpdated(tenant = tenant, oldValue = lastConsentFactStored, payload = lastConsentFactToStore, author = author, metadata = metadata, command = command), consentFactToStore)
          } yield consentFactToStore

        case Some(lastConsentFactStored) if consentFact.lastUpdate.isBefore(lastConsentFactStored.lastUpdate) =>
          NioLogger.error(
            s"consentFact.lastUpdate < lastConsentFactStored.lastUpdate (${consentFact.lastUpdate} < ${lastConsentFactStored.lastUpdate}) for ${lastConsentFactStored._id}"
          )
          IO(toErrorWithStatus("the.specified.update.date.must.be.greater.than.the.saved.update.date", Conflict))
        case Some(lastConsentFactStored)
          if isOffersUpdateConflict(lastConsentFactStored.offers, consentFact.offers) =>
          val conflictedOffers = conflictedOffersByLastUpdate(lastConsentFactStored.offers, consentFact.offers).get
          NioLogger.error(s"offer.lastUpdate < lastOfferStored.lastUpdate (${
            conflictedOffers
              .map(o => s"${o._1.key} -> ${o._2.lastUpdate} < ${o._1.lastUpdate}")
              .mkString(", ")
          })")
          IO(toErrorWithStatus(
            "the.specified.offer.update.date.must.be.greater.than.the.saved.offer.update.date",
            Conflict
          ))
        case Some(lastConsentFactStored) =>
          NioLogger.error(
            s"lastConsentFactStored.version > consentFact.version (${lastConsentFactStored.version} > ${consentFact.version}) for ${lastConsentFactStored._id}"
          )
          IO(toErrorWithStatus("invalid.lastConsentFactStored.version.sup.consentFact.version"))
      }
    } yield res

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

  private def publishAndUpdateConsent(tenant: String, event: NioEvent, consentFact: ConsentFact): IO[AppErrorWithStatus, Boolean] = {
    kafkaMessageBroker
      .publish(event)
      .flatMap(_ =>
        consentFactMongoDataStore
          .updateOne(tenant, consentFact._id, consentFact.nowSendToKafka())
      )

    IO.succeed(true)
  }

  private def validateOffersStructures(
      tenant: String,
      orgKey: String,
      userId: String,
      maybeConsentOffersToCompare: Option[Seq[ConsentOffer]]
  ): IO[AppErrorWithStatus, Option[Seq[ConsentOffer]]] =
    maybeConsentOffersToCompare match {
      case None                                             => IO.succeed(None)
      case Some(offersToCompare) if offersToCompare.isEmpty => IO.succeed(None)
      case Some(offersToCompare)                            => offersToCompare.to(List)
          .traverse {validateOfferStructureWithOrganisation(tenant, orgKey, userId, _)}
          .mapError(errs => Foldable[List].fold(errs.to(List))).map(_.some)
    }

  private def validateOfferStructureWithOrganisation(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer
  ): IO[AppErrorWithStatus, ConsentOffer] = {
    for {
      mayBeOffer  <- IO(organisationMongoDataStore.findOffer(tenant, orgKey, offerToCompare.key)).mapError(err => AppErrorWithStatus(err, NotFound))
      offer       <- IO.fromOption(mayBeOffer, AppErrorWithStatus(s"offer.${offerToCompare.key}.not.found", NotFound))
      res         <- offer match {
                      case offer if offerToCompare.version > offer.version =>
                        NioLogger.error(s"offer.${offerToCompare.key}.version.${offerToCompare.version} > ${offer.version}")
                        IO(toErrorWithStatus(s"offer.${offerToCompare.key}.version.${offerToCompare.version}.unavailable"))
                      case permissionOffer if offerToCompare.version < permissionOffer.version =>
                        validateOfferStructureWithConsent(tenant, orgKey, userId, offerToCompare)
                      case permissionOffer
                        if offerToCompare.version == permissionOffer.version && compareConsentOfferWithPermissionOfferStructure(
                          offerToCompare,
                          permissionOffer
                        ) =>
                        IO.succeed[AppErrorWithStatus](offerToCompare)
                      case _ =>
                        NioLogger.error(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation")
                        IO(toErrorWithStatus(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.organisation"))
                  }
    } yield res
  }

  private def validateOfferStructureWithConsent(
      tenant: String,
      orgKey: String,
      userId: String,
      offerToCompare: ConsentOffer
  ): IO[AppErrorWithStatus, ConsentOffer] = {
    for {
      mayBeOffer <- IO(lastConsentFactMongoDataStore.findConsentOffer(tenant, orgKey, userId, offerToCompare.key)).mapError(errors => AppErrorWithStatus(errors, BadRequest))
      res <- IO.fromOption(mayBeOffer, {
        NioLogger.error(s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
        AppErrorWithStatus(s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
      }).keep(
        offer => compareConsentOffersStructure(offerToCompare, offer),
        offer => {
          if (offer.version != offerToCompare.version) {
            NioLogger.error(
              s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.not.equal.to.${offer.version}"
            )
            AppErrorWithStatus(s"offer.${offerToCompare.key}.with.version.${offerToCompare.version}.unavailable")
          } else {
            NioLogger.error(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsent")
            AppErrorWithStatus(s"offer.${offerToCompare.key}.structure.unavailable.compare.to.lastconsentfact")
          }
        }
      )
    } yield res
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

  private def sameVersion(organisation: Organisation, consentFact: ConsentFact): Boolean =
    organisation.version.num == consentFact.version

  def saveConsents(
      tenant: String,
      author: String,
      metadata: Option[Seq[(String, String)]],
      organisationKey: String,
      userId: String,
      consentFact: ConsentFact,
      command: JsValue
  ): IO[AppErrorWithStatus, ConsentFact] =
    for {
      mayBeLastConsent <- IO.fromFuture[AppErrorWithStatus](lastConsentFactMongoDataStore.findByOrgKeyAndUserId(tenant, organisationKey, userId))
      res <- mayBeLastConsent match {
        case None => IO
            .fromFutureOption(
              organisationMongoDataStore.findLastReleasedByKey(tenant, organisationKey),
              // If released not found
              {
                NioLogger.error(s"error.specified.org.never.released for organisation key $organisationKey")
                AppErrorWithStatus("error.specified.org.never.released")
              }
            ).keep(
              organisation => sameVersion(organisation, consentFact),
              // If released not found
              organisation => {
                NioLogger.error(
                  s"error.specified.version.not.latest : latest version ${organisation.version.num} -> version specified ${consentFact.version}"
                )
                AppErrorWithStatus("error.specified.version.not.latest")
            })
          .flatMap { organisation =>
            createOrReplace(tenant, author, metadata, organisation, consentFact, command = command)
          }

        // Update consent fact with the same organisation version
        case Some(lastConsentFactStored) if lastConsentFactStored.version == consentFact.version =>
          IO
            .fromFutureOption(
              organisationMongoDataStore.findReleasedByKeyAndVersionNum(tenant, organisationKey, lastConsentFactStored.version),
              {
                NioLogger.error(s"error.unknow.specified.version : version specified ${lastConsentFactStored.version}")
                AppErrorWithStatus("error.unknow.specified.version")
              }
            )
            .flatMap { organisation =>
              createOrReplace(tenant, author, metadata, organisation, consentFact, Some(lastConsentFactStored), command = command)
            }

        // Update consent fact with the new organisation version
        case Some(lastConsentFactStored) if lastConsentFactStored.version <= consentFact.version =>
          IO
            .fromFutureOption(organisationMongoDataStore.findLastReleasedByKey(tenant, organisationKey), {
              NioLogger.error(s"error.specified.org.never.released for organisation key $organisationKey")
              AppErrorWithStatus("error.specified.org.never.released")
            })
            .keep(organisation => !(organisation.version.num < consentFact.version), {
              NioLogger.error(
                s"error.version.higher.than.release : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}"
              )
              _ => AppErrorWithStatus("error.version.higher.than.release")
            })
            .flatMap { organisation =>
                createOrReplace(tenant, author, metadata, organisation, consentFact, Option(lastConsentFactStored), command = command)
            }

        // Cannot rollback and update a consent fact to an old organisation version
        case Some(lastConsentFactStored)  =>
          NioLogger.error(
            s"error.version.lower.than.stored : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}"
          )
          IO(toErrorWithStatus("error.version.lower.than.stored"))
      }
    } yield res

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
  ): ConsentFact = {
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

    val consentFactFiltered = consentFact.filterExpiredConsent(false)

    val groupsUpdated: Seq[ConsentGroup] =
      template.groups.map { group =>
        val maybeGroup = consentFactFiltered.groups
          .find(cg => cg.key == group.key && cg.label == group.label)

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
  ): IO[AppErrorWithStatus, ConsentOffer] = {
    import cats.implicits._
    for {
      removeResult                <- IO(lastConsentFactMongoDataStore.removeOfferById(tenant, orgKey, userId, offerKey))
      mayBelastConsentFactToStore <- lastConsentFactMongoDataStore.findByOrgKeyAndUserId(tenant, orgKey, userId).io[AppErrorWithStatus]
      _                           <- mayBelastConsentFactToStore match {
                                       case Some(lastConsentFactToStore) =>

                                         val lastConsentFactToStoreCopy = lastConsentFactToStore.copy(_id = BSONObjectID.generate().stringify)

                                         consentFactMongoDataStore.insert(tenant, lastConsentFactToStoreCopy).io[AppErrorWithStatus] *>
                                             kafkaMessageBroker.publish(
                                               ConsentFactUpdated(
                                                 tenant = tenant,
                                                 oldValue = lastConsentFactStored,
                                                 payload = lastConsentFactToStore,
                                                 author = author,
                                                 metadata = metadata,
                                                 command = Json.obj()
                                               )
                                             ).io[AppErrorWithStatus]
                                       case None                         => IO.succeed[AppErrorWithStatus](())
                                     }

    } yield removeResult
  }
}
