package peer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


class Peer(context: ActorContext[PeerMessage]) extends AbstractBehavior[PeerMessage](context) {


  override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
    context.log.info(s"Received message $msg")
    msg match {
      case _ => this
    }
  }
}

object Peer{
  def apply(): Behavior[PeerMessage] = {
    Behaviors.setup(context => new Peer(context))
  }
}