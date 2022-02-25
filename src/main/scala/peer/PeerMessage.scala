package peer

import dht.LocalDht


trait PeerMessage{ }


case class Example(text : String) extends PeerMessage

class SynchronousMessage(text: String, peer: Peer) extends PeerMessage{
    //Contact DHT to get Peer info
    //If peer info 
    val peerInfo=dht.LocalDht.get("user@location") // TODO: How to fetch this correctly

    val peerInfoProcessing = peerInfo match {
        case Some(_) => ???
        case None => "Not able to contact user"
    }



    
}



