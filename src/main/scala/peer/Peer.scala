package peer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import logic.async_messages.AsyncMessage
import logic.async_messages.AsyncMessage.OfflineMessage
import dht.DistributedDHT
import logic.wall.FileOperations.DHTFileEntry
import logic.login.{LocatorInfo, LoginProcedure, LogoutProcedure, State}
import logic.wall.Wall.WallEntry
import logic.wall.{File, FileOperations, Wall}
import services.{Encrypt, GetPathByMail, GetPeerRef}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object PeerWall {
  case class WallIndex(owner: String, lastIndex: Int, entries: ListBuffer[String]) extends File
}


object Peer {
  def apply(mail: String, dhtNode: DistributedDHT): Behavior[PeerMessage] = {
    Behaviors.setup(context => {
      new PeerBehavior(context, mail, dhtNode)
    })
  }

  class PeerBehavior(context: ActorContext[PeerMessage], mail: String, dhtNode: DistributedDHT) extends AbstractBehavior[PeerMessage](context) {

    private val hashedMail = Encrypt(mail)
    // the peer location, e.g., home / phone
    var location : String =""
    // the peer path, like akka://guadian@localhost/....
    var path : String =""

    /**
     * instance variable - a mutable map to store all the local files
     * TODO (if time allows): connects to JSON files or databases to fetch a peer's files
     * Now just create a new Map every time
     */
    val localFiles: mutable.Map[String, File] = mutable.Map()
    val WALL_INDEX_KEY = s"$hashedMail@wi"
    var wallIndex: PeerWall.WallIndex = PeerWall.WallIndex(hashedMail, -1, ListBuffer.empty)

    /**
     *
     * @param sender the hashed mail of the person who added the entry.
     */
    def addToWall(sender:String, text: String): Unit = {
      val newIndex = wallIndex.lastIndex + 1
      val entryKey = Wall.getWallEntryKey(mail,newIndex)
      wallIndex.entries.append(entryKey)
      localFiles.put(entryKey, WallEntry(newIndex,sender, text))                  // is this ok?
      dhtNode.append(entryKey, DHTFileEntry(hashedMail, LocatorInfo(location,"","",State.active,path), 0)) // for now assume not versioning

      // increment index
      localFiles.put(WALL_INDEX_KEY, wallIndex.copy(lastIndex = newIndex))

      localFiles.put(WALL_INDEX_KEY, wallIndex)
      dhtNode.append(WALL_INDEX_KEY, DHTFileEntry(hashedMail, LocatorInfo(location,"","",State.active,path), 0)) // for now assume not versioning
    }


    /**
     * message handler
     *
     * @param msg incoming Akka message
     */
    override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
      context.log.info(s"$mail - received message: $msg")
      msg match {


        case AddWallEntry(sender,text) =>
          addToWall(sender,text)


        case Message(sender, text, ack) =>
          if (ack) {
            context.log.info(s"$sender send an ack")
          } else {
            context.log.info(s"From: $sender | Message: $text")
            new GetPathByMail(sender, dhtNode,{
              case Some(senderPath: String) =>
                GetPeerRef(context, senderPath) ! Message(mail, "I got your message", ack = true)
              case _ =>
                AsyncMessage.add(mail, sender, "I got your message", ack = true, dhtNode)
            }).get()
          }


        case Login(location, path) =>
          AsyncMessage.load(context, mail, dhtNode)
          this.location = location
          this.path = path
          val loginProcedure = new LoginProcedure(location, hashedMail, path, dhtNode)
          loginProcedure.start()


        case Logout(location) =>
          val logoutProcedure = new LogoutProcedure(location, hashedMail, dhtNode)
          logoutProcedure.start()


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
//              FileOperations.add(hashedMail, context.self.path.toString, 0, file,dhtNode)
            case None => ()
          }


        case PeerCmd(cmd) =>
          cmd match {


            // command the current peer (as sender) to put text on receiver's wall
            case AddToWallCommand(receiver, text) =>
              new GetPathByMail(receiver, dhtNode,{
               case Some(receiverPath: String) =>
                 services.GetPeerRef(context, receiverPath) ! AddWallEntry(mail,text)
               case None => AsyncMessage.AddWallEntry(mail,receiver,text,dhtNode)
             }).get()


            case AddOfflineMessage(receiver, text, ack) =>
              AsyncMessage.add(mail, receiver, text, ack, dhtNode)


            // command the current peer to request a file
            case GetFileCommand(fileName, replyTo) =>
              val realFileName = fileName.dropRight(3)
              val a = Encrypt(realFileName) + "@wi"
              dhtNode.getAll(a,{
                case Some(FileOperations.DHTFileEntry(hashedMail, path, version)::xs) =>
                  // actor classic kinda screws up. Instead just send a file request and then handle in explicitly
                  services.GetPeerRef(context, path.path) ! FileRequest(a, version, context.self)
            })


            // command the current peer to send message
            case SendMessageCommand(receiver, text) =>
              new GetPathByMail(receiver,dhtNode,{
                case Some(receiverPath: String) =>
                  services.GetPeerRef(context, receiverPath) ! Message(mail, text, ack = false)
                case _ =>
                  context.self ! PeerCmd(AddOfflineMessage(receiver, text, ack = false))
              }).get()
            case _ => ()
          }


        case Notification(content) =>
          content match {


            case OfflineMessage(sender: String, content: String, ack: Boolean) =>
              context.self ! Message(sender, content, ack)


            case WallEntry(_,sender,content) => addToWall(sender,content)
          }
      }
      this
    }
  }
}