import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import peer.{Example, Login, PeerMessage, Test}

object Guardian {
  case class Empty()

  def apply(n: Int): Behavior[Empty] = Behaviors.setup { context =>

    // create an empty list of actor references
    var peers  : List[ActorRef[PeerMessage]]= List.empty

    // create two Peer actors with an int as mail
    (0 until n).foreach(i => {
      peers = context.spawn(peer.Peer(s"${i}"), s"peer${i}") :: peers
    })

    // For now you can put send messages to peers here.
    // For example :
    peers.head ! Login("home")

    // WHAT DOES THIS DO?
    Behaviors.receiveMessage { message: Empty =>

      Behaviors.same
    }
  }
}


object main extends App {
  // Call the apply method of the Guardian class with parameter 2 and return a Behaviour
  val guardian = ActorSystem(Guardian(2), "guardian")
}
