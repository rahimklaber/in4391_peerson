package peer

import akka.actor.ActorSelection
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
 
// For hashing: Based on https://stackoverflow.com/questions/46329956/how-to-correctly-generate-sha-256-checksum-for-a-string-in-scala 
import java.security.MessageDigest
import java.math.BigInteger

class Peer(context: ActorContext[PeerMessage], mail: String) extends AbstractBehavior[PeerMessage](context) {

  //Used SHA-256 because MD5 used in paper is not secure
  private def SHA256Hash(stringToHash: String): String = {
    String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(stringToHash.getBytes("UTF-8"))));
  }

  // All Peers need a hashed mail
  private val hashedMail = SHA256Hash(mail)

  // Put location in the DHT with the hash being the key
  dht.LocalDht.put(hashedMail, context.self.path.toString)

  // Get a reference to a peer from its path.
  private def getPeerRef(path: String): ActorSelection = {
    context.system.classicSystem.actorSelection(path)
  }

  // Logic for answering the user
  private def chatMessage(mailToContact: String, message: String, selfMail:String){
    val dhtLookUp = dht.LocalDht.get(SHA256Hash(mailToContact))
    dhtLookUp match {
      case None => println(s"User: $mailToContact, not found in DHT")
      case Some(value) => 
        try {
          // Looks up user and sends a message back
          val peerRef = getPeerRef(dhtLookUp.get.asInstanceOf[String]) 
          peerRef ! Message(selfMail,message)
        }
        catch { 
          //Catches all errors if DHT has stored value, but user offline
          case _ : Throwable => println("Could not send to user!")
        }
      }
  }

  override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
    
    msg match{
      case Message(nonHashedSender, message) => 
        // TODO: what is expected test behavior here? Does only send "I got your message" back and forth
        context.log.info(s"From: $nonHashedSender| Message: $message")
        this.chatMessage(nonHashedSender,"I got your message",this.mail)
        this
    }
  } 
}

object Peer {
  def apply(mail:String): Behavior[PeerMessage] = {
    Behaviors.setup(context => {
      new Peer(context, mail)
    })
  }
}
