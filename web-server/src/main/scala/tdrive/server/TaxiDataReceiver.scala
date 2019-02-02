package tdrive.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import tdrive.shared.dto.{TaxiLocation, TaxiSpeed, TaxiSpeeding}

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
class TaxiDataReceiver(broadcaster: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case location: TaxiLocation => broadcaster ! location//println(location)
    case speeding: TaxiSpeeding => broadcaster ! speeding//println(speeding)
    case speed: TaxiSpeed       => broadcaster ! speed//println(speed)
  }
}

