import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import peer.{AddToWall, AddWallEntry, Example, FileRequest, FileResponse, PeerCmd, PeerMessage}

import scala.concurrent.duration.DurationInt
import scala.io.StdIn.readLine

object Guardian {
  case class Empty()

  def apply(n: Int): Behavior[Empty] = Behaviors.setup { context =>
    var peers  : List[ActorRef[PeerMessage]]= List.empty
    (0 until n).foreach(i => {
      peers = context.spawn(peer.Peer(s"${i}"), s"peer${i}") :: peers
    })
    new Thread{
      while(true){
        val cmd = readLine().split(" ")
        cmd.head match {
          case "wall-add" => peers.head ! AddWallEntry(cmd.tail.reduce((a,b) => a + " " + b))
          case "inspect-dht" => dht.LocalDht._map.foreach(e => println(e._1))
          case "get-file" => {
            implicit val system = context.system
            implicit val timeout : Timeout = 1.seconds
            val future = peers.head.ask(ref => FileRequest(cmd.tail.head,0,ref))
            implicit  val ec = system.executionContext
            future.onComplete(println)
          }
          case _ => ()
        }
      }
    }

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
