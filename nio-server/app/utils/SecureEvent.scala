package utils

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, RestartSettings}
import org.apache.pekko.stream.scaladsl.{RestartSource, Sink}
import configuration.Env
import messaging.KafkaMessageBroker
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationDouble

trait TSecureEvent {}

class SecureEvent(
    env: Env,
    kafkaMessageBroker: KafkaMessageBroker,
    actorSystem: ActorSystem,
    implicit val executionContext: ExecutionContext
) extends TSecureEvent {

  implicit val materializer: Materializer = Materializer(actorSystem)
  private lazy val kafkaConfig      = env.config.kafka

  private def run(): Future[Done] = {
    NioLogger.info("secure event : run")

    val source = kafkaMessageBroker.readAllEvents(kafkaConfig.eventsGroupIn, kafkaConfig.eventsGroupDuration)

    // Auto restart
    RestartSource
      .withBackoff(RestartSettings(
        3.seconds,  // min backoff
        30.seconds, // max backoff
        0.2         // adds 20% "noise" to vary the intervals slightly
      )) { () =>
        NioLogger.debug("secure event : restart source")
        source
      }
      .runWith(Sink.ignore)

  }

  def initialize(): Any =
    if (env.config.recordManagementEnabled && env.config.s3ManagementEnabled) {
      run()
    }
}
