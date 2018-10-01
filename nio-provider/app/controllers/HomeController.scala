package controllers

import akka.actor.ActorSystem
import auth.AuthActionWithEmail
import configuration.Env
import messaging.KafkaMessageBroker
import models.NioEvent
import play.api.libs.EventSource
import play.api.libs.EventSource.{EventDataExtractor, EventNameExtractor}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class HomeController(val AuthAction: AuthActionWithEmail,
                     val cc: ControllerComponents,
                     val env: Env,
                     val actorSystem: ActorSystem,
                     val kafkaMessageBroker: KafkaMessageBroker,
                     implicit val ec: ExecutionContext)
    extends AbstractController(cc) {

  def index() = AuthAction { implicit req =>
    Ok(views.html.index(env, req.email))
  }

  def indexOther() = index()

  def otherRoutes(route: String) = AuthAction { implicit req =>
    Ok(views.html.index(env, req.email))
  }

  private implicit val nameExtractor = EventNameExtractor[NioEvent](_ => None)
  private implicit val dataExtractor =
    EventDataExtractor[NioEvent](event => Json.stringify(event.asJson))

  def readEvent() = AuthAction { implicit req =>
    Ok.chunked(kafkaMessageBroker.run() via EventSource.flow)
      .as("text/event-stream")

  }

}
