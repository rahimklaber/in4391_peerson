import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import dht.LocalDHT
import peer.{Message, Peer, PeerMessage}

import scala.collection.mutable
import scala.io.StdIn.readLine


object Guardian {
  /**
   * user and location as a key of peers
   * @param user mail (not hashed)
   * @param location location string
   */
  case class UserAndLocation(user: String, location: String)

  def apply(n: Int): Behavior[REPLCommand] = Behaviors.setup { context =>
    // create a map of peers
    // (user, string) -> ActorRef[PeerMessage]
    var peers: mutable.Map[UserAndLocation, ActorRef[PeerMessage]] = mutable.Map()

    Behaviors.receiveMessage { msg: REPLCommand =>
      msg match {

        /**
         * TODO: improve SendMessage Object under peer/
         * 1. if any location of receiver is found active/online, send message
         * 2. if not, add to wall
         */
        case SendMessage(sender: String, receiver: String, text: String) => ()

        /**
         * inspect DHT
         */
        case InspectDHT() =>
          LocalDHT.printElement()

        /**
         * test purpose: add wall by user
         */
        case AddWallByUser(sender: String, owner: String, text: String) => ()
        case AddWallByGuardian(owner: String, text: String) => ()
        case RequestFileByUser(requester: String, responder: String, fileName: String, version: Int) => ()
        case RequestFileByGuardian(responder: String, fileName: String, version: Int) => ()
        case SendFileByUser(sender: String, receiver: String, fileName: String, version: Int) => ()
        case SendFileByGuardian(receiver: String, fileName: String, version: Int) => ()
        case Login(user: String, location: String) =>
          val userKey = UserAndLocation(user, location)
          val actorName : String = s"'${user}'@${location}"
          val userRef = context.spawn(Peer(user), actorName)
          peers.put(userKey, userRef)
          userRef ! peer.Login(location)
        case Logout(user: String, location: String) => ()  // TODO: finish the logic
        case _ => ()
      }
      Behaviors.same
    }

  }
}


object main extends App {
  // Call the apply method of the Guardian object with parameter 2
  // start the ActorSystem named "guardian"
  val guardian = ActorSystem(Guardian(2), "guardian")

  /**
   * receiving commands from REPL
   */
  while (true) {
    println("Guardian waits for your command")
    val input = readLine.strip.split(" ")
    val command: REPLCommand = input.head match {
      case "send-message" =>
        SendMessage(readLine("sender: ").strip, readLine("receiver: ").strip, readLine("text: ").strip)
      case "inspect-dht" =>
        InspectDHT()
      case "add-wall-by-user" =>
        AddWallByUser(readLine("sender: ").strip, readLine("owner: ").strip,  readLine("text: ").strip)
      case "add-wall-by-guardian" =>
        AddWallByGuardian(readLine("owner: ").strip, readLine("text: ").strip)
      case "request-file-by-user" =>
        RequestFileByUser(readLine("requester: ").strip, readLine("responder: ").strip,
          readLine("fileName: ").strip, version = 0)
      case "request-file-by-guardian" =>
        RequestFileByGuardian(readLine("responder: ").strip, readLine("fileName: ").strip, version = 0)
      case "send-file-by-user" =>
        SendFileByUser(readLine("sender: ").strip, readLine("receiver: ").strip,
          readLine("fileName: ").strip, version = 0)
      case "send-file-by-guardian" =>
        SendFileByGuardian(readLine("receiver: ").strip, readLine("fileName: ").strip, version = 0)
      case "login" =>
        Login(readLine("email: ").strip, readLine("location: ").strip)
      case "logout" =>
        Logout(readLine("email: ").strip, readLine("location: ").strip)
      case "exit" =>
        sys.exit
      case _ => new REPLCommand {}
    }
    guardian ! command
  }
}


