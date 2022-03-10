package peer

import akka.actor.typed.scaladsl.ActorContext
import dht.{DistributedDHT, Encrypt, GetPathByMail}

object SendChatMessage {
  /**
   * method for chatting
   * @param context ActorContext
   * @param sender sender email (not hashed)
   * @param receiver receiver email (not hashed)
   * @param text message text
   * @param ack ack
   */
  def apply(context: ActorContext[PeerMessage], sender: String, receiver: String, text: String, ack: Boolean, dhtNode: DistributedDHT): Unit = {
    val pathLookUp = GetPathByMail(receiver, dhtNode)
    pathLookUp match {
      case None => println(s"User: $receiver, not found in DHT")
      case Some(receiverPath) =>
        try {
          // looks up user and sends a message back
          val receiverRef = GetPeerRef(context, receiverPath)
          receiverRef ! Message(sender, text, ack)
        } catch {
          // catches all errors if DHT has stored value, but user offline
          case _: Throwable => println("Cannot find the user!")
        }
    }
  }
}
