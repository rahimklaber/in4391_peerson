package peer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.collection.mutable


case class WallItem(creator: String, text: String)

case class Wall(owner: String, items: List[WallItem])

class Peer(context: ActorContext[PeerMessage]) extends AbstractBehavior[PeerMessage](context) {

  val files : mutable.Map[String,Any] = mutable.Map()


  override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
    context.log.info(s"Received message $msg")
    msg match {
      case PeerCmd(cmd) => {
        cmd match {
          case AddToWall(user,text) => dht.LocalDht.put(user,Wall(user,WallItem("test",text) :: List.empty))
          case _ => ()
        }

        this
    }

      case _ => this
    }


  }
}

object Peer{
  def apply(): Behavior[PeerMessage] = {
    Behaviors.setup(context => new Peer(context))
  }
}