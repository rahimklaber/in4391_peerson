package dht

import net.tomp2p.dht.{PeerBuilderDHT, PeerDHT}
import net.tomp2p.futures.FutureBootstrap
import net.tomp2p.p2p.PeerBuilder
import net.tomp2p.peers.Number160
import net.tomp2p.storage.Data

import java.net.InetAddress

class DistributedDHT(nodeId: Int) extends DHT {

  // create a new DHT node
  val peer: PeerDHT = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(nodeId)).ports(4000 + nodeId).start).start

  // connect to a stable DHT node
  val fb: FutureBootstrap = this.peer.peer.bootstrap.inetAddress(InetAddress.getByName("127.0.0.1")).ports(4001).start
  fb.awaitUninterruptibly
  if (fb.isSuccess) peer.peer.discover.peerAddress(fb.bootstrapTo.iterator.next).start.awaitUninterruptibly

  override def put(key: String, data: Any): Unit = {
    peer.put(Number160.createHash(key)).data(new Data(data)).start.awaitUninterruptibly
  }

  override def get(key: String): Option[Any] = {
    val futureGet = peer.get(Number160.createHash(key)).start
    futureGet.awaitUninterruptibly
    if (futureGet.isSuccess) return Some(futureGet.dataMap.values.iterator.next.`object`())
    null
  }

  override def contains(key: String): Boolean = {
    val futureGet = peer.get(Number160.createHash(key)).start
    futureGet.awaitUninterruptibly
    if (!futureGet.isSuccess) return false
    println(futureGet.dataMap())
    if(futureGet.isEmpty) return false
    true
  }

  override def getAll(key: String): Option[List[Any]] = {
    val value = get(key)
    value match {
      case Some(v) => Some(v.asInstanceOf[List[Any]])
      case _ => None
    }
  }

  /**
   * remove a key-value pair
   */
  override def remove(key: String): Unit = {
    peer.remove(Number160.createHash(key)).start()
  }

  override def append(key: String, data: Any): Unit = {
    val futureGet = peer.get(Number160.createHash(key)).start
    futureGet.awaitUninterruptibly
    if (futureGet.isSuccess) {
      if (futureGet.dataMap.values.iterator.hasNext) {


        val list = futureGet.dataMap.values.iterator.next.`object`()
        list match {
          case l@List(xs) => put(key, data :: l)
        }
      } else {
        put(key,data::Nil)
      }
    }

  }
}
