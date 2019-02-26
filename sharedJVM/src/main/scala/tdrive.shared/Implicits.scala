package tdrive.shared

import java.util.Properties
import java.util.zip.GZIPInputStream

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 01.02.19
  */
object Implicits {

  implicit class RichProperties(val property: Properties) extends AnyVal {

    def getFilePath(name: String): String =
      property.getProperty(name).replaceFirst("^~", System.getProperty("user.home").replace("\\", "\\\\"))

    def getFilePath(name: String, default: String) =
      property.getProperty(name, default).replaceFirst("^~", System.getProperty("user.home").replace("\\", "\\\\"))

  }

  implicit class RichGzipInputStream(val gzipInputStream: GZIPInputStream) extends AnyVal {

    def readFully(buffer: Array[Byte]): Int = {
      var position = 0

      Stream
        .continually(gzipInputStream.read(buffer, position, buffer.length - position))
        .map{read => if (read > -1) position += read ; read }
        .takeWhile{read => read != -1 && position < buffer.length}
        .foreach{_ => }

      position
    }

  }

}
