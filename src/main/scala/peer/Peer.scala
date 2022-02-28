package peer

import akka.actor.ActorSelection
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import userData.{LocatorInfo, LoginProcedure, State}
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.ask
import akka.remote.transport.ActorTransportAdapter.AskTimeout

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// For hashing: Based on https://stackoverflow.com/questions/46329956/how-to-correctly-generate-sha-256-checksum-for-a-string-in-scala 
import java.math.BigInteger
import java.security.MessageDigest

object FileTypes extends Enumeration {
  type FileType = Value

  val Index, List, FriendList, FirstName, LastName, BirthDay, City, WallIndex, WallEntry = Value
}

case class DhtFileEntry(hGUID: String, locator: String, version: Int) // <hGUID>#<locator>#<version>

trait File

case class WallEntry(index: Int, text: String) extends File

/**
 *
 * @param lastIndex the index of the most recent entry.
 */
case class WallIndex(owner: String, lastIndex: Int, entries: ListBuffer[String]) extends File

class Peer(context: ActorContext[PeerMessage], mail: String) extends AbstractBehavior[PeerMessage](context) {
  //Used SHA-256 because MD5 not secure (Check with linux: "echo -n "Test@test.com" | openssl dgst -sha256")
  val hashedMail = String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(mail.getBytes("UTF-8"))))
  val files: mutable.Map[String, Any] = mutable.Map()

  val WALL_INDEX_KEY = s"${hashedMail}@wi"

  type callback = PeerMessage => Unit

  val callBacks : mutable.Map[String,callback] = mutable.Map()

  // put location in the dht with the hash being the key
  files.put(WALL_INDEX_KEY, WallIndex(hashedMail, -1, ListBuffer.empty))
  dht.LocalDht.append(WALL_INDEX_KEY, DhtFileEntry(hashedMail, context.self.path.toString, 0))
  dht.LocalDht.put(hashedMail, context.self.path.toString)

  def wallEntryKey(index: Int): String = s"${hashedMail}@we${index}"

  def wallIndex = files(WALL_INDEX_KEY).asInstanceOf[WallIndex]

  def addToWall(text: String): Unit = {
    val newIndex = wallIndex.lastIndex + 1
    val entryKey = wallEntryKey(newIndex)
    wallIndex.entries.append(entryKey)
    files.put(entryKey, WallEntry(newIndex, text))
    dht.LocalDht.append(entryKey, DhtFileEntry(hashedMail, context.self.path.toString, 0)) // for now assume not versioning

    // increment index
    files.put(WALL_INDEX_KEY, wallIndex.copy(lastIndex = newIndex))
  }

  /**
   * Add a file to our local storage and update the dht.
   */
  def addFile(fileName: String,file: File): Unit ={
    files.put(fileName, file)
    dht.LocalDht.append(fileName, DhtFileEntry(hashedMail, context.self.path.toString, 0)) // for now assume not versioning
  }

  /**
   * Get a reference to a peer from its path.
   */
  def getPeerRef(path: String): ActorSelection = {
    context.system.classicSystem.actorSelection(path)
  }



  override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
    context.log.info(s"Received message $msg")

    val processMessage = msg match {
      case Login(location) => {
        LoginProcedure.start(location, hashedMail)
        println(dht.LocalDht.get(hashedMail))
      }
    }

    msg match {
      case AddWallEntry(text) => {
        addToWall(text)

      }
      case FileRequest(name,version,ref) => {
        files.get(name) match {
          case Some(value) if value.isInstanceOf[File] => ref ! FileResponse(200,name,version,Some(value.asInstanceOf[File]))
          case None => ref ! FileResponse(404,name,version,None)
        }

      }
      // If we get a response, store it locally and send it if it was requested.
      case r@FileResponse(code,name,version,file) if code == 200 => {
        callBacks(name)(r)
        file match {
          case Some(value) => addFile(name,value)
        }
      }
      case PeerCmd(cmd) => {
        cmd match {
          case AddToWall(user, text) => {
            dht.LocalDht.get(user) match {
              case Some(path: String) => getPeerRef(path) ! AddWallEntry(text)
              case None => ()
            }
          }
          case GetFile(fileName, replyTo) => dht.LocalDht.getAll(fileName) match {
            case Some(DhtFileEntry(hGUID, locator, version) :: _) =>
              callBacks.put(fileName,{msg => replyTo ! msg})
              getPeerRef(locator) ! FileRequest(fileName, version, this.context.self) // actor classic kinda screws up. Instead just send a file request and then handle in explicitly
          }
          case _ => ()
        }
      }
    }
    this


  }
}

object Peer{
  def apply(mail:String): Behavior[PeerMessage] = {
    Behaviors.setup(context => {
      new Peer(context, mail)
    })
  }
}