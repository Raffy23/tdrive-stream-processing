package tdrive.shared

import tdrive.shared.dto.{TaxiSpeed, TaxiSpeeding}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.02.2019
  */
case class TaxiData(taxi: Taxi, distance: Double, speed: Double, avgSpeed: Double) {
  def asTaxiAvgSpeed = new TaxiSpeed(taxi.id, avgSpeed)
  def asTaxiSpeeding = TaxiSpeeding(taxi.id, speed)
}
