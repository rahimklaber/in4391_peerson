package peer

import akka.actor.typed.ActorRef

trait Command

/**
 * Command
 */

case class GetFileCommand(fileName: String, replyTo: ActorRef[PeerMessage]) extends Command
case class AddToWallCommand(user: String, text: String) extends Command
