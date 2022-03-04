package peer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import dht.{Encrypt, File, FileOperations, LocalDHT, Wall}
import userData.LoginProcedure

import scala.collection.mutable

object Peer {
  def apply(mail: String): Behavior[PeerMessage] = {
    Behaviors.setup(context => {
      new PeerBehavior(context, mail)
    })
  }

  class PeerBehavior(context: ActorContext[PeerMessage], mail: String) extends AbstractBehavior[PeerMessage](context) {

    /**
     * instance variable - hashed email
     */
    private val hashedMail = Encrypt(mail)

    /**
     * instance variable - a mutable map to store all the local files
     * TODO (if time allows): connects to JSON files or databases to fetch a peer's files
     * Now just create a new Map every time
     */
    val localFiles: mutable.Map[String, File] = mutable.Map()

    /**
     * message handler
     * @param msg incoming Akka message
     */
    override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
      context.log.info(s"received message: $msg")
      msg match {
        case Message(sender, text, ack) =>
          if (ack) {
            context.log.info(s"$sender send an ack")
          } else {
            context.log.info(s"From: $sender | Message: $text")
            SendMessage(context, sender, "I got your message", mail, ack = true)
          }

        case Login(location) =>
          // TODO: modify LogInProcedure.start(), which overwrites the previous storage in DHT
          LoginProcedure.start(location, hashedMail)
          println(LocalDHT.get(hashedMail))

        case FileRequest(fileName, version, replyTo) =>
          localFiles.get(fileName) match {
            case Some(value) if value.isInstanceOf[File] =>
              replyTo ! FileResponse(200, fileName, version, Some(value), context.self)
            case Some(_) => ()
            case None => replyTo ! FileResponse(404, fileName, version, None, context.self)
          }

        // If we get a response, store it locally.
        case FileResponse(code, fileName, version, received, from) if code == 200 =>
          received match {
            case Some(file) =>
              localFiles.put(fileName, file)
              FileOperations.add(hashedMail, context.self.path.toString, 0, file)
            case None => ()
          }

        case PeerCmd(cmd) =>
          cmd match {
            // command the current peer (as sender) to put text on receiver's wall
            case AddToWallCommand(receiver, text) => {
              LocalDHT.get(receiver) match {
                case Some(receiverPath: String) => Wall.add(mail, receiver, text)
                case None => ()
              }
            }

            // command the current peer to request a file
            case GetFileCommand(fileName, replyTo) => LocalDHT.get(fileName) match {
              case Some(FileOperations.DHTFileEntry(hashedMail, locator, version)) =>
                // actor classic kinda screws up. Instead just send a file request and then handle in explicitly
                GetPeerRef(context, locator) ! FileRequest(fileName, version, context.self)
            }

            case _ => ()
          }
      }
      this
    }
  }
}