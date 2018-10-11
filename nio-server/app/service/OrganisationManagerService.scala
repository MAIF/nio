package service

import akka.http.scaladsl.util.FastFuture
import db.OrganisationMongoDataStore
import messaging.KafkaMessageBroker
import models.{
  Organisation,
  OrganisationCreated,
  OrganisationValidator,
  VersionInfo
}
import utils.Result.{AppErrors, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}

class OrganisationManagerService(
    organisationMongoDataStore: OrganisationMongoDataStore,
    kafkaMessageBroker: KafkaMessageBroker)(
    implicit val executionContext: ExecutionContext) {

  def createOrganisation(tenant: String,
                         orgKey: String,
                         organisation: Organisation,
                         author: String,
                         metadatas: Option[Seq[(String, String)]])
    : Future[Either[AppErrors, Organisation]] = {
    val organisationToCreate = organisation.copy(version = VersionInfo())
    // TODO add restriction (otoroshi metadata restritedOffer)
    OrganisationValidator.validateOrganisation(organisationToCreate) match {
      case Left(e) =>
        FastFuture.successful(Left(e))
      case Right(_) =>
        organisationMongoDataStore.findByKey(tenant, orgKey).flatMap {
          case None =>
            organisationMongoDataStore
              .insert(tenant, organisationToCreate)
              .map { _ =>
                OrganisationCreated(
                  tenant,
                  author,
                  metadatas,
                  payload = organisationToCreate
                )

                Right(organisationToCreate)
              }
          case Some(_) =>
            FastFuture.successful(
              Left(AppErrors(
                Seq(ErrorMessage("error.organisation.key.already.used")))))
        }
    }
  }

}
