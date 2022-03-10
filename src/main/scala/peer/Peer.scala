package peer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import dht.{Encrypt, File, FileOperations, GetPathByMail, GetPeerKey, LocalDHT, Wall}
import userData.State.offline
import userData.{LocatorInfo, LoginProcedure}

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
            SendChatMessage(context, sender, mail, "I got your message", ack = true)
          }

        case Login(location) =>
          Wall.load(context, mail)
          LoginProcedure.start(location, hashedMail)
          println(LocalDHT.get(hashedMail))

        case Logout(location) =>
          val lookup = LocalDHT.getAll(hashedMail)
          lookup match {
            case Some(value) =>
              val locatorInfoList: List[LocatorInfo] = value.asInstanceOf[List[LocatorInfo]]
              // test case
              // - login kevin, m; login kevin, n; logout kevin, m
              val locatorInfo = locatorInfoList.filter(l => l.locator == location)
              if (locatorInfo.isEmpty) {
                println(s"user ${mail} not logged in at location ${location}")
              } else {
                val onlineLocatorInfo = locatorInfo.head
                val offlineLocatorInfo = onlineLocatorInfo.copy(state = offline)
                val newLocatorInfoList = offlineLocatorInfo :: locatorInfoList.filter(l => l.locator != location)
                LocalDHT.put(hashedMail, newLocatorInfoList.head)
                newLocatorInfoList.tail.foreach(l => LocalDHT.append(hashedMail, l))
                println(newLocatorInfoList)
                val peerKey = GetPeerKey(mail, location)
                LocalDHT.remove(peerKey)
              }
            case _ => println(s"user ${mail} not found by DHT!")
          }

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

              /**
               * TODO (if time allows): replace context.self.path.toString to locator
               */
              FileOperations.add(hashedMail, context.self.path.toString, 0, file)
            case None => ()
          }

        case PeerCmd(cmd) =>
          cmd match {
            // command the current peer (as sender) to put text on receiver's wall
            case AddToWallCommand(receiver, text) => {
              Wall.add(mail, receiver, text)
            }

            // command the current peer to request a file
            case GetFileCommand(fileName, replyTo) => LocalDHT.get(fileName) match {
              /**
               * TODO (if time allows): replace context.self.path.toString to locator
               */
              case Some(FileOperations.DHTFileEntry(hashedMail, path, version)) =>
                // actor classic kinda screws up. Instead just send a file request and then handle in explicitly
                GetPeerRef(context, path) ! FileRequest(fileName, version, context.self)
            }

            // command the current peer to send message
            case SendMessageCommand(receiver, text) =>
              val pathLookUp = GetPathByMail(receiver)
              println(pathLookUp)
              pathLookUp match {
                case Some(receiverPath: String) =>
                  GetPeerRef(context, receiverPath) ! Message(mail, text, ack = false)
                case _ =>
                  context.self ! PeerCmd(AddToWallCommand(receiver, text))
              }

            case _ => ()
          }
      }
      this
    }
  }
}