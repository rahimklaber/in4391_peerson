package peer

import akka.actor.typed.ActorRef
import dht.File

trait PeerMessage

/**
 * PeerMessage
 */

case class Message(sender: String, text: String, ack: Boolean) extends PeerMessage
case class Login(location: String, path: String) extends PeerMessage
case class Logout(location: String) extends PeerMessage
case class AddWallEntry(text: String) extends PeerMessage


// for now assume version == 0
case class FileRequest(fileName: String, version: Int, replyTo: ActorRef[PeerMessage]) extends PeerMessage

/**
 * @param code response code. Kinda like http response codes.
 */
case class FileResponse(code: Int, fileName: String, version: Int,
                        file: Option[File], from: ActorRef[PeerMessage]) extends PeerMessage

/**
 * for sending commands to the peer
 * @param cmd of Command trait
 */
case class PeerCmd(cmd: Command) extends PeerMessage





