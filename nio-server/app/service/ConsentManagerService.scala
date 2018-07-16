package service

import akka.http.scaladsl.util.FastFuture
import cats.data.OptionT
import db.{
  ConsentFactMongoDataStore,
  LastConsentFactMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import utils.Result.AppErrors
import cats.implicits._

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
                    author = author
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
                    author = author
                  )
                )
              )
            } yield Right(consentFact)

        }
    }
  }

  def saveConsents(
      tenant: String,
      author: String,
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
                createOrReplace(tenant, author, organisation, consentFact)
            }

        // Update consent fact with the same organisation version
        case Some(lastConsentFactStored)
            if lastConsentFactStored.version == consentFact.version =>
          organisationMongoDataStore
            .findReleasedByKeyAndVersionNum(tenant,
                                            organisationKey,
                                            consentFact.version)
            .flatMap {
              case Some(organisation) =>
                createOrReplace(tenant,
                                author,
                                organisation,
                                consentFact,
                                Option(lastConsentFactStored))
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
      userId: String  <- OptionT.fromOption[Future](maybeUserId)
      _               = Logger.info(s"userId is defined with $userId")
      consentFact     <- OptionT(lastConsentFactMongoDataStore.findByOrgKeyAndUserId(tenant, orgKey, userId))
      built           <- OptionT.pure[Future](buildTemplate(orgKey, orgVersion, template, consentFact, userId))
    } yield built
    // format: on
    res.value.map(_.getOrElse(template))
  }

  private def buildTemplate(orgKey: String,
                            orgVersion: Int,
                            template: ConsentFact,
                            consentFact: ConsentFact,
                            userId: String) = {
    Logger.info(s"consent fact exist")

    val groupsUpdated: Seq[ConsentGroup] =
      template.groups.map(
        group => {
          val maybeGroup = consentFact.groups.find(cg =>
            cg.key == group.key && cg.label == group.label)

          maybeGroup
            .map {
              consentGroup =>
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
        }
      )

    ConsentFact
      .template(orgVersion, groupsUpdated, orgKey)
      .copy(userId = userId)
  }
}
