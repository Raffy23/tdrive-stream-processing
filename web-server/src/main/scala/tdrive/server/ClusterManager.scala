package tdrive.server

import akka.cluster.ClusterEvent._
import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
class ClusterManager extends Actor with ActorLogging {

  private val cluster = Cluster(context.system)

  override def receive: Receive = {
    case MemberUp(member)               => log.info(s"Member is up: ${member.address}")
    case UnreachableMember(member)      => log.info(s"Member is unreachable: ${member.address}")
    case MemberRemoved(member, pState)  => log.info(s"Member is removed: ${member.address} after $pState")
    case _: MemberEvent =>
  }


  override def preStart(): Unit = {
    cluster.subscribe(
      subscriber = self,
      initialStateMode = InitialStateAsEvents,
      to = classOf[MemberEvent], classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(subscriber = self)
  }

}
