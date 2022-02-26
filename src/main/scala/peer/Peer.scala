package peer

import akka.actor.ActorSelection
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import userData.{LocatorInfo, LoginProcedure, State}

// For hashing: Based on https://stackoverflow.com/questions/46329956/how-to-correctly-generate-sha-256-checksum-for-a-string-in-scala 
import java.math.BigInteger
import java.security.MessageDigest


class Peer(context: ActorContext[PeerMessage], mail: String) extends AbstractBehavior[PeerMessage](context) {
  //Used SHA-256 because MD5 not secure (Check with linux: "echo -n "Test@test.com" | openssl dgst -sha256")
  val hashedMail = String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(mail.getBytes("UTF-8"))))

  /**
   * Get a reference to a peer from its path.
   */
  def getPeerRef(path: String): ActorSelection = {
    context.system.classicSystem.actorSelection(path)
  }


  override def onMessage(msg: PeerMessage): Behavior[PeerMessage] = {
    context.log.info(s"Received message $msg")

    val processMessage = msg match {
      case Login(location) => {
        LoginProcedure.start(location, hashedMail)
        println(dht.LocalDht.get(hashedMail))
      }
    }

    msg match {
      case _ => this
    }
  }
}

object Peer{
  def apply(mail:String): Behavior[PeerMessage] = {
    Behaviors.setup(context => {
      new Peer(context, mail)
    })
  }
}