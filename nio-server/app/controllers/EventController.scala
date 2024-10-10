package controllers

import auth.SecuredAuthContext

import messaging.KafkaMessageBroker
import models.NioEvent
import play.api.libs.EventSource
import play.api.libs.EventSource.{EventDataExtractor, EventNameExtractor}
import play.api.libs.json.Json
import play.api.mvc.{Action, ActionBuilder, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class EventController(
    val AuthAction: ActionBuilder[SecuredAuthContext, AnyContent],
    val cc: ControllerComponents,
    val broker: KafkaMessageBroker
)(implicit val ec: ExecutionContext)
    extends ControllerUtils(cc) {

  private implicit val nameExtractor: EventNameExtractor[NioEvent] = EventNameExtractor[NioEvent](_ => None)
  private implicit val dataExtractor: EventDataExtractor[NioEvent] = EventDataExtractor[NioEvent](event => Json.stringify(event.asJson()))

  def events(tenant: String): Action[AnyContent] = AuthAction { _ =>
    Ok.chunked(broker.events(tenant) via EventSource.flow)
      .as("text/event-stream")
  }
}
