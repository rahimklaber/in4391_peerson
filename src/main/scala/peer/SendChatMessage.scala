package peer

import akka.actor.typed.scaladsl.ActorContext
import dht.{DistributedDHT, Encrypt, GetPathByMail}

class SendChatMessage(val context: ActorContext[PeerMessage], val sender: String, val receiver: String, val text: String, val ack: Boolean, val dhtNode: DistributedDHT) {
  /**
   * method for chatting
   * @param context ActorContext
   * @param sender sender email (not hashed)
   * @param receiver receiver email (not hashed)
   * @param text message text
   * @param ack ack
   */
  def send(): Unit = {
    val pathLookUp = new GetPathByMail(receiver, dhtNode, onReceivePath)
    pathLookUp.get()
  }

  def onReceivePath(pathLookUp:Option[String]): Unit ={
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
