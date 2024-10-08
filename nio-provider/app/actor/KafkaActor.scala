package actor

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
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
