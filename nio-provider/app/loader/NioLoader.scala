package loader

import actor.{EventActor, KafkaActor}
import akka.actor.{ActorRef, ActorSystem}
import auth._
import com.softwaremill.macwire.wire
import configuration._
import controllers._
import filters.OtoroshiFilter
import messaging.KafkaMessageBroker
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers.Default
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.filters.gzip._
import router.Routes

class NioLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new NioComponents(context).application
  }
}

class NioComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with HttpFiltersComponents
    with AssetsComponents
    with GzipFilterComponents {

  implicit val system: ActorSystem = actorSystem

  // wire Env
  implicit lazy val env: Env = wire[Env]

  // wire Kafka
  implicit lazy val kafkaMessageBroker: KafkaMessageBroker =
    wire[KafkaMessageBroker]

  // wire Action
  lazy val bodyParserDefault: Default =
    wire[Default]
  lazy val authAction: AuthAction = wire[AuthAction]
  lazy val authActionWithEmail: AuthActionWithEmail =
    wire[AuthActionWithEmail]

  // wire Controller
  lazy val homeController: HomeController = wire[HomeController]
  lazy val userDataController: UserDataController = wire[UserDataController]

  lazy val starter: Starter = wire[Starter]

  override def router: Router = {
    lazy val prefix: String = "/"

    wire[Routes]
  }

  override def httpFilters: Seq[EssentialFilter] = {
    Seq(new OtoroshiFilter(env), gzipFilter)
  }

  starter.run()

}
