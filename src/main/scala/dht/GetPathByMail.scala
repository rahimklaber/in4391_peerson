package dht
import userData.{LocatorInfo, State}

object GetPathByMail {

  /**
   * find a current active/online actor's path purely based on its mail
   * should be a function of DHT
   * @param mail mail to look up
   * @return path if exists, else None
   */
  def apply(mail: String, DistributedDHT: DistributedDHT): Option[String] = {
    val hashedMail: String = dht.Encrypt(mail)
//    val lookup = LocalDHT.getAll(hashedMail)
    val lookup = DistributedDHT.getAll(hashedMail)
    println(lookup)
    lookup match {
      case Some(value: List[LocatorInfo]) =>
        // filter an active or online locator info
        val validLocatorInfoList = value.filter(l => l.state == State.active || l.state == State.online)
        if (validLocatorInfoList.isEmpty) {
          println(s"peer ${mail} not found by DHT")
          None
        } else {
//          val location = validLocatorInfoList.head.locator
//          val pathLookUp = LocalDHT.get(GetPeerKey(mail, location))
//          pathLookUp match {
//            case Some(path: String) => Some(path)
//            case _ => None
//          }
          Some(validLocatorInfoList.head.path)
        }
      case _ => None
    }
  }
}

