import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import dht.{GetPeerKey, LocalDHT, Wall}
import peer.{AddToWallCommand, FileRequest, GetFileCommand, PeerCmd, PeerMessage, SendMessageCommand}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.io.StdIn.readLine
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import com.typesafe.config.ConfigFactory


/**
 * Based on LoginProcedure
 * All the registered users will store
 * (hashedMail, List(LocatorInfo(location, IP, port, state))
 * in DHT
 */

object Guardian {

  def apply(): Behavior[REPLCommand] = Behaviors.setup { context =>
    /**
     * create a map of peers, [a local backup for Guardian to speed up lookup]
     * s"'${user}'@${location}" -> ActorRef[PeerMessage]
     * only stores active/online users, offline users will be removed
     */
    val peers: mutable.Map[String, ActorRef[PeerMessage]] = mutable.Map()

    /**
     * get ActorRef if the message sender is now active/online
     * @param sender sender mail (not hashed)
     * @return ActorRef if exists, else None
     */
    def getPeerRefByGuardian(sender: String): Option[ActorRef[PeerMessage]] = {
      val validSenders = peers.keys.filter(k => k.startsWith(s"'${sender}'"))
      if (validSenders.isEmpty) {
        None
      } else {
        Some(peers(validSenders.head))
      }
    }

    Behaviors.receiveMessage { msg: REPLCommand =>
      msg match {

        /**
         * TODO: finish the logic here
         * 1. if any location of receiver is found active/online, send message
         * 2. if not, add to wall
         */
        case SendMessage(sender: String, receiver: String, text: String) => {
          val lookup = getPeerRefByGuardian(sender)
          lookup match {
            case Some(senderRef: ActorRef[PeerMessage]) =>
              senderRef ! PeerCmd(SendMessageCommand(receiver, text))
            case _ =>
              println(s"Sender ${sender} currently unavailable")
          }
        }

        /**
         * inspect DHT
         */
        case InspectDHT() =>
          LocalDHT.printElement()

        /**
         * TODO: The following cases
         */
        case AddWallByUser(sender: String, owner: String, text: String) =>  {
          val lookup = getPeerRefByGuardian(sender)
          lookup match {
            case Some(senderRef: ActorRef[PeerMessage]) =>
              senderRef ! PeerCmd(AddToWallCommand(owner,text))
            case _ =>
              println(s"Owner ${owner} currently unavailable")
          }
        }
        case AddWallByGuardian(owner: String, text: String) => {
          Wall.add("",owner,text)
        }
        case RequestFileByUser(requester: String, responder: String, fileName: String, version: Int) =>
          val lookup = getPeerRefByGuardian( requester)
          lookup match {
            case Some(requesterRef: ActorRef[PeerMessage]) =>
              requesterRef ! PeerCmd(GetFileCommand(fileName,null))
            case _ =>
              println(s"Peer ${requester } currently unavailable")
          }
        case RequestFileByGuardian(responder: String, fileName: String, version: Int) => {
          val lookup = getPeerRefByGuardian( responder)
          lookup match {
            case Some(senderRef: ActorRef[PeerMessage]) =>
              implicit val system = context.system
              implicit val timeout : Timeout = 1.seconds
              val future = senderRef.ask(ref => FileRequest(fileName,0,ref))
              implicit  val ec = system.executionContext
              future.onComplete(println)
            case _ =>
              println(s"Peer ${responder} currently unavailable")
          }

        }
        case SendFileByUser(sender: String, receiver: String, fileName: String, version: Int) => ()
        case SendFileByGuardian(receiver: String, fileName: String, version: Int) => ()

        /**
         * Login
         * Every login spawns a new context
         */
        case Login(user: String, location: String) =>
          val peerKey = GetPeerKey(user, location)
          // I have to take `peerKey` out as a separate variable or it may throw an error
          // with brackets [] added on both sides of the string, probably because it's not thread-safe

          //val peerRef = context.spawn(peer.Peer(user), peerKey)
          val peerRef = context.system.systemActorOf(peer.Peer(user),peerKey)
          println(peerRef.path.address)
          println(peerRef.path)
          /**
           * peerKey -> peerRef stored in `peers`
           */
          peers.put(peerKey, peerRef)
          /**
           * peerKey -> peerPaths stored in `LocalDHT`
           */
          val peerPath = peerRef.path.toString
          LocalDHT.put(peerKey, peerPath)
          peerRef ! peer.Login(location)
        /**
         * TODO: Logout
         */
        case Logout(user: String, location: String) => ()
        case _ => ()
      }
      Behaviors.same
    }

  }
}


object main extends App {
  // Call the apply method of the Guardian object with parameter 2
  // start the ActorSystem named "guardian"

  val config = ConfigFactory.load("remote_application")

  val guardian = ActorSystem(Guardian(), "guardian",config)

  println(guardian.path)

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


