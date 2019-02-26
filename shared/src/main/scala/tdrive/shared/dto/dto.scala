package tdrive.shared

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
package object dto {

  type TaxiID = Short

  case class Coordinates(lat: Float, long: Float)

  sealed trait TaxiMessage
  case class TaxiLocation(id: TaxiID, lat: Float, long: Float) extends TaxiMessage
  case class TaxiLeftArea(id: TaxiID) extends TaxiMessage
  case class TaxiSpeed(id: TaxiID, speed: Double) extends TaxiMessage
  case class TaxiSpeeding(id: TaxiID, speed: Double) extends TaxiMessage

}
