package dht

trait DHT {
    val local: LocalDht.type = LocalDht

  def put(key: String, data: Any): Unit

  def get(key: String): Option[Any]

  def append(key: String, data : Any): Unit

  def getAll(key: String): Option[List[Any]]
}
