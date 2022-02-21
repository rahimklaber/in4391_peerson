trait DHT {
  def put[V](key: String, data: V): Unit

  def get[V](key: String): V
}