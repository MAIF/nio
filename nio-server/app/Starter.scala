import java.util.concurrent.TimeUnit

import javax.inject.{Inject, Singleton}
import db._
import messaging.KafkaMessageBroker
import models.Tenant
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import utils.{DefaultLoader, SecureEvent}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class Starter @Inject()(
    config: Configuration,
    tenantStore: TenantMongoDataStore,
    organisationStore: OrganisationMongoDataStore,
    userStore: UserMongoDataStore,
    consentFactStore: ConsentFactMongoDataStore,
    accountMongoDataStore: AccountMongoDataStore,
    destroyTaskMongoDataStore: DeletionTaskMongoDataStore,
    kafkaMessageBroker: KafkaMessageBroker,
    defaultLoader: DefaultLoader,
    secureEvent: SecureEvent,
    applicationLifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext) {

  val dbFlush = config.get[Boolean]("db.flush")
  val tenants = config.get[Seq[String]]("db.tenants")

  if (dbFlush) {
    Await.result(
      for {
        _ <- tenantStore.init()
        _ <- Future.sequence(tenants.map { tenant =>
          tenantStore.insert(Tenant(tenant, "Default tenant from config file"))
        })
        _ <- Future.sequence(tenants.map { tenant =>
          for {
            _ <- organisationStore.init(tenant)
            _ <- userStore.init(tenant)
            _ <- consentFactStore.init(tenant)
            _ <- accountMongoDataStore.init(tenant)
            _ <- destroyTaskMongoDataStore.init(tenant)
            _ <- defaultLoader.load(tenant)
          } yield {
            ()
          }
        })
      } yield (),
      Duration(60, TimeUnit.SECONDS)
    )
  }

  applicationLifecycle.addStopHook { () =>
    Future.sequence(
      Seq(
        Future.successful(() => Unit)
      )
    )
  }
}
