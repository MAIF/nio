package actor

import akka.NotUsed
import akka.actor.{Actor, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import models.NioEvent
import utils.NioLogger

object KafkaActor {
  def props(source: Source[NioEvent, NotUsed]) = Props(new KafkaActor(source))
}

class KafkaActor(source: Source[NioEvent, NotUsed]) extends Actor {

  implicit val mat: Materializer = Materializer(context.system)

  override def preStart() =
    source.runForeach { message =>
      self ! message
    }

  override def receive: Receive = { case k: NioEvent =>
    NioLogger.info(s"Kafka actor received a message of type  ${k.tYpe} and transfert message to context")
    context.system.eventStream.publish(k)
  }
}
