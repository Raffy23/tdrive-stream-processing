package tdrive.importer

import java.time.LocalDateTime

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
package object util {

  implicit object LocalDateTimeOrdering extends Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int = x.compareTo(y)
  }

}
