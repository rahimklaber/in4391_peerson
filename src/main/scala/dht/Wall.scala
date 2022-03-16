package dht

import akka.actor.typed.scaladsl.ActorContext
import peer.{Message, PeerMessage}

import scala.collection.mutable

/**
 * Async Wall Protocol:
 * wi - The wall index file contains a list of all wall entries a user has.
 * we - This file contains one wall entry. The number of the wall entry is successive
 *             and defined on the receiver's side
 * ----------------------------------------------------------------------------------------
 * To put a message on another user's Wall, the following key-value pair is stored in DHT
 * (key, value)
 * key - ${hashedMail}@we${index}, where ${hashedMail} is the hashed mail of the receiver
 * value - WallEntry(sender, content)
 * ----------------------------------------------------------------------------------------
 * The Wall object manages methods to update the Wall entries to DHT
 */

object Wall {
  /**
   *
   * @param index the current index of the entry
   * @param sender the sender email (not hashed)
   * @param content the file/message content
   */
  case class WallEntry(index: Int, sender: String, content: String) extends File

  /**
   * The WallIndex File, stored in the DHT in the key-value form of
   *   wallIndexKey -> WallIndex(owner, lastEntryIndex, entries)
   *
   * @param hashedMail who owns the wall, email (not hashed)
   * @param lastEntryIndex the index of the most recent entry (i.e., the current last index)
   * @param entries a list buffer of all wall entries
   */
  case class WallIndex(hashedMail: String, lastEntryIndex: Int, entries: mutable.ListBuffer[String])


  /**
   * initialization:
   * create a WallIndex entry for owner if not existed
   * @param owner who owns the wall, mail (not hashed)
   */
  def load(context: ActorContext[PeerMessage], owner: String, dht: DHT): Unit = {
    val wallIndexKey: String = getWallIndexKey(owner)
    // if wallIndexKey found, then try to fetch the wall entries
    dht.contains(wallIndexKey,{ res =>
      if (res) {
        val wallIndexLookup = dht.get(wallIndexKey,{
          case Some(ownerWallIndex: WallIndex) =>
            val wallEntryKeyBuffer = ownerWallIndex.entries
            wallEntryKeyBuffer.foreach(wallEntryKey => {
              val lookup = dht.get(wallEntryKey,{
                case Some(currentWallEntry: WallEntry) =>
                  context.self ! Message(currentWallEntry.sender, currentWallEntry.content, ack = false)
                  // remove from DHT
                  dht.remove(wallEntryKey)
                case _ => println(s"WallEntry under ${wallEntryKey} not found")
              })

            })
          case _ => println(s"WallIndex under ${wallIndexKey} not found")
        })
      }
    })

    dht.put(wallIndexKey, WallIndex(Encrypt(owner), -1, mutable.ListBuffer.empty))
  }

  def getWallIndexKey(owner: String): String = {
    s"${owner}@wi"
  }

//  def getWallIndex(owner: String, dht: DHT): WallIndex = {
//    val key: String = getWallIndexKey(owner)
//    val lookup = dht.get(key)
//    lookup match {
//      case Some(entry) =>
//        entry.asInstanceOf[WallIndex]
//      case None =>
//        println(s"Cannot find WallIndex of ${owner}")
//        WallIndex("", -1, null)
//    }
//  }

  /**
   * generate a wall entry key
   * @param owner mail (not hashed)
   * @param index current index for the wall entry
   * @return wall entry key
   */
  def getWallEntryKey(owner: String, index: Int): String = {
    s"${owner}@we${index}"
  }

//  def getWallEntry(owner: String, index: Int,dht:DHT): WallEntry = {
//    val key: String = getWallEntryKey(owner, index)
//    val lookup = dht.get(key)
//    lookup match {
//      case Some(entry) =>
//        entry.asInstanceOf[WallEntry]
//      case None =>
//        println(s"Cannot find WallEntry of ${owner} at index ${index}")
//        WallEntry(-1, "", "")
//    }
//  }

//  /**
//   *
//   * @param index the current index of the entry
//   * @param sender the sender email (not hashed)
//   * @param content the file/message content
//   */
//  case class WallEntry(index: Int, sender: String, content: String)
//
//  /**
//   * The WallIndex File, stored in the DHT in the key-value form of
//   *   wallIndexKey -> WallIndex(owner, lastEntryIndex, entries)
//   *
//   * @param hashedMail who owns the wall, email (not hashed)
//   * @param lastEntryIndex the index of the most recent entry (i.e., the current last index)
//   * @param entries a list buffer of all wall entries
//   */
//  case class WallIndex(hashedMail: String, lastEntryIndex: Int, entries: mutable.ListBuffer[String])
//
//  /**
//   * initialization:
//   * create a WallIndex entry for owner if not existed
//   * @param owner who owns the wall, mail (not hashed)
//   */
//  def load(context: ActorContext[PeerMessage], owner: String, dht: DHT): Unit = {
//    val wallIndexKey: String = getWallIndexKey(owner)
//    // if wallIndexKey found, then try to fetch the wall entries
//    if (dht.contains(wallIndexKey)) {
//      val wallIndexLookup = dht.get(wallIndexKey)
//      wallIndexLookup match {
//        case Some(ownerWallIndex: WallIndex) =>
//          val wallEntryKeyBuffer = ownerWallIndex.entries
//          wallEntryKeyBuffer.foreach(wallEntryKey => {
//            val lookup = dht.get(wallEntryKey)
//            lookup match {
//              case Some(currentWallEntry: WallEntry) =>
//                context.self ! Message(currentWallEntry.sender, currentWallEntry.content, ack = false)
//                // remove from DHT
//                dht.remove(wallEntryKey)
//              case _ => println(s"WallEntry under ${wallEntryKey} not found")
//            }
//          })
//        case _ => println(s"WallIndex under ${wallIndexKey} not found")
//      }
//    }
//    dht.put(wallIndexKey, WallIndex(Encrypt(owner), -1, mutable.ListBuffer.empty))
//  }
//
//  def getWallIndexKey(owner: String): String = {
//    s"${owner}@wi"
//  }
//
//  def getWallIndex(owner: String, dht: DHT): WallIndex = {
//    val key: String = getWallIndexKey(owner)
//    val lookup = dht.get(key)
//    lookup match {
//      case Some(entry) =>
//        entry.asInstanceOf[WallIndex]
//      case None =>
//        println(s"Cannot find WallIndex of ${owner}")
//        WallIndex("", -1, null)
//    }
//  }
//
//  /**
//   * generate a wall entry key
//   * @param owner mail (not hashed)
//   * @param index current index for the wall entry
//   * @return wall entry key
//   */
//  def getWallEntryKey(owner: String, index: Int): String = {
//    s"${owner}@we${index}"
//  }
//
//  def getWallEntry(owner: String, index: Int,dht:DHT): WallEntry = {
//    val key: String = getWallEntryKey(owner, index)
//    val lookup = dht.get(key)
//    lookup match {
//      case Some(entry) =>
//        entry.asInstanceOf[WallEntry]
//      case None =>
//        println(s"Cannot find WallEntry of ${owner} at index ${index}")
//        WallEntry(-1, "", "")
//    }
//  }
//
////  /**
////   * add the file to receiver's wall
////   * @param sender the email of the sender (not hashed)
////   * @param receiver the email of the receiver (not hashed)
////   * @param file the file of type File trait
////   * @param maxLength max length of the file content stored in DHT, default: 128
////   * TODO (if time allows): deal with fileName and fileType
////   */
////  def add(sender: String, receiver: String, file: File, maxLength: Int = 128): Unit = {
////    add(sender, receiver, text = file.content.substring(0, maxLength))
////  }
//
//  /**
//   * add the message to receiver's wall
//   * @param sender the email of the sender (not hashed)
//   * @param receiver the email of the receiver (not hashed)
//   * @param text the text content of the message
//   */
//  def add(sender: String, receiver: String, text: String,dht: DHT): Unit = {
//    val lastWallIndex = getWallIndex(receiver,dht)
//    val newIndex = lastWallIndex.lastEntryIndex + 1
//    // put a new WallEntry
//    val wallEntryKey = getWallEntryKey(receiver, newIndex)
//    val wallEntry = WallEntry(newIndex, sender, text)
//    dht.put(wallEntryKey, wallEntry)
//    // update WallIndex
//    val wallIndexKey = getWallIndexKey(receiver)
//    val newWallIndex = WallIndex(Encrypt(receiver), newIndex, wallEntryKey +: lastWallIndex.entries)
//    dht.put(wallIndexKey, newWallIndex)
//  }

//  /**
//   * add the message to receiver's wall
//   * @param sender the email of the sender (not hashed)
//   * @param receiver the email of the receiver (not hashed)
//   * @param text the text content of the message
//   */
//  def add(sender: String, receiver: String, text: String,dht: DHT): Unit = {
//    val lastWallIndex = getWallIndex(receiver,dht)
//    val newIndex = lastWallIndex.lastEntryIndex + 1
//    // put a new WallEntry
//    val wallEntryKey = getWallEntryKey(receiver, newIndex)
//    val wallEntry = WallEntry(newIndex, sender, text)
//    dht.put(wallEntryKey, wallEntry)
//    // update WallIndex
//    val wallIndexKey = getWallIndexKey(receiver)
//    val newWallIndex = WallIndex(Encrypt(receiver), newIndex, wallEntryKey +: lastWallIndex.entries)
//    dht.put(wallIndexKey, newWallIndex)
//  }

  // TODO (if time allows): def remove()

}