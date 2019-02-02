package tdrive.server

import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 01.02.19
  */
object WebServer extends App {

  val port   = if (args.isEmpty) 0 else args(0)
  val config = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.port=$port")
    .withFallback(ConfigFactory.load())

  val system  = ActorSystem("TaxiWebServer", config)
  val manager = system.actorOf(Props[ClusterManager], name = "ClusterManager")

  val serviceA = system.actorOf(Props[TestActor], name = "serviceA")
  ClusterClientReceptionist(system).registerService(serviceA)

  println("Press any key to stop the cluster instance ...")
  System.in.read()
  Await.result(system.terminate(), 5 minutes)

}
