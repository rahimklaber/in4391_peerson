package peer


trait PeerMessage

case class Example(text: String) extends PeerMessage

case class PeerCmd(cmd: Command) extends PeerMessage // for sending commands to the peer


trait Command

case class AddToWall(user: String,text: String) extends Command
