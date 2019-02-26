package tdrive.shared

import tdrive.shared.dto.TaxiSpeed

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.02.2019
  */
object TaxiSpeed {
  def apply(data: (Taxi,Double)): TaxiSpeed = new TaxiSpeed(data._1.id, data._2)
}
