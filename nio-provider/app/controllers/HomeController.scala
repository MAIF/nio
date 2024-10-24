package controllers

import org.apache.pekko.actor.ActorSystem
import auth.AuthActionWithEmail
import configuration.Env
import messaging.KafkaMessageBroker
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class HomeController(val AuthAction: AuthActionWithEmail,
                     val cc: ControllerComponents,
                     val env: Env,
                     val actorSystem: ActorSystem,
                     val kafkaMessageBroker: KafkaMessageBroker,
                     implicit val ec: ExecutionContext)
    extends AbstractController(cc) {

  def index(): Action[AnyContent] = AuthAction { implicit req =>
    Ok(views.html.index(env, req.email, env.config.websocketHost))
  }

  def indexOther(): Action[AnyContent] = index()

  def otherRoutes(route: String): Action[AnyContent] = AuthAction { implicit req =>
    Ok(views.html.index(env, req.email, env.config.websocketHost))
  }

}
