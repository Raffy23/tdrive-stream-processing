package tdrive.server

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import tdrive.server.WebSocketActor.{WsConnected, WsDisconnected}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 01.02.19
  */
object WebServer extends App {

  val port    = if (args.isEmpty) 0 else args(0)
  val httPort = if (args.length <= 1) 8080 else args(1).toInt
  val enableKafka = if ((if(args.length <= 2) 1 else args(2).toInt) == 1) true else false

  val config  = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.port=$port")
    .withFallback(ConfigFactory.load())

  implicit val system: ActorSystem = ActorSystem("TaxiWebServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val manager = system.actorOf(Props[ClusterManager], name = "ClusterManager")
  if (enableKafka)
    manager ! "SpawnKafka"

  val routes: HttpRequest => HttpResponse = {

    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) =>
          Try{
            val eventActor = system.actorOf(Props[WebSocketActor])

            val sink = Flow[Message]
              .collect{ case TextMessage.Strict(json) => /*todo do something*/ println(json) }
              .to(Sink.actorRef(eventActor, WsDisconnected))

            val source = Source
              .actorRef(64, OverflowStrategy.dropHead)
              .map( (json: String) => TextMessage.Strict(json) )
              .mapMaterializedValue{actorRef =>
                eventActor ! WsConnected(actorRef)
                NotUsed
              }

            upgrade.handleMessagesWithSinkSource(inSink = sink, outSource = source)
          }.recover {
            case e: Exception => e.printStackTrace()
              HttpResponse(500, entity = "woops")
          }.get

        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }

    case r: HttpRequest =>
      r.discardEntityBytes()
      HttpResponse(404, entity = "Unknown resource!")

  }


  Http().bindAndHandleSync(routes, "localhost", httPort)


  println("Press any key to stop the cluster instance ...")
  System.in.read()

  manager ! "Shutdown"
  Await.result(system.terminate(), 5 minutes)

}
