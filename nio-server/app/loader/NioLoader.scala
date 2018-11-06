package loader

import akka.actor.ActorSystem
import auth._
import com.softwaremill.macwire.wire
import configuration._
import controllers._
import db._
import filters._
import messaging.KafkaMessageBroker
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers.Default
import play.api.mvc.{ActionBuilder, AnyContent, EssentialFilter, Filter}
import play.api.routing.{Router, SimpleRouter}
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

  implicit lazy val nioAccountMongoDataStore: NioAccountMongoDataStore =
    wire[NioAccountMongoDataStore]

  // wire service
  implicit lazy val consentManagerService: ConsentManagerService =
    wire[ConsentManagerService]
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

  // wire Action
  lazy val bodyParserDefault: Default =
    wire[Default]

  lazy val authAction: ActionBuilder[AuthContext, AnyContent] = wire[AuthAction]
  lazy val authActionWithEmail
    : ActionBuilder[AuthContextWithEmail, AnyContent] =
    wire[AuthActionWithEmail]
  lazy val securedAuthAction: ActionBuilder[SecuredAuthContext, AnyContent] =
    wire[SecuredAction]

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

  lazy val nioAccountController: NioAccountController =
    wire[NioAccountController]
  lazy val nioAuthenticateController: NioAuthenticateController =
    wire[NioAuthenticateController]

  lazy val auth0Controller: Auth0Controller = wire[Auth0Controller]

  lazy val mailService: MailService = if (env.config.mailSendingEnable) {
    wire[MailGunService]
  } else {
    wire[MailMockService]
  }

  lazy val nioAccountRouter: SimpleRouter = wire[NioAccountRouter]
  lazy val auth0Router: SimpleRouter = wire[Auth0Router]

  override def router: Router = {
    lazy val prefix: String = "/"

    val routes: Routes = wire[Routes]

    env.config.filter.securityMode match {
      case "otoroshi" =>
        new AppRouter(routes)
      case "auth0" =>
        new AppRouter(routes, Some(auth0Router))
      case "default" =>
        new AppRouter(routes, Some(nioAccountRouter))
      case _ =>
        new AppRouter(routes, Some(nioAccountRouter))
    }
  }

  lazy val securityFilter: Filter = env.config.filter.securityMode match {
    case "otoroshi" => wire[OtoroshiFilter]
    case "auth0"    => wire[Auth0Filter]
    case "default"  => wire[NioDefaultFilter]
    case _          => wire[NioDefaultFilter]
  }

  override def httpFilters: Seq[EssentialFilter] = {
    Seq(securityFilter, gzipFilter)
  }

  implicit val config: Configuration = context.initialConfiguration
  val starter: Starter = wire[Starter]

  starter.initialize()
}

class AppRouter(allRoutes: Routes,
                maybeExtraRoutes: Option[SimpleRouter] = None)
    extends SimpleRouter {

  override def routes: Router.Routes =
    maybeExtraRoutes match {
      case Some(extraRoutes) =>
        extraRoutes.routes.orElse(allRoutes.routes)
      case None =>
        allRoutes.routes
    }

}

class Auth0Router(auth0Controller: Auth0Controller) extends SimpleRouter {
  import play.api.routing.sird._

  override def routes: Router.Routes = {
    case GET(
        p"/api/auth0/callback" ? q_o"code=${codeOpt}" ? q_o"state=${stateOpt}") =>
      auth0Controller.callback(codeOpt, stateOpt)
    case GET(p"/auth0/login") =>
      auth0Controller.login
    case GET(p"/auth0/logout") =>
      auth0Controller.logout
  }
}

class NioAccountRouter(nioAccountController: NioAccountController,
                       nioAuthenticateController: NioAuthenticateController)
    extends SimpleRouter {
  import play.api.routing.sird._

  override def routes: Router.Routes = {
    case GET(
        p"/api/nio/accounts" ? q_o"page=${int(maybePage)}" ? q_o"pageSize=${int(maybePageSize)}") =>
      val page = maybePage.getOrElse(0)
      val pageSize = maybePageSize.getOrElse(10)
      nioAccountController.findAll(page, pageSize)
    case GET(p"/api/nio/accounts/${nioAccountId}") =>
      nioAccountController.find(nioAccountId)
    case POST(p"/api/nio/accounts") =>
      nioAccountController.create()
    case PUT(p"/api/nio/accounts/${nioAccountId}") =>
      nioAccountController.update(nioAccountId)
    case DELETE(p"/api/nio/accounts/${nioAccountId}") =>
      nioAccountController.delete(nioAccountId)
    case POST(p"/api/nio/login") =>
      nioAuthenticateController.login
    case GET(p"/api/nio/logout") =>
      nioAuthenticateController.logout
  }
}
