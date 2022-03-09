package dht

import net.tomp2p.dht.{PeerBuilderDHT, PeerDHT}
import net.tomp2p.futures.FutureBootstrap
import net.tomp2p.p2p.PeerBuilder
import net.tomp2p.peers.Number160

import java.net.InetAddress

class DistributedDHT(peerId: Int) extends DHT {

  // create a new Peer
  val peer: PeerDHT = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerId)).ports(4000 + peerId).start).start

  val fb: FutureBootstrap = this.peer.peer.bootstrap.inetAddress(InetAddress.getByName("127.0.0.1")).ports(4001).start
  fb.awaitUninterruptibly
  if (fb.isSuccess) peer.peer.discover.peerAddress(fb.bootstrapTo.iterator.next).start.awaitUninterruptibly

  override def put(key: String, data: Any): Unit = ???

  override def get(key: String): Option[Any] = ???

  override def contains(key: String): Boolean = ???

  override def append(key: String, data: Any): Unit = ???

  override def getAll(key: String): Option[List[Any]] = ???
}
