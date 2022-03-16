package dht

import net.tomp2p.dht.{FutureGet, PeerBuilderDHT, PeerDHT}
import net.tomp2p.futures.{BaseFuture, BaseFutureAdapter, FutureBootstrap}
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



  // METHODS FOR RETRIEVING FROM DHT
  override def get(key: String, callback: Option[Any] => Unit) = {
    val futureGet = peer.get(Number160.createHash(key)).start

    futureGet.addListener(new BaseFutureAdapter[BaseFuture] {

      override def operationComplete(future: BaseFuture): Unit = {
        if(future.isSuccess()) {
          callback(Some(futureGet.dataMap.values.iterator.next.`object`()))
        } else {
          callback(null)
        }
      }

      })
  }

  override def getAll(key: String, callback: Option[List[Any]] => Unit) {

    val futureGet = peer.get(Number160.createHash(key)).start

    futureGet.addListener(new BaseFutureAdapter[BaseFuture] {

      override def operationComplete(future: BaseFuture): Unit = {
        if(future.isSuccess()) {
          val value = futureGet.dataMap.values.iterator.next.`object`()
          value match {
            case v@List(xs) => callback(Some(v))
            case v@_ =>
              callback(None)
          }
        } else {
          callback(None)
        }
      }

    })
  }

  override def contains(key: String, callback: Boolean => Unit) = {
    val futureGet = peer.get(Number160.createHash(key)).start

    futureGet.addListener(new BaseFutureAdapter[BaseFuture] {

      override def operationComplete(future: BaseFuture): Unit = {
        if(future.isSuccess()) {
          if (futureGet.isEmpty) {
//            println("contains - success, but empty")
            callback(false)
          } else {
//            println("contains - success")
            callback(true)
          }
        } else {
//          println("contains - not success")
          callback(false)
        }
      }
    })
  }



  // METHODS FOR PUTTING IN THE DHT
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
  override def put(key: String, data: Any): Unit = {
    peer.put(Number160.createHash(key)).data(new Data(data)).start()
  }
}
