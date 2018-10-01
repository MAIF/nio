package controllers

import actor.EventActor
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import auth.AuthActionWithEmail
import configuration.Env
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}

import scala.concurrent.ExecutionContext

class UserDataController(val AuthAction: AuthActionWithEmail,
                         val cc: ControllerComponents,
                         val env: Env,
                         implicit val actorSystem: ActorSystem,
                         implicit val ec: ExecutionContext)
    extends AbstractController(cc) {
  implicit val mat = ActorMaterializer()(actorSystem)

  def listen() = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      EventActor.props(out)
    }
  }

}
