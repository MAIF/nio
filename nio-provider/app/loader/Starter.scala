package loader

import actor.KafkaActor
import org.apache.pekko.actor.ActorSystem
import messaging.KafkaMessageBroker

class Starter(kafkaMessageBroker: KafkaMessageBroker,
              actorSystem: ActorSystem) {

  def run(): Unit = {
    actorSystem.actorOf(KafkaActor.props(kafkaMessageBroker.run()))
  }

}
