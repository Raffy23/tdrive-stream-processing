package tdrive.server

import akka.actor.{Actor, ActorLogging}
import tdrive.shared.dto.TaxiLocation

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
class TestActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case e: Any => println(e)
  }
}
