package tdrive.server.kafka

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import scala.concurrent.duration._
import scala.language.postfixOps
/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 02.02.19
  */
object TaxiDataReceiver {
  object Done
}
class TaxiDataReceiver[T](topic: String, groupID: String, deserializer: Deserializer[T]) extends Actor with ActorLogging {

  protected implicit val timeout: Timeout = Timeout(5 seconds)
  protected implicit val materializer: ActorMaterializer = ActorMaterializer()

  protected val mediator: ActorRef = DistributedPubSub.get(context.system).mediator

  private lazy val consumerSettings = ConsumerSettings(
    ConfigFactory.load("kafka.conf"),
    new StringDeserializer, deserializer
  ).withGroupId(groupID)

  private lazy val kafkaConsumer = Consumer
    .plainSource(consumerSettings, Subscriptions.topics(topic))
    .mapAsyncUnordered(1)(record => self ? record)
    .toMat(Sink.seq)(Keep.both)
    .mapMaterializedValue(DrainingControl.apply)
    .run()

  override def preStart(): Unit = {
    kafkaConsumer.isShutdown.isCompleted
  }

  override def postStop(): Unit = {
    kafkaConsumer.shutdown()
  }

  override def receive: Receive = {
    case record: ConsumerRecord[Any, T]  =>
      mediator ! Publish("taxi-data", record.value())
      sender() ! TaxiDataReceiver.Done
  }

}

