package tdrive

import java.io.{File, FileInputStream, FileReader}
import java.nio.ByteBuffer
import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.zip.GZIPInputStream

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Try
import Utils.RichGzipInputStream

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
object KafkaIngestor extends App {

  val StandardConfigFile = "config/ingestor.properties"
  val inputFilePath      = Try(args(1)).getOrElse(StandardConfigFile)

  val properties = {
    val prop = new Properties()
    val configReader = new FileReader(new File(inputFilePath))
    prop.load(configReader)

    prop
  }

  val taxiData = new GZIPInputStream(
    new FileInputStream(new File(properties.getProperty("ingestor.input") + ".gz")),
    4096
  )
  val metaData = {
    val prop = new Properties()
    val in = new FileReader(new File(properties.getProperty("ingestor.input")) + ".properties")
    prop.load(in)

    prop
  }

  val taxiDataSize = {
    java.lang.Short.BYTES + java.lang.Integer.BYTES + java.lang.Float.BYTES * 2
  }

  val kafkaProps = new Properties()
  kafkaProps.put("bootstrap.servers", properties.getProperty("ingestor.bootstrap.servers"))
  kafkaProps.put("client.id", "TDrive-Ingestor")
  kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

  val producer = new KafkaProducer[String, Array[Byte]](kafkaProps)

  val buffer = Array.fill[Byte](taxiDataSize)(0x0)
  val sleepTime = properties.getProperty("ingestor.sleep").toFloat

  val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  var currentTime = LocalDateTime
    .parse(metaData.getProperty("start_time"), dateFmt)
    .atZone(ZoneId.systemDefault())
    .toEpochSecond

  val progressOut = properties.getProperty("ingestor.progress").toInt
  val records = metaData.getProperty("records").toInt

  Stream
    .continually(taxiData.readFully(buffer))
    .takeWhile(_ == taxiDataSize)
    .zipWithIndex
    .foreach{ case (_, idx) =>
      val view = ByteBuffer.wrap(buffer.drop(java.lang.Short.BYTES))
      val time = view.asIntBuffer().get()
      val delta = time - currentTime

      Thread.sleep((delta * sleepTime * 1000L).toInt)

      currentTime += delta
      producer.send(new ProducerRecord[String, Array[Byte]]("taxi", buffer))

      if (idx % progressOut == 0)
        println(s"Send $idx / $records (${(idx.toFloat/records.toFloat*100F).formatted("%3.2f")}%) to kafka")

    }

  producer.close()
}
