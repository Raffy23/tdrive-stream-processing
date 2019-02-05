package tdrive.util

import java.nio.ByteBuffer
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.TimeZone

import org.apache.flink.api.common.serialization.{DeserializationSchema, SerializationSchema}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala.createTypeInformation
import tdrive.shared.dto.{Taxi, TaxiLocation, TaxiSpeed, TaxiSpeeding}

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 04.02.19
  */
object KafkaSchemas {

  class TaxiKafkaSchema extends DeserializationSchema[Taxi] {
    private lazy val defaultZone: ZoneId = TimeZone.getDefault.toZoneId

    override def deserialize(message: Array[Byte]): Taxi = {
      val buffer = ByteBuffer.wrap(message)
      Taxi(
        buffer.getShort,
        LocalDateTime.ofInstant(Instant.ofEpochSecond(buffer.getInt.toLong), defaultZone),
        buffer.getFloat,
        buffer.getFloat
      )
    }

    override def isEndOfStream(nextElement: Taxi): Boolean = nextElement.id == -1
    override def getProducedType: TypeInformation[Taxi] = createTypeInformation
  }

  class TaxiLocationKafkaSchema extends SerializationSchema[TaxiLocation] {
    override def serialize(element: TaxiLocation): Array[Byte] = {
      val buffer = ByteBuffer.allocate(java.lang.Short.BYTES + java.lang.Double.BYTES)
      buffer.putShort(element.id)
      buffer.putFloat(element.lat)
      buffer.putFloat(element.long)

      buffer.array()
    }
  }

  class TaxiSpeedKafkaSchema extends SerializationSchema[TaxiSpeed] {
    override def serialize(element: TaxiSpeed): Array[Byte] = {
      val buffer = ByteBuffer.allocate(java.lang.Short.BYTES + java.lang.Double.BYTES)
      buffer.putShort(element.id)
      buffer.putDouble(element.speed)

      buffer.array()
    }
  }

  class TaxiSpeedingKafkaSchema extends SerializationSchema[TaxiSpeeding] {
    override def serialize(element: TaxiSpeeding): Array[Byte] = {
      val buffer = ByteBuffer.allocate(java.lang.Short.BYTES + java.lang.Double.BYTES)
      buffer.putShort(element.id)
      buffer.putDouble(element.speed)

      buffer.array()
    }
  }
}
