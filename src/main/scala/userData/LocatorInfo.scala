package userData

import userData.State.State

case class LocatorInfo(
                        val locator: String, // a string defined by user, to tell on which machine the user is currently active
                        val IP: String,
                        val port: String,
                        val State: State, // only one peerID can be active at a time
                        // val meshID: String, // optional, if a user is participating in a user mesh project
                        // val GPS: String, // optional, coordinate based P2p neighbour selection
                        // val timestamp: Int // time in seconds since 01.01.1970, used fot OpenDHT problems -> maybe not needed
                      )

object State extends Enumeration {
  type State = Value
  val online = Value("online")
  val active = Value("active")
  val offline = Value("offline")
}
