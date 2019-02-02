package tdrive.server

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import tdrive.server.WebSocketActor.{WsConnected, WsDisconnected}

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

  val port    = if (args.isEmpty) 0 else args(0)
  val httPort = if (args.length <= 1) 8080 else args(1).toInt
  val config  = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.port=$port")
    .withFallback(ConfigFactory.load())

  implicit val system: ActorSystem = ActorSystem("TaxiWebServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val manager = system.actorOf(Props[ClusterManager], name = "ClusterManager")
  val broadcaster = system.actorOf(Props[TaxiDataRouter], name = "TaxidataRouter")

  val serviceA = system.actorOf(Props(new TaxiDataReceiver(broadcaster)), name = "serviceA")

  ClusterClientReceptionist(system).registerService(serviceA)


  val routes: HttpRequest => HttpResponse = {

    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) =>
          val eventActor = system.actorOf(Props(new WebSocketActor(broadcaster)))

          val sink = Flow[Message]
            .collect{ case TextMessage.Strict(json) => /*todo do something*/ println(json) }
            .to(Sink.actorRef(eventActor, WsDisconnected))

          val source = Source
            .actorRef(64, OverflowStrategy.dropHead)
            .map( (json: String) => TextMessage.Strict(json) )
            .mapMaterializedValue{actorRef =>
              eventActor ! WsConnected(actorRef)
              println("REGISTER")
              NotUsed
            }

          upgrade.handleMessagesWithSinkSource(inSink = sink, outSource = source)

        case None          => HttpResponse(400, entity = "Not a valid websocket request!")
      }

    case r: HttpRequest =>
      r.discardEntityBytes()
      HttpResponse(404, entity = "Unknown resource!")

  }


  Http().bindAndHandleSync(routes, "localhost", httPort)


  println("Press any key to stop the cluster instance ...")
  System.in.read()
  Await.result(system.terminate(), 5 minutes)

}
