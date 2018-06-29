package loader

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import auth._
import com.codahale.metrics.MetricRegistry
import com.softwaremill.macwire.wire
import configuration._
import controllers._
import db._
import filters.OtoroshiFilter
import messaging.KafkaMessageBroker
import models.Tenant
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers.Default
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.modules.reactivemongo.{
  ReactiveMongoApi,
  ReactiveMongoApiFromContext
}
import router.Routes
import s3._
import utils.{DefaultLoader, S3Manager, SecureEvent}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class NioLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new NioComponents(context).application
  }
}

//object modules {

class NioComponents(context: Context)
    extends ReactiveMongoApiFromContext(context)
    with AhcWSComponents
    with HttpFiltersComponents
    with AssetsComponents {

  implicit val system: ActorSystem = actorSystem

  // wire Reactive mongo
  implicit lazy val reactiveMongo: ReactiveMongoApi = reactiveMongoApi

  // wire Env
  implicit lazy val env: Env = wire[Env]

  // wire DataStore
  implicit lazy val accountDataStore: AccountMongoDataStore =
    wire[AccountMongoDataStore]
  implicit lazy val consentFactDataStore: ConsentFactMongoDataStore =
    wire[ConsentFactMongoDataStore]
  implicit lazy val deletionTaskDataStore: DeletionTaskMongoDataStore =
    wire[DeletionTaskMongoDataStore]
  implicit lazy val extractionTaskDataStore: ExtractionTaskMongoDataStore =
    wire[ExtractionTaskMongoDataStore]
  implicit lazy val lastConsentFactDataStore: LastConsentFactMongoDataStore =
    wire[LastConsentFactMongoDataStore]
  implicit lazy val organisationDataStore: OrganisationMongoDataStore =
    wire[OrganisationMongoDataStore]
  implicit lazy val tenantDataStore: TenantMongoDataStore =
    wire[TenantMongoDataStore]
  implicit lazy val userDataStore: UserMongoDataStore =
    wire[UserMongoDataStore]

  // wire Kafka
  implicit lazy val kafkaMessageBroker: KafkaMessageBroker =
    wire[KafkaMessageBroker]

  // wire S3
  implicit lazy val s3: S3 = wire[S3]
  implicit lazy val s3FileDataStore: S3FileDataStore = wire[S3FileDataStore]
  implicit lazy val s3Manager: S3Manager = wire[S3Manager]
  implicit lazy val s3Configuration: S3Configuration = wire[S3Configuration]
  implicit lazy val secureEvent: SecureEvent = wire[SecureEvent]

  // wire utils
  implicit lazy val defaultLoader: DefaultLoader = wire[DefaultLoader]

  // metrics registry
  implicit lazy val metrics: MetricRegistry = new MetricRegistry

  // wire Action
  lazy val bodyParserDefault: Default =
    wire[Default]
  lazy val authAction: AuthAction = wire[AuthAction]
  lazy val authActionWithEmail: AuthActionWithEmail =
    wire[AuthActionWithEmail]

  // wire Controller
  lazy val accountController: AccountController = wire[AccountController]
  lazy val consentController: ConsentController = wire[ConsentController]
  lazy val deletionController: DeletionController = wire[DeletionController]
  lazy val eventController: EventController = wire[EventController]
  lazy val extractionController: ExtractionController =
    wire[ExtractionController]
  lazy val homeController: HomeController = wire[HomeController]
  lazy val metricsController: MetricsController = wire[MetricsController]
  lazy val organisationController: OrganisationController =
    wire[OrganisationController]
  lazy val tenantController: TenantController = wire[TenantController]
  lazy val userController: UserController = wire[UserController]

  override def router: Router = {
    lazy val prefix: String = "/"

    wire[Routes]
  }

  override def httpFilters: Seq[EssentialFilter] = {
    Seq(new OtoroshiFilter(env))
  }

  // TODO : sortir ce code dans un service util à part
  // TODO : ajouter l'utilisation du secureEvent via un appel explicite!
  val config = context.initialConfiguration
  val dbFlush = config.get[Boolean]("db.flush")

  implicit val mat = ActorMaterializer()(system)

  if (dbFlush) {
    val tenants = config.get[Seq[String]]("db.tenants")

    Await.result(
      for {
        _ <- tenantDataStore.init()
        _ <- Future.sequence(tenants.map { tenant =>
          tenantDataStore.insert(
            Tenant(tenant, "Default tenant from config file"))
        })
        _ <- Future.sequence(tenants.map {
          tenant =>
            for {
              _ <- organisationDataStore.init(tenant)
              _ <- userDataStore.init(tenant)
              _ <- consentFactDataStore.init(tenant)
              _ <- lastConsentFactDataStore.init(tenant)
              _ <- accountDataStore.init(tenant)
              _ <- deletionTaskDataStore.init(tenant)
              _ <- extractionTaskDataStore.init(tenant)
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

  Await.result(
    tenantDataStore.findAll().flatMap { tenants =>
      Future.sequence(
        tenants.map {
          t =>
            Future.sequence(
              Seq(
                {
                  Logger.info(s"Ensuring indices for users on ${t.key}")
                  userDataStore.ensureIndices(t.key)
                }, {
                  Logger.info(s"Ensuring indices for consents on ${t.key}")
                  consentFactDataStore.ensureIndices(t.key)
                }, {
                  Logger.info(s"Ensuring indices for last consents on ${t.key}")
                  lastConsentFactDataStore.ensureIndices(t.key)
                }, {
                  Logger.info(s"Ensuring indices for organisations on ${t.key}")
                  organisationDataStore.ensureIndices(t.key)
                }, {
                  Logger.info(s"Ensuring indices for accounts on ${t.key}")
                  accountDataStore.ensureIndices(t.key)
                }, {
                  Logger.info(s"Ensuring indices for destroy task on ${t.key}")
                  deletionTaskDataStore.ensureIndices(t.key)
                }, {
                  Logger.info(
                    s"Ensuring indices for extraction task on ${t.key}")
                  extractionTaskDataStore.ensureIndices(t.key)
                }
              )
            )
        }
      )
    },
    Duration(5, TimeUnit.MINUTES)
  )

}

//}
