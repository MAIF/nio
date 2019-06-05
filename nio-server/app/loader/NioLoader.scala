package loader

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import auth._
import com.softwaremill.macwire.wire
import configuration._
import controllers._
import db._
import db.mongo._
import db.postgres._
import filters._
import messaging.KafkaMessageBroker
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.libs.json.Json
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers.Default
import play.api.mvc.{
  ActionBuilder,
  AnyContent,
  EssentialFilter,
  Filter,
  RequestHeader,
  Result,
  Results
}
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

import scala.concurrent.Future

class NioLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    context.initialConfiguration.get[String]("nio.storage") match {
      case "postgres" => new NioComponentsPostgres(context).application
      case _          => new NioComponentsMongo(context).application
    }
  }
}

class NioComponentsPostgres(val context: Context)
    extends NioComponents(context) {

  override def init(): Unit = {
    val database: Database = dbApi.database("default")
    DbInitializer(database.dataSource, configuration)
  }

  // wire DataStore
  implicit lazy val accountDataStore: AccountDataStore =
    wire[AccountPostgresDataStore]
  implicit lazy val consentFactDataStore: ConsentFactDataStore =
    wire[ConsentFactPostgresDataStore]
  implicit lazy val deletionTaskDataStore: DeletionTaskDataStore =
    wire[DeletionTaskPostgresDataStore]
  implicit lazy val extractionTaskDataStore: ExtractionTaskDataStore =
    wire[ExtractionTaskPostgresDataStore]
  implicit lazy val lastConsentFactDataStore: LastConsentFactDataStore =
    wire[LastConsentFactPostgresDataStore]
  implicit lazy val organisationDataStore: OrganisationDataStore =
    wire[OrganisationPostgresDataStore]
  implicit lazy val tenantDataStore: TenantDataStore =
    wire[TenantPostgresDataStore]
  implicit lazy val userDataStore: UserDataStore = wire[UserPostgresDataStore]
  implicit lazy val userExtractTaskDataStore: UserExtractTaskDataStore =
    wire[UserExtractTaskPostgresDataStore]
  implicit lazy val catchupLockDataStore: CatchupLockDataStore =
    wire[CatchupLockPostgresDataStore]
  implicit lazy val nioAccountDataStore: NioAccountDataStore =
    wire[NioAccountPostgresDataStore]
  implicit lazy val apiKeyDataStore: ApiKeyDataStore =
    wire[ApiKeyPostgresDataStore]
}

class NioComponentsMongo(val context: Context) extends NioComponents(context) {

  // wire Reactive mongo
  implicit lazy val reactiveMongo: ReactiveMongoApi = reactiveMongoApi

  // wire DataStore
  implicit lazy val accountDataStore: AccountDataStore =
    wire[AccountMongoDataStore]
  implicit lazy val consentFactDataStore: ConsentFactDataStore =
    wire[ConsentFactMongoDataStore]
  implicit lazy val deletionTaskDataStore: DeletionTaskDataStore =
    wire[DeletionTaskMongoDataStore]
  implicit lazy val extractionTaskDataStore: ExtractionTaskDataStore =
    wire[ExtractionTaskMongoDataStore]
  implicit lazy val lastConsentFactDataStore: LastConsentFactDataStore =
    wire[LastConsentFactMongoDataStore]
  implicit lazy val organisationDataStore: OrganisationDataStore =
    wire[OrganisationMongoDataStore]
  implicit lazy val tenantDataStore: TenantDataStore =
    wire[TenantMongoDataStore]
  implicit lazy val userDataStore: UserDataStore = wire[UserMongoDataStore]
  implicit lazy val userExtractTaskDataStore: UserExtractTaskDataStore =
    wire[UserExtractTaskMongoDataStore]
  implicit lazy val catchupLockDataStore: CatchupLockDataStore =
    wire[CatchupLockMongoDataStore]
  implicit lazy val nioAccountDataStore: NioAccountDataStore =
    wire[NioAccountMongoDataStore]
  implicit lazy val apiKeyDataStore: ApiKeyDataStore =
    wire[ApiKeyMongoDataStore]

  override def init(): Unit = Unit
}

abstract class NioComponents(context: Context)
    extends ReactiveMongoApiFromContext(context)
    with DBComponents
    with HikariCPComponents
    with AhcWSComponents
    with HttpFiltersComponents
    with AssetsComponents
    with GzipFilterComponents {

  implicit val system: ActorSystem = actorSystem

  def init(): Unit

  implicit def accountDataStore: AccountDataStore
  implicit def consentFactDataStore: ConsentFactDataStore
  implicit def deletionTaskDataStore: DeletionTaskDataStore
  implicit def extractionTaskDataStore: ExtractionTaskDataStore
  implicit def lastConsentFactDataStore: LastConsentFactDataStore
  implicit def organisationDataStore: OrganisationDataStore
  implicit def tenantDataStore: TenantDataStore
  implicit def userDataStore: UserDataStore
  implicit def userExtractTaskDataStore: UserExtractTaskDataStore
  implicit def catchupLockDataStore: CatchupLockDataStore
  implicit def nioAccountDataStore: NioAccountDataStore
  implicit def apiKeyDataStore: ApiKeyDataStore

  implicit lazy val authInfo: AuthInfoMock = wire[AuthInfoDev]

  // wire Env
  implicit lazy val env: Env = wire[Env]

  init()

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
  lazy val apiKeyController: ApiKeyController = wire[ApiKeyController]

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

  override lazy val httpErrorHandler: HttpErrorHandler = new HttpErrorHandler {

    private lazy val defaultHandler = new DefaultHttpErrorHandler(environment,
                                                                  configuration,
                                                                  sourceMapper,
                                                                  Some(router))

    def acceptedContent(request: RequestHeader,
                        statusCode: Int,
                        message: String,
                        uuid: String): Result = {
      request.headers.get("Accept") match {
        case Some(accept) if accept.contains("text/html") =>
          Results.Status(statusCode)(views.html.error(message, uuid))
        case _ =>
          Results.Status(statusCode)(
            Json.obj("error" -> Json.obj("id" -> uuid, "message" -> message)))
      }
    }

    override def onClientError(request: RequestHeader,
                               statusCode: Int,
                               mess: String): Future[Result] = {
      val uuid =
        java.util.UUID.nameUUIDFromBytes(new SecureRandom().generateSeed(16))

      val message =
        Option(mess).filterNot(_.trim.isEmpty).getOrElse("An error occured")
      val realMessage =
        s"Client Error: $message on ${request.uri} ($statusCode)"
      Logger.error(s"$uuid - $realMessage")

      if (env.isDev) {
        defaultHandler.onClientError(request, statusCode, s"[$uuid] $message")
      } else {
        val maybeState =
          request.headers.get(env.config.filter.otoroshi.headerGatewayState)
        FastFuture.successful(
          acceptedContent(request, statusCode, realMessage, uuid.toString)
            .withHeaders(
              "X-Error" -> s"$uuid - $realMessage",
              env.config.filter.otoroshi.headerGatewayStateResp -> maybeState
                .getOrElse("--")
            ))
      }
    }

    override def onServerError(request: RequestHeader,
                               exception: Throwable): Future[Result] = {
      val uuid =
        java.util.UUID.nameUUIDFromBytes(new SecureRandom().generateSeed(16))
      val message = s"Server Error: ${exception.getMessage}"

      Logger.error(s"$uuid - $message", exception)
      if (env.isDev) {
        defaultHandler.onServerError(request, exception)
      } else {
        val maybeState =
          request.headers.get(env.config.filter.otoroshi.headerGatewayState)
        FastFuture.successful(
          acceptedContent(request, 500, message, uuid.toString)
            .withHeaders(
              "X-Error" -> s"$uuid - $message",
              env.config.filter.otoroshi.headerGatewayStateResp -> maybeState
                .getOrElse("--")
            ))
      }
    }
  }

  override def httpFilters: Seq[EssentialFilter] = {
    Seq(securityFilter, gzipFilter)
  }

  implicit val config: Configuration = context.initialConfiguration
  val starter: Starter = wire[Starter]

  starter.initialize()

  lazy val catchup = wire[CatchupNioEventService]
  catchup.catchupNioEventScheduler()
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

class Auth0Router(auth0Controller: Auth0Controller,
                  apiKeyController: ApiKeyController)
    extends SimpleRouter {
  import play.api.routing.sird._

  override def routes: Router.Routes = {
    case GET(
        p"/api/auth0/callback" ? q_o"code=${codeOpt}" ? q_o"state=${stateOpt}") =>
      auth0Controller.callback(codeOpt, stateOpt)
    case GET(p"/auth0/login") =>
      auth0Controller.login
    case GET(p"/auth0/logout") =>
      auth0Controller.logout

    case GET(
        p"/api/apikeys" ? q_o"page=${int(maybePage)}" ? q_o"pageSize=${int(maybePageSize)}") =>
      val page = maybePage.getOrElse(0)
      val pageSize = maybePageSize.getOrElse(10)
      apiKeyController.findAll(page, pageSize)
    case GET(p"/api/apikeys/${apiKeyId}") =>
      apiKeyController.find(apiKeyId)
    case POST(p"/api/apikeys") =>
      apiKeyController.create()
    case PUT(p"/api/apikeys/${apiKeyId}") =>
      apiKeyController.update(apiKeyId)
    case DELETE(p"/api/apikeys/${apiKeyId}") =>
      apiKeyController.delete(apiKeyId)

  }
}

class NioAccountRouter(nioAccountController: NioAccountController,
                       apiKeyController: ApiKeyController,
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

    case GET(
        p"/api/apikeys" ? q_o"page=${int(maybePage)}" ? q_o"pageSize=${int(maybePageSize)}") =>
      val page = maybePage.getOrElse(0)
      val pageSize = maybePageSize.getOrElse(10)
      apiKeyController.findAll(page, pageSize)
    case GET(p"/api/apikeys/${apiKeyId}") =>
      apiKeyController.find(apiKeyId)
    case POST(p"/api/apikeys") =>
      apiKeyController.create()
    case PUT(p"/api/apikeys/${apiKeyId}") =>
      apiKeyController.update(apiKeyId)
    case DELETE(p"/api/apikeys/${apiKeyId}") =>
      apiKeyController.delete(apiKeyId)
  }
}
