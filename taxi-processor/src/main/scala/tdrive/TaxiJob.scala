package tdrive

import java.time.temporal.ChronoUnit
import java.util.Properties

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.{OutputTag, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import tdrive.dto._
import tdrive.util.{TaxiDataCounter, TaxiKafkaSchema, TaxiRedisSink}
import tdrive.Implicits._
import tdrive.shared.Haversine

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 24.01.19
  */
object TaxiJob {

  def main(cmdArgs: Array[String]): Unit = {
    val args = ParameterTool.fromArgs(cmdArgs)

    val redisSettings = TaxiRedisSink.Settings(
      args.get("redis.server"),
      args.getInt("redis.port"),
      args.getInt("redis.maxConnections", 32)
    )

    val kafkaProps = new Properties()
    kafkaProps.setProperty("bootstrap.servers", args.get("kafka.server"))

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val source = env.addSource(new FlinkKafkaConsumer[Taxi]("taxi", TaxiKafkaSchema, kafkaProps))

    val locationOutput      = OutputTag[TaxiLocation]("taxi-location")
    val currentSpeedOutput  = OutputTag[TaxiSpeed]("taxi-cur_speed")
    val avgSpeedOutput      = OutputTag[TaxiSpeed]("taxi-avg_speed")
    //val outOfAreaOutput     = OutputTag[TaxiLeftArea]("taxi-area_left")

    val redisLocationSink = TaxiRedisSink.createTaxiLocationSink(redisSettings)
    val redisSpeedSink    = TaxiRedisSink.createTaxiSpeedingSink(redisSettings)
    val redisAvgSpeedSink = TaxiRedisSink.createTaxiSpeedSink(redisSettings)

    val stream = source.keyBy(_.id).mapWithState[TaxiData, TaxiState]{ case (taxi, state) =>
      val prev      = state.getOrElse(TaxiState(taxi))
      val deltaS    = math.max(1, prev.taxi.timestamp.until(taxi.timestamp, ChronoUnit.SECONDS))
      val distance  = Haversine.haversine(taxi.long, taxi.lat, prev.taxi.long, prev.taxi.lat)
      val speed     = distance / deltaS * 3600D
      val avgSpeed  = (prev.speed * prev.count + speed) / (prev.count + 1)

      prev.update(taxi, speed)
      (TaxiData(taxi, distance, speed, avgSpeed), state)
    }
      .processToSink(locationOutput, _.taxi.asLocation, redisLocationSink)
      .processToSink(avgSpeedOutput, _.asTaxiSpeed, redisSpeedSink)
      .processToFilteredSink[TaxiSpeed](currentSpeedOutput, _.asTaxiSpeed, _.speed >= 50D, redisAvgSpeedSink)


    stream
      .timeWindowAll(Time.seconds(60))
      .aggregate(new TaxiDataCounter)
      .map(_ / 60)
      .print()
      .setParallelism(1)

    env.execute("Taxi Job")
  }

  def emitToOutput[T, S](sideOutput: OutputTag[S], func: T => S): ProcessFunction[T, T] = {
    case (value: T, ctx: ProcessFunction[T, T]#Context, out: Collector[T]) =>
      out.collect(value)
      ctx.output(sideOutput, func(value))
  }

}