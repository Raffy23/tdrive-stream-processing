package tdrive.importer

import java.time.LocalDateTime

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
package object dto {

  case class TaxiEntry(timestamp: LocalDateTime, longitude: Double, latitude: Double)

}
