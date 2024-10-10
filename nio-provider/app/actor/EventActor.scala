package actor

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import models.NioEvent
import play.api.libs.json.Json
import utils.NioLogger

object EventActor {
  def props(out: ActorRef): Props = Props(new EventActor(out))
}

class EventActor(out: ActorRef) extends Actor {

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[NioEvent])

  override def receive: Receive = { case e: NioEvent =>
    NioLogger.info(s"Event actor received a message : ${e.tYpe}")
    out ! Json.stringify(e.asJson())
  }

}
