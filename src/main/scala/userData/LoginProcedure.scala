package userData

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL

object LoginProcedure {

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
//    val inf = List(LocatorInfo("home", findIPAddress(), "80", State.active))
//    val inf = List(LocatorInfo("home", findIPAddress(), "80", State.offline))
//    val inf = List(LocatorInfo("laptop", findIPAddress(), "80", State.online))
    val inf = List(LocatorInfo("laptop", findIPAddress(), "80", State.offline))
    dht.LocalDht.put(hashedMail, inf)

    // choose between login and register
    if (dht.LocalDht.contains(hashedMail)) {
      login(location, hashedMail)
    } else {
      register(location, hashedMail)
    }
  }

  def login(location: String, hashedMail: String): Unit = {

    // 1. get user info from the dht
    val userInfo = dht.LocalDht.get(hashedMail)
    var locations: List[LocatorInfo] = userInfo match {
      case Some(value) => value.asInstanceOf[List[LocatorInfo]]
      case None => throw new Exception() // TO DO: handle error
    }

    // 3. if no desired location add it
    val desiredLocation = locations.filter(l => l.locator == location)
    if (desiredLocation.isEmpty) {
      locations = locations :+ LocatorInfo(location, findIPAddress(), "80", State.active)
    }

    // 4. update user info
    val updatedUserInfo = locations.map(l => {
      val newState = {
        if(l.locator == location) State.active
        else State.offline
      }
      LocatorInfo(l.locator, l.IP, l.port, newState)
    })

    // 5. send new info to DHT
    dht.LocalDht.put(hashedMail, updatedUserInfo)
  }

  def register(location: String, hashedMail: String) = {
    val ip = findIPAddress()
    val port = "80"
    val locator = new LocatorInfo(location, ip, port, State.active)
    dht.LocalDht.put(hashedMail, List(locator))

  }

  def findIPAddress(): String ={
    val whatismyipURL = new URL("http://checkip.amazonaws.com")
    val in:BufferedReader = new BufferedReader(new InputStreamReader(
      whatismyipURL.openStream()))
    in.readLine()
  }

}
