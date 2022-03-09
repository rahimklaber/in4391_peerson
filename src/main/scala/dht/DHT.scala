package dht

trait DHT {
    val local: LocalDht.type = LocalDht

  val distributed: DistributedDHT.type = DistributedDHT

  def put(key: String, data: Any): Unit

  def get(key: String): Option[Any]

    def contains(key: String): Boolean

  def append(key: String, data : Any): Unit

  def getAll(key: String): Option[List[Any]]
}
