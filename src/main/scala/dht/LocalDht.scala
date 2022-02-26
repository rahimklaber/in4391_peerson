package dht

import scala.collection.mutable

object LocalDht extends DHT{
  private val _map : mutable.Map[String,Any] = mutable.Map()
  override def put(key: String, data: Any): Unit = {
    _map += (key -> data)
  }

  override def get(key: String): Option[Any] = _map.get(key)

  override def contains(key: String): Boolean =
    if (_map.contains(key)) true
    else false
}
