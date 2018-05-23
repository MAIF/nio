package controllers

import auth.AuthAction
import javax.inject.{Inject, Singleton}
import messaging.KafkaMessageBroker
import models.NioEvent
import play.api.libs.EventSource
import play.api.libs.EventSource.{EventDataExtractor, EventNameExtractor}
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

@Singleton
class EventController @Inject()(
    val AuthAction: AuthAction,
    val cc: ControllerComponents,
    val broker: KafkaMessageBroker)(implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {

  private implicit val nameExtractor = EventNameExtractor[NioEvent](_ => None)
  private implicit val dataExtractor =
    EventDataExtractor[NioEvent](event => Json.stringify(event.asJson))

  def events(tenant: String) = AuthAction { implicit req =>
    Ok.chunked(broker.events(tenant) via EventSource.flow)
      .as("text/event-stream")
  }
}
