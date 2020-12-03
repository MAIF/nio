package utils

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Sink}
import configuration.Env
import messaging.KafkaMessageBroker

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationDouble

trait TSecureEvent {}

class SecureEvent(
    env: Env,
    kafkaMessageBroker: KafkaMessageBroker,
    actorSystem: ActorSystem,
    implicit val executionContext: ExecutionContext
) extends TSecureEvent {

  implicit val materializer = Materializer(actorSystem)
  lazy val kafkaConfig      = env.config.kafka

  private def run() = {
    NioLogger.info("secure event : run")

    val source = kafkaMessageBroker.readAllEvents(kafkaConfig.eventsGroupIn, kafkaConfig.eventsGroupDuration)

    // Auto restart
    RestartSource
      .withBackoff(
        3.seconds,  // min backoff
        30.seconds, // max backoff
        0.2         // adds 20% "noise" to vary the intervals slightly
      ) { () =>
        NioLogger.debug("secure event : restart source")
        source
      }
      .runWith(Sink.ignore)

  }

  def initialize() =
    if (env.config.recordManagementEnabled && env.config.s3ManagementEnabled) {
      run()
    }
}
