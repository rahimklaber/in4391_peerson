package peer

import akka.actor.typed.ActorRef

// Info needed for chat messages
trait PeerMessage

case class Message(nonHashedSender: String, message : String, ack: Boolean) extends PeerMessage 

case class Example(text: String) extends PeerMessage

case class PeerCmd(cmd: Command) extends PeerMessage // for sending commands to the peer

case class AddWallEntry(text: String) extends PeerMessage

case class FileRequest(fileName: String, version: Int, replyTo : ActorRef[PeerMessage]) extends PeerMessage// for now assume version == 0

/**
 *
 * @param code response code. Kinda like http response codes.
 */
case class FileResponse(code : Int,fileName : String,version : Int, file: Option[File]) extends PeerMessage


trait Command

case class GetFile(fileName: String, replyTo : ActorRef[PeerMessage]) extends Command

case class AddToWall(user: String,text: String) extends Command

case class Login(location: String) extends PeerMessage
