package tdrive.server

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import tdrive.server.kafka.TaxiDataReceiver
import tdrive.server.util.KafkaSchemas

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

    case "Shutdown" =>
      log.info("ClusterManager is shutting down")
      context.stop(self)

    case "SpawnKafka" =>
      val groupID = cluster.selfAddress.toString
      context.watch(context.system.actorOf(Props(new TaxiDataReceiver("taxi-locations", groupID, new KafkaSchemas.TaxiLocationDeserializer))))
      context.watch(context.system.actorOf(Props(new TaxiDataReceiver("taxi-current-speed", groupID, new KafkaSchemas.TaxiSpeedDeserializer))))
      context.watch(context.system.actorOf(Props(new TaxiDataReceiver("taxi-speeding", groupID, new KafkaSchemas.TaxiSpeedingDeserializer))))

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
