package actor

import akka.NotUsed
import akka.actor.{Actor, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import models.NioEvent
import play.Logger

object KafkaActor {
  def props(source: Source[NioEvent, NotUsed]) = Props(new KafkaActor(source))
}

class KafkaActor(source: Source[NioEvent, NotUsed]) extends Actor {

  implicit val mat: ActorMaterializer = ActorMaterializer()(context.system)

  override def preStart() = {
    source.runForeach(
      message => {
        self ! message
      }
    )
  }

  override def receive: Receive = {
    case k: NioEvent => {
      Logger.info(
        s"Kafka actor received a message of type  ${k.tYpe} and transfert message to context")
      context.system.eventStream.publish(k)
    }
  }
}
