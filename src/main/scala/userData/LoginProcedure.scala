package userData

import dht.LocalDHT

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL

object LoginProcedure {

  /**
   * the start stage of login process
   */
  def start(location: String, hashedMail: String): Unit = {
    /*
    * Possible cases
    * 1. never been registered
    * 2. still logged in at the same device
    * 3. offline at the same device
    * 4. online at a different device
    * 5. offline at a different device
    * */

    // for testing
    //    val inf = LocatorInfo("home", findIPAddress(), "80", State.active)
    //    val inf = LocatorInfo("home", findIPAddress(), "80", State.offline)
    //    val inf = LocatorInfo("laptop", findIPAddress(), "80", State.online)
    // val inf = LocatorInfo("laptop", findIPAddress(), "80", State.offline)
    // LocalDHT.put(hashedMail, inf)

    // choose between login and register
    if (LocalDHT.contains(hashedMail)) {
      login(location, hashedMail)
    } else {
      register(location, hashedMail)
    }
  }

  /**
   *
   * @param location location in string, say "laptop", "home"
   * @param hashedMail hashedMail
   */
  def login(location: String, hashedMail: String): Unit = {
    // 1. get user info from the DHT
    val userLocatorInfos: Option[List[Any]] = LocalDHT.getAll(hashedMail)
    var locationInfoList: List[LocatorInfo] = userLocatorInfos match {
      case Some(value) => value.asInstanceOf[List[LocatorInfo]]
      case None => throw new Exception()  // TODO: handle error
    }

    // 2. if no desired location add it
    val desiredLocation = locationInfoList.filter(l => l.locator == location)
    if (desiredLocation.isEmpty) {
      locationInfoList = LocatorInfo(location, findIPAddress(), "80", State.active) :: locationInfoList
    }

    // 3. update user info
    //  - only one location is active
    //  - but there might be multiple locations that are online
    //  - needs improvement if needed
    val updateUserInfo = locationInfoList.map(l => {
      val newState = {
        if (l.locator == location) State.active
        else {
          // active/online -> online
          if (l.state != State.offline) State.online
          else State.offline
        }
      }
      LocatorInfo(l.locator, l.IP, l.port, newState)
    })

    // 4. send new info to DHT
    LocalDHT.put(hashedMail, updateUserInfo)
  }

  def register(location: String, hashedMail: String): Unit = {
    val ip = findIPAddress()
    val port = "80"
    val locatorInfo = LocatorInfo(location, ip, port, State.active)
    LocalDHT.put(hashedMail, locatorInfo)
  }

  def findIPAddress(): String = {
    val whereIsMyIPURL = new URL("http://checkip.amazonaws.com")
    val in: BufferedReader = new BufferedReader(new InputStreamReader(whereIsMyIPURL.openStream()))
    in.readLine()
  }
}
