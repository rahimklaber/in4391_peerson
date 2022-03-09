package dht

import scala.collection.mutable

object LocalDHT extends DHT {

  /**
   * a mutable map
   * stores user in the format ["hashed-email", location]
   */
  private val _map: mutable.Map[String, List[Any]] = mutable.Map()

  /**
   * put a new key on the DHT
   * create a data list for this key
   * add the data to the list
   */
  override def put(key: String, data: Any): Unit = {
    _map.put(key, data :: Nil)
  }

  /**
   * retrieves the head of data list by key
   * if key not existed, return None
   */
  override def get(key: String): Option[Any] = {
    val value = _map.get(key)
    value match {
      case Some(list) => Some(list.head)
      case _ => None
    }
  }

  /**
   * check if the DHT contains a certain key
   */
  override def contains(key: String): Boolean = {
    _map.contains(key)
  }

  /**
   * update the data list with a new item under a key
   */
  override def append(key: String, data: Any): Unit = {
    _map.put(key, data :: _map.getOrElse(key, Nil))
  }

  /**
   * retrieves all values stored in a data list under a key
   */
  override def getAll(key: String): Option[List[Any]] = _map.get(key)

  override def remove(key: String): Unit = _map.remove(key)

  /**
   * a helper method to print all entries in _map
   */
  def printElement(): Unit = _map.foreach(entry => println(entry))

}
