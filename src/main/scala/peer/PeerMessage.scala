package peer


trait PeerMessage

case class Example(text : String) extends PeerMessage

case class Login(location: String) extends PeerMessage