package peer

import dht.LocalDht


// Info needed for chat messages
trait PeerMessage{ 
    val nonHashedSender: String
    val message: String
}


case class Message(nonHashedSender: String, message : String) extends PeerMessage 



