package userData

import dht.{DistributedDHT, GetPeerKey, LocalDHT}
import userData.LoginProcedure.{login, register}
import userData.State.offline

object LogoutProcedure {

  def start(location: String, hashedMail: String, DistributedDHT: DistributedDHT): Unit = {
    //          val lookup = LocalDHT.getAll(hashedMail)
    val lookup = DistributedDHT.getAll(hashedMail)
    lookup match {
      case Some(value) =>
        val locatorInfoList: List[LocatorInfo] = value.asInstanceOf[List[LocatorInfo]]
        // test case
        // - login kevin, m; login kevin, n; logout kevin, m
        //        val locatorInfo = locatorInfoList.filter(l => l.locator == location)
        //        if (locatorInfo.isEmpty) {
        //          println(s"user ${mail} not logged in at location ${location}")
        //        } else {
        //          val onlineLocatorInfo = locatorInfo.head
        //          val offlineLocatorInfo = onlineLocatorInfo.copy(state = offline)
        //          val newLocatorInfoList = offlineLocatorInfo :: locatorInfoList.filter(l => l.locator != location)
        //          LocalDHT.put(hashedMail, newLocatorInfoList.head)
        //          newLocatorInfoList.tail.foreach(l => LocalDHT.append(hashedMail, l))
        //          println(newLocatorInfoList)
        //        }

        val updateUserInfo = locatorInfoList.map(l => {
          val newState = {
            if (l.locator == location) State.offline
            else {
              l.state
            }
          }
          LocatorInfo(l.locator, l.IP, l.port, newState, l.path)
        })
        DistributedDHT.put(hashedMail, updateUserInfo)

      case _ => println(s"user not found by DHT!")
    }
  }
}
