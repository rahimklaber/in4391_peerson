package peer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
 
// For hashing: Based on https://stackoverflow.com/questions/46329956/how-to-correctly-generate-sha-256-checksum-for-a-string-in-scala 
import java.security.MessageDigest
import java.math.BigInteger

import dht.LocalDht

class Peer(context: ActorContext[PeerMessage],mail: String) extends AbstractBehavior[PeerMessage](context) {

  //Used SHA-256 because MD5 not secure (Check with linux: "echo -n "Test@test.com" | openssl dgst -sha256")
  val hashedMail = String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(mail.getBytes("UTF-8"))))
  var location = "42.422.171.250" // How should this be solved an IP?

  updateDHT(location)

  def updateDHT(location:String){
    dht.LocalDht.put(hashedMail, location)
  }
  
  


  override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
    context.log.info(s"Received message $msg")
    msg match {
      case _ => this
    }
  }
}

object Peer{
  def apply(): Behavior[PeerMessage] = {
    Behaviors.setup(context => new Peer(context, "Test@test.com"))
  }
}