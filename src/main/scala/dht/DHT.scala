package dht

  trait DHT {
    val local: LocalDht.type = LocalDht

  def put(key: String, data: Any): Unit

  def get(key: String): Option[Any]

    def contains(key: String): Boolean
}
