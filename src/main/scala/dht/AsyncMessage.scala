package dht

import akka.actor.typed.scaladsl.ActorContext
import peer.{Message, PeerMessage}

import scala.collection.mutable

object AsyncMessage {

  case class Notification(sender: String, content: String)

  def load(context: ActorContext[PeerMessage], owner: String, dht: DHT): Unit = {
    val notificationKey: String = getNotificationKey(owner)
    dht.contains(notificationKey, {res =>
      if (res) {
        val notificationLookup = dht.get(notificationKey, {
          case Some(notifications: mutable.ListBuffer[Notification]) =>
            notifications.foreach(msg =>
              context.self ! Message(msg.sender, msg.content, ack = false))
          case _ => println(s"notifications under ${notificationKey} not found")
        })
      }
    })

    dht.put(notificationKey, mutable.ListBuffer.empty[Notification])
  }

  def getNotificationKey(owner: String): String = {
    s"${owner}@no"
  }

  def getNotifications(owner: String, dht: DHT): mutable.ListBuffer[Notification] = {
    val notificationKey: String = getNotificationKey(owner)
    dht.get(notificationKey, {
      case Some(notificationsListBuffer) =>
        notificationsListBuffer.asInstanceOf[mutable.ListBuffer[Notification]]
      case None =>
        mutable.ListBuffer.empty[Notification]
    })
  }

  def add(sender: String, receiver: String, text: String, dht: DHT): Unit = {
    val newNotifications = Notification(sender, text) +: getNotifications(receiver, dht)
    dht.put(receiver, newNotifications)
  }
}
