package tdrive.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import tdrive.server.WebSocketActor.{Command, WsConnected, WsDisconnected}
import tdrive.shared.dto.{TaxiLocation, TaxiSpeed, TaxiSpeeding}

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
object WebSocketActor {
  sealed trait Command
  case class WsConnected(actorRef: ActorRef) extends Command
  case object WsDisconnected extends Command
}
class WebSocketActor(broadcaster: ActorRef) extends Actor with ActorLogging {

  private var wSocket: Option[ActorRef] = None

  override def receive: Receive = {

    case cmd: Command => cmd match {
      case WsConnected(actorRef) =>
        wSocket = Some(actorRef)
        broadcaster ! TaxiDataRouter.Register(self)

      case WsDisconnected =>
        broadcaster ! TaxiDataRouter.Unregister(self)
        context.stop(self)
    }

    // TODO: to Json or whatever
    case location: TaxiLocation => wSocket.foreach(_ ! location.toString)
    case speeding: TaxiSpeeding => wSocket.foreach(_ ! speeding.toString)
    case speed: TaxiSpeed       => wSocket.foreach(_ ! speed.toString)

  }
}
