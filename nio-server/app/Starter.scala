import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import configuration.Env
import javax.inject.{Inject, Singleton}
import db._
import messaging.KafkaMessageBroker
import models.Tenant

import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import s3.S3
import utils.{DefaultLoader, SecureEvent}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class Starter @Inject()(config: Configuration,
                        tenantStore: TenantMongoDataStore,
                        organisationStore: OrganisationMongoDataStore,
                        userStore: UserMongoDataStore,
                        consentFactStore: ConsentFactMongoDataStore,
                        accountMongoDataStore: AccountMongoDataStore,
                        destroyTaskMongoDataStore: DeletionTaskMongoDataStore,
                        extractionTaskMongoDataStore: ExtractionTaskMongoDataStore,
                        kafkaMessageBroker: KafkaMessageBroker,
                        defaultLoader: DefaultLoader,
                        secureEvent: SecureEvent,
                        s3: S3,
                        env: Env,
                        applicationLifecycle: ApplicationLifecycle)(
    implicit ec: ExecutionContext,
    system: ActorSystem) {

  val dbFlush = config.get[Boolean]("db.flush")
  val tenants = config.get[Seq[String]]("db.tenants")

  implicit val mat = ActorMaterializer()(system)

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
            _ <- extractionTaskMongoDataStore.init(tenant)
            _ <- defaultLoader.load(tenant)
          } yield {
            ()
          }
        })
        _ = s3.startExpiredFilesCleaner
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
