package tdrive.util

import com.redis.RedisClientPool
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import tdrive.dto.{TaxiLocation, TaxiSpeed}
import tdrive.util.TaxiRedisSink.Settings

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */

object TaxiRedisSink {
  case class Settings(host: String, port: Int, maxConnections: Int)

  def createTaxiLocationSink(redisSettings: Settings) = new TaxiRedisSink[TaxiLocation](
    redisSettings,
    x => s"location.${x.id}",
    x => s"${x.lat}, ${x.long}"
  )

  def createTaxiSpeedingSink(redisSettings: Settings) = new TaxiRedisSink[TaxiSpeed](
    redisSettings,
    x => s"speeding.${x.id}",
    x => s"${x.speed}"
  )

  def createTaxiSpeedSink(redisSettings: Settings) = new TaxiRedisSink[TaxiSpeed](
    redisSettings,
    x => s"speed.${x.id}",
    x => s"${x.speed}"
  )

}

class TaxiRedisSink[IN](settings: Settings, keySerializer: IN => String, valueSerializer: IN => String)  extends SinkFunction[IN] {

  private lazy val redis = new RedisClientPool(settings.host, settings.port, settings.maxConnections)

  override def invoke(value: IN, context: SinkFunction.Context[_]): Unit = {
    redis.withClient(_.set(s"taxi.${keySerializer(value)}", valueSerializer(value)))
  }

}
