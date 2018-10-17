package loader

import akka.actor.ActorSystem
import auth._
import com.codahale.metrics.MetricRegistry
import com.softwaremill.macwire.wire
import configuration._
import controllers._
import db._
import filters.{AuthInfoDev, AuthInfoMock, OtoroshiFilter}
import messaging.KafkaMessageBroker
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers.Default
import play.api.mvc.{EssentialFilter, Filter}
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.filters.gzip._
import play.modules.reactivemongo.{
  ReactiveMongoApi,
  ReactiveMongoApiFromContext
}
import router.Routes
import s3._
import service.{AccessibleOfferManagerService, _}
import utils.{DefaultLoader, S3Manager, SecureEvent}

class NioLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new NioComponents(context).application
  }
}

class NioComponents(context: Context)
    extends ReactiveMongoApiFromContext(context)
    with AhcWSComponents
    with HttpFiltersComponents
    with AssetsComponents
    with GzipFilterComponents {

  implicit val system: ActorSystem = actorSystem

  implicit lazy val authInfo: AuthInfoMock = wire[AuthInfoDev]

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
  implicit lazy val userExtractTaskDataStore: UserExtractTaskDataStore =
    wire[UserExtractTaskDataStore]

  // wire service
  implicit lazy val consentManagerService: ConsentManagerService =
    wire[ConsentManagerService]
  implicit lazy val organisationManagerService: OrganisationManagerService =
    wire[OrganisationManagerService]
  implicit lazy val accessibleOfferManagerService
    : AccessibleOfferManagerService = wire[AccessibleOfferManagerService]
  implicit lazy val offerManagerService: OfferManagerService =
    wire[OfferManagerService]

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
  lazy val userExtractTaskController: UserExtractController =
    wire[UserExtractController]
  lazy val organisationOfferController: OrganisationOfferController =
    wire[OrganisationOfferController]

  lazy val mailService: MailService = if (env.config.mailSendingEnable) {
    wire[MailGunService]
  } else {
    wire[MailMockService]
  }

  override def router: Router = {
    lazy val prefix: String = "/"

    wire[Routes]
  }

  lazy val securityFilter: Filter = wire[OtoroshiFilter]

  override def httpFilters: Seq[EssentialFilter] = {
    Seq(securityFilter, gzipFilter)
  }

  implicit val config: Configuration = context.initialConfiguration
  val starter: Starter = wire[Starter]

  starter.initialize()
}
