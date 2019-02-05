package tdrive.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import tdrive.server.WebSocketActor.{WsConnected, WsDisconnected}
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
class WebSocketActor extends Actor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe, UnsubscribeAck}

  private val mediator = DistributedPubSub.get(context.system).mediator
  private var wSocket: Option[ActorRef] = None

  override def receive: Receive = {

    case WsConnected(actorRef) =>
      wSocket = Some(actorRef)
      mediator ! Subscribe("taxi-data", self)

    case SubscribeAck(Subscribe("taxi-data", None, `self`)) =>
      context.become(receiveTaxiData)

    case UnsubscribeAck(Unsubscribe("taxi-data", None, `self`)) =>
      wSocket = None
      context.stop(self)

  }

  import io.circe.generic.auto._
  import io.circe.syntax._

  def receiveTaxiData: Receive = {
    case location: TaxiLocation => wSocket.foreach(_ ! location.asJson.noSpaces)
    case speeding: TaxiSpeeding => wSocket.foreach(_ ! speeding.asJson.noSpaces)
    case speed: TaxiSpeed if speed.speed > 0.0D => wSocket.foreach(_ ! speed.asJson.noSpaces)

    case WsDisconnected =>
      mediator ! Unsubscribe("taxi-data", self)
      context.become(receive)

  }

}
