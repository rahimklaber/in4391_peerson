package peer

import akka.actor.typed.scaladsl.ActorContext
import dht.Encrypt

object SendMessage {
  /**
   * method for chatting
   * @param sender sender email (non-hashed)
   * @param receiver receiver email (non-hashed)
   * @param text message text
   * @param ack ack
   */
  def apply(context: ActorContext[PeerMessage], sender: String, receiver: String, text: String, ack: Boolean): Unit = {
    // hashedMail -> List(receiverPath)
    val dhtLookUp = dht.LocalDHT.get(Encrypt(receiver))
    dhtLookUp match {
      case None => println(s"User: $receiver, not found in DHT")
      case Some(receiverPath) =>
        try {
          // looks up user and sends a message back
          val receiverRef = GetPeerRef(context, receiverPath.asInstanceOf[String])
          receiverRef ! Message(sender, text, ack)
        } catch {
          // catches all errors if DHT has stored value, but user offline
          case _: Throwable => println("Cannot find the user!")
        }
    }
  }
}
