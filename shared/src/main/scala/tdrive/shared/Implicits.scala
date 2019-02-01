package tdrive.shared

import java.util.Properties

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 01.02.19
  */
object Implicits {

  implicit class RichProperties(val property: Properties) extends AnyVal {

    def getFilePath(name: String): String =
      property.getProperty(name).replaceFirst("^~", System.getProperty("user.home"))

    def getFilePath(name: String, default: String) =
      property.getProperty(name, default).replaceFirst("^~", System.getProperty("user.home"))

  }

}
