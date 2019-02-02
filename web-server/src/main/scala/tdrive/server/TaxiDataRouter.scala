package tdrive.server

import akka.actor.{Actor, ActorRef}
import akka.routing.{BroadcastRoutingLogic, Routee, Router}
import tdrive.server.TaxiDataRouter.{Register, Unregister}
import tdrive.shared.dto.{TaxiLocation, TaxiSpeed, TaxiSpeeding}

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
object TaxiDataRouter {
  sealed trait RouterCommand
  case class Register(actorRef: ActorRef) extends RouterCommand
  case class Unregister(actorRef: ActorRef) extends RouterCommand
}
class TaxiDataRouter extends Actor {

  @volatile var router = Router(BroadcastRoutingLogic(), Vector.empty)

  override def receive: Receive = {

    case Register(actor) =>
      context.watch(actor)
      router = router.addRoutee(actor)

    case Unregister(actor) =>
      router = router.removeRoutee(actor)
      context.unwatch(actor)

    case location: TaxiLocation => router.route(location, sender())
    case speeding: TaxiSpeeding => router.route(speeding, sender())
    case speed: TaxiSpeed       => router.route(speed, sender())

  }
}
