package dht

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object LocalDht extends DHT{
  val _map : mutable.Map[String,List[Any]] = mutable.Map()
  override def put(key: String, data: Any): Unit = {
    _map.put(key,data :: Nil)
  }

  override def get(key: String): Option[Any] = {
    val value = _map.get(key)
    value match {
      case Some(list) => Some(list.head)
      case _ => None
    }
  }

  override def append(key: String, data: Any): Unit = {
    _map.put(key, data :: _map.getOrElse(key,Nil))
  }

  override def getAll(key: String): Option[List[Any]] = _map.get(key)
}
