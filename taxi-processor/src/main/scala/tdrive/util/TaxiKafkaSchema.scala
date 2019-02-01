package tdrive.util

import java.nio.ByteBuffer
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.TimeZone

import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala.createTypeInformation
import tdrive.dto.Taxi

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
object TaxiKafkaSchema extends DeserializationSchema[Taxi] {

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
