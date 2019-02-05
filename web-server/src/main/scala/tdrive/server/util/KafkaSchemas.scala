package tdrive.server.util

import java.nio.ByteBuffer
import java.util

import org.apache.kafka.common.serialization.Deserializer
import tdrive.shared.dto.{TaxiLocation, TaxiSpeed, TaxiSpeeding}

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 05.02.19
  */
object KafkaSchemas {

  class TaxiLocationDeserializer extends Deserializer[TaxiLocation] {
    override def deserialize(topic: String, data: Array[Byte]): TaxiLocation = {
      val view = ByteBuffer.wrap(data)

      TaxiLocation(
        view.getShort,
        view.getFloat,
        view.getFloat
      )
    }

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
    override def close(): Unit = {}
  }

  class TaxiSpeedDeserializer extends Deserializer[TaxiSpeed] {
    override def deserialize(topic: String, data: Array[Byte]): TaxiSpeed = {
      val view = ByteBuffer.wrap(data)

      TaxiSpeed(
        view.getShort,
        view.getDouble
      )
    }

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
    override def close(): Unit = {}
  }

  class TaxiSpeedingDeserializer extends Deserializer[TaxiSpeeding] {
    override def deserialize(topic: String, data: Array[Byte]): TaxiSpeeding = {
      val view = ByteBuffer.wrap(data)

      TaxiSpeeding(
        view.getShort,
        view.getDouble
      )
    }

    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
    override def close(): Unit = {}
  }

}
