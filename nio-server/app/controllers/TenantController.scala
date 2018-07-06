package controllers

import auth.AuthAction
import configuration.Env
import db._
import javax.inject.Inject
import models.{Tenant, TenantCreated, TenantDeleted, Tenants}
import play.api.mvc.ControllerComponents
import messaging.KafkaMessageBroker
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}

class TenantController @Inject()(
    AuthAction: AuthAction,
    tenantStore: TenantMongoDataStore,
    accountMongoDataStore: AccountMongoDataStore,
    lastConsentFactMongoDataStore: LastConsentFactMongoDataStore,
    consentFactStore: ConsentFactMongoDataStore,
    organisationDataStore: OrganisationMongoDataStore,
    userDataStore: UserMongoDataStore,
    extractionTaskDataStore: ExtractionTaskMongoDataStore,
    deletionTaskDataStore: DeletionTaskMongoDataStore,
    conf: Configuration,
    cc: ControllerComponents,
    env: Env,
    broker: KafkaMessageBroker)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  def tenants = AuthAction.async { implicit req =>
    tenantStore.findAll().map { tenants =>
      renderMethod(Tenants(tenants))
    }
  }

  def createTenant = AuthAction(parse.anyContent).async { implicit req =>
    req.headers.get(env.tenantConfig.admin.header) match {
      case Some(secret) if secret == env.tenantConfig.admin.secret =>
        parseMethod[Tenant](Tenant) match {
          case Left(error) =>
            Logger.error("Invalid tenant format " + error)
            Future.successful(BadRequest("error.invalid.tenant.format"))
          case Right(tenant) =>
            tenantStore.findByKey(tenant.key).flatMap {
              case Some(_) =>
                Future.successful(Conflict("error.key.already.used"))
              case None =>
                tenantStore.insert(tenant).flatMap { _ =>
                  broker.publish(
                    TenantCreated(tenant = tenant.key,
                                  payload = tenant,
                                  metadata = req.authInfo.metadatas))

                  Future
                    .sequence(
                      Seq(
                        accountMongoDataStore
                          .init(tenant.key)
                          .map(
                            _ => accountMongoDataStore.ensureIndices(tenant.key)
                          ),
                        consentFactStore
                          .init(tenant.key)
                          .map(
                            _ => consentFactStore.ensureIndices(tenant.key)
                          ),
                        lastConsentFactMongoDataStore
                          .init(tenant.key)
                          .map(
                            _ =>
                              lastConsentFactMongoDataStore.ensureIndices(
                                tenant.key)
                          ),
                        organisationDataStore
                          .init(tenant.key)
                          .map(
                            _ => organisationDataStore.ensureIndices(tenant.key)
                          ),
                        userDataStore
                          .init(tenant.key)
                          .map(
                            _ => userDataStore.ensureIndices(tenant.key)
                          ),
                        extractionTaskDataStore
                          .init(tenant.key)
                          .map(
                            _ =>
                              extractionTaskDataStore.ensureIndices(tenant.key)
                          ),
                        deletionTaskDataStore
                          .init(tenant.key)
                          .map(
                            _ => deletionTaskDataStore.ensureIndices(tenant.key)
                          )
                      )
                    )
                    .map(_ => renderMethod(tenant, Created))
                }
            }
        }
      case _ =>
        Future.successful(Unauthorized("error.missing.secret"))
    }
  }

  def deleteTenant(tenantKey: String) = AuthAction.async { implicit req =>
    req.headers.get(env.tenantConfig.admin.header) match {
      case Some(secret) if secret == env.tenantConfig.admin.secret =>
        tenantStore.findByKey(tenantKey).flatMap {
          case Some(tenantToDelete) =>
            import cats.implicits._
            (
              consentFactStore.deleteConsentFactByTenant(tenantKey),
              lastConsentFactMongoDataStore.deleteConsentFactByTenant(
                tenantKey),
              organisationDataStore.deleteOrganisationByTenant(tenantKey),
              userDataStore.deleteUserByTenant(tenantKey),
              tenantStore.removeByKey(tenantKey)
            ).mapN { (_, _, _, _, _) =>
              broker.publish(
                TenantDeleted(tenant = tenantToDelete.key,
                              payload = tenantToDelete,
                              metadata = req.authInfo.metadatas))
              Ok
            }
          case None =>
            Future.successful(NotFound("error.tenant.not.found"))
        }
      case _ => Future.successful(Unauthorized("error.missing.secret"))
    }
  }
}
