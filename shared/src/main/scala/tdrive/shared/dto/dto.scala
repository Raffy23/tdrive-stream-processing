package tdrive.shared

import java.time.LocalDateTime

import tdrive.shared.Haversine

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
package object dto {

  type TaxiID = Short
  type TaxiWithData = (Taxi, Double)

  case class Taxi(id: TaxiID, timestamp: LocalDateTime, lat: Float, long: Float) {
    def coordinates = Coordinates(lat, long)
    def distance(o: Taxi): Double = Haversine.haversine(lat, long, o.lat, o.long)

    def asLocation = TaxiLocation(id, lat, long)
  }

  case class Coordinates(lat: Float, long: Float)
  case class TaxiLocation(id: TaxiID, lat: Float, long: Float)
  case class TaxiLeftArea(id: TaxiID)

  case class TaxiSpeed(id: TaxiID, speed: Double)
  case class TaxiSpeeding(id: TaxiID, speed: Double)
  object TaxiSpeed {
    def apply(data: (Taxi,Double)): TaxiSpeed = new TaxiSpeed(data._1.id, data._2)
  }

  case class TaxiState(var taxi: Taxi, var speed: Double, var count: Long) {
    def update(taxi: Taxi, speed: Double): TaxiState = {
      this.taxi = taxi
      this.speed = speed
      this.count += 1

      this
    }
  }
  object TaxiState {
    def apply(taxi: Taxi): TaxiState = TaxiState(taxi, 0D, 1)
  }


  case class TaxiData(taxi: Taxi, distance: Double, speed: Double, avgSpeed: Double) {
    def asTaxiSpeed = new TaxiSpeed(taxi.id, speed)
    def asTaxiSpeeding = new TaxiSpeeding(taxi.id, speed)
  }

}
