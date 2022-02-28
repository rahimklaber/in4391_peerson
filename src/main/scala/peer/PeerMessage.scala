package peer

import dht.LocalDht


// Info needed for chat messages
trait PeerMessage

case class Message(nonHashedSender: String, message : String) extends PeerMessage 



