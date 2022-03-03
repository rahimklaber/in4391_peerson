import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import peer.{Message, Peer, PeerMessage}

import scala.io.StdIn.readLine


object Guardian {
  def apply(n: Int) = Behaviors.setup { context =>
    // create a list of ActorRef's
    var peers: List[ActorRef[PeerMessage]] = List.empty

    // create n Peer actors with an int as mail
    (0 until n).foreach(i => {
      peers = context.spawn(Peer(s"Peer$i@mail.com"), s"peer$i") :: peers
    })


  }
}


object main extends App {
  // Call the apply method of the Guardian object with parameter 2
  // start the ActorSystem named "guardian"
  val guardian = ActorSystem(Guardian(2), "guardian")
}


