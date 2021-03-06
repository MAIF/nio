package messaging

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import configuration.{Env, KafkaConfig}
import models.NioEvent
import utils.NioLogger
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

class KafkaMessageBroker(actorSystem: ActorSystem)(implicit context: ExecutionContext, env: Env) {
  implicit val mat: Materializer = Materializer(actorSystem)

  private lazy val kafka: KafkaConfig = env.config.kafka

  private lazy val consumerSettings =
    KafkaSettings.consumerSettings(actorSystem, kafka)

  private val toNioEvent: Flow[String, Option[NioEvent], NotUsed] = Flow[String]
    .map { str =>
      NioLogger.info(s"read string from kafka $str")
      Json.parse(str)
    }
    .map(json =>
      NioEvent.fromJson(json) match {
        case None    =>
          NioLogger.error(s"Error deserializing event of type ${json \ "type"}")
          None
        case Some(e) =>
          Some(e)
      }
    )

  def run(): Source[NioEvent, NotUsed] = {
    NioLogger.info("run")
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(kafka.topic))
      .map(msg => msg.value())
      .via(toNioEvent)
      .filter(_.isDefined)
      .map(_.get)
      .mapMaterializedValue(_ => NotUsed)
      .alsoTo(Sink.foreach(e => NioLogger.debug(s"hey I find a new message ${e.tYpe}")))
  }
}
