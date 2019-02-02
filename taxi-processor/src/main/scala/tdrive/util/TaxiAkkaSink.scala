package tdrive.util

import akka.actor.{ActorPath, ActorRef, ActorSelection, ActorSystem}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import tdrive.TaxiJob
import tdrive.util.TaxiAkkaSink.initialContacts

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 01.02.19
  */
class TaxiAkkaSink[IN]() extends SinkFunction[IN] {

  lazy val remote: ActorRef = {
    val system = TaxiJob.akkaSystem
    val settings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val remote: ActorRef = system.actorOf(
      ClusterClient.props(settings), s"TaxiAkkaSink-${this.hashCode()}"
    )

    remote
  }

  override def invoke(value: IN, context: SinkFunction.Context[_]): Unit = {
    remote ! ClusterClient.Send("/user/serviceA", value, localAffinity = true)
  }

}

object TaxiAkkaSink {

  protected val initialContacts: Set[ActorPath] = Set(
    ActorPath.fromString("akka.tcp://TaxiWebServer@127.0.0.1:2551/system/receptionist"),
    ActorPath.fromString("akka.tcp://TaxiWebServer@127.0.0.1:2552/system/receptionist")
  )

}
