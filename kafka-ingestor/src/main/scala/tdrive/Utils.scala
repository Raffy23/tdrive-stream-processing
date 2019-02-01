package tdrive

import java.util.zip.GZIPInputStream

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 01.02.19
  */
object Utils {

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
