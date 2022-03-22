package file

import logic.login.LocatorInfo

object FileOperations {
  /**
   * DHTFileEntry is a file entry stored in DHT:
   * "${hashedMail}@${fileType} -> ${hashedMail}#${locator}#${version}"
   */
  case class DHTFileEntry(hashedMail: String, locator: LocatorInfo, version: Int)

  //  /**
  //   * DHTFileKey is the key for lookup in DHT
  //   */
  //  def getDHTFileKey(hashedMail: String, fileType: FileType.FileType): String = {
  //    s"${hashedMail}@${fileType}"
  //  }
  //
  //  /**
  //   * add the file key-value pair to DHT
  //   * @param hashedMail hashed email of the file holder
  //   * @param locator usually user path, context.self.path.toString
  //   * @param version default: 0
  //   * @param file a file of File trait (fileName, fileType, content)
  //   */
  //  def add(hashedMail: String, locator: String, version: Int = 0, file: File,dht:DHT): Unit = {
  //    val key: String = getDHTFileKey(hashedMail, file.fileType)
  //    val entry = DHTFileEntry(hashedMail, locator, version)
  //    dht.put(key, entry)
  //  }
}
