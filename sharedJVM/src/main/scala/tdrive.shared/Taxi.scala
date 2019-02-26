package tdrive.shared

import java.time.LocalDateTime

import tdrive.shared.dto.{Coordinates, TaxiID, TaxiLocation}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.02.2019
  */
case class Taxi(id: TaxiID, timestamp: LocalDateTime, lat: Float, long: Float) {
  def coordinates = Coordinates(lat, long)
  def distance(o: Taxi): Double = Haversine.haversine(lat, long, o.lat, o.long)

  def asLocation = TaxiLocation(id, lat, long)
}
