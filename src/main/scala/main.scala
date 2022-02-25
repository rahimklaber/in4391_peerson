import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import peer.{Example, PeerMessage}

object Guardian {
  case class Empty()

  def apply(n: Int): Behavior[Empty] = Behaviors.setup { context =>
    var peers  : List[ActorRef[PeerMessage]]= List.empty
    (0 until n).foreach(i => {
      peers = context.spawn(peer.Peer(s"${i}"), s"peer${i}") :: peers
    })

    // For now you can put send messages to peers here.
    // For example :
    // peers.head ! Example("hi")

    Behaviors.receiveMessage { message: Empty =>

      Behaviors.same
    }
  }
}


object main extends App {
  val guardian = ActorSystem(Guardian(2), "guardian")
}
