package service

import akka.http.scaladsl.util.FastFuture
import db.{
  ConsentFactMongoDataStore,
  LastConsentFactMongoDataStore,
  OrganisationMongoDataStore,
  UserMongoDataStore
}
import messaging.KafkaMessageBroker
import models._
import play.api.Logger
import play.api.libs.json.{JsString, JsValue}

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
    : Future[Either[JsValue, ConsentFact]] = {
    organisation.isValidWith(consentFact) match {
      case Some(error) =>
        Logger.error(
          s"invalid consent fact (compare with organisation ${organisation.key} version ${organisation.version}) : $error || ${consentFact.asJson}")
        FastFuture.successful(Left(JsString(error)))
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
      consentFact: ConsentFact): Future[Either[JsValue, ConsentFact]] = {
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
                  Left(JsString("error.specified.org.never.released")))

              case Some(organisation)
                  if organisation.version.num != consentFact.version =>
                Logger.error(
                  s"error.specified.version.not.latest : latest version ${organisation.version.num} -> version specified ${consentFact.version}")
                FastFuture.successful(
                  Left(JsString("error.specified.version.not.latest")))

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
                  Left(JsString("error.unknow.specified.version")))
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
                  Left(JsString("error.specified.org.never.released")))

              case Some(organisation)
                  if organisation.version.num < consentFact.version =>
                Logger.error(
                  s"error.version.higher.than.release : last version saved ${lastConsentFactStored.version} -> version specified ${consentFact.version}")
                Future.successful(
                  Left(JsString("error.version.higher.than.release")))

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
            Left(JsString("error.version.lower.than.stored")))
      }
  }

}
