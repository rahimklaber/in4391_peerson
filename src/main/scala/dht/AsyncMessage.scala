package dht

import akka.actor.typed.scaladsl.ActorContext
import peer.{Message, Notification, PeerMessage}

import scala.collection.mutable

object AsyncMessage {

  case class OfflineMessage(sender: String, content: String, ack: Boolean)

  def load(context: ActorContext[PeerMessage], owner: String, dht: DHT): Unit = {
    println(s"loading async messages of ${owner}")
    val offMsgKey: String = getKey(owner)
    dht.contains(offMsgKey, { res =>
      if (res) {
        dht.get(offMsgKey, {
          case Some(notifications: mutable.ListBuffer[OfflineMessage]) =>
            println(s"I'm here with notifications $notifications")
            notifications.foreach(msg => {
              println(s"Loading offline message... $msg")
              context.self ! Notification(msg)
            })
          case _ => println(s"offline messages under ${offMsgKey} not found")
        })
      } else {
        println(res)
        println(s"key $offMsgKey not found in dht")
      }
    })
    dht.put(offMsgKey, mutable.ListBuffer.empty[OfflineMessage])
  }

  def getKey(owner: String): String = {
    s"${owner}@no"
  }

  def add(sender: String, receiver: String, text: String, ack: Boolean, dht: DHT): Unit = {
    val offMsgKey: String = getKey(receiver)
    dht.get(offMsgKey, {
      case Some(offMsgs: mutable.ListBuffer[OfflineMessage]) =>
        val newOffMsgs = OfflineMessage(sender, text, ack) +: offMsgs
        println(s"newOffMsgs: $newOffMsgs")
        dht.put(offMsgKey, newOffMsgs)
        println(s"adding offline message to $receiver's async message list")
      case _ =>
        dht.put(offMsgKey, mutable.ListBuffer.empty[OfflineMessage])
        println(s"async message list of $receiver not found, create a new empty one")
    })
  }
}
