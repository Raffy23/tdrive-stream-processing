package tdrive.importer

import java.io._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.Properties
import java.util.zip.{GZIPOutputStream, ZipFile}

import tdrive.importer.dto.TaxiEntry
import tdrive.importer.util.{IOUtil, LocalDateTimeOrdering}
import tdrive.shared.Haversine

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.Try
import tdrive.shared.Implicits.RichProperties


/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 31.01.19
  */
object TaxiDataImporter extends App {

  val StandardConfigFile = "config/importer.properties"
  val inputFilePath      = Try(args(0)).getOrElse(StandardConfigFile)

  val properties = {
    val prop = new Properties()
    val configReader = new FileReader(new File(inputFilePath))
    prop.load(configReader)

    prop
  }

  val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val TaxiDataPattern = "(\\d+),([^,]+),([^,]+),([^,]+)".r
  val BeijingLat  = 39.904211
  val BeijingLong = 116.407395

  val zis = new ZipFile(properties.getFilePath("importer.input"))
  val filterFarAwayTaxis = properties.getProperty("importer.filter-taxis").toBoolean
  val filterRadius = properties.getProperty("importer.filter-radius", "0.0").toDouble

  val defaultZone = ZoneId.systemDefault()
  val distanceMap = new TrieMap[Int, Double]()
  //val whitelist = List(2560, 8179, 366, 8717, 534, 4798, 1277, 5860, 8662, 9415, 6665, 2669, 9946,
  //  9945, 9944, 750, 2884, 6464, 4177, 3961, 9468, 9579, 9949, 5071, 8696, 10287, 28, 9109, 7971,
  //  9537, 8568, 9138, 9548, 8094, 10011, 4363, 3572, 10012, 3899, 6068, 315, 6876, 1336, 1622, 1359,
  //  9905, 618, 1574, 3365, 8594) // 50 taxis sorted by total km in 50 km radius


  val converted = IOUtil.readTaxis(zis).map{ case (taxiID, zipEntry) => Future {
    val in = Source.fromInputStream(zis.getInputStream(zipEntry)).getLines().toStream.map {
      case TaxiDataPattern(_, date, long, lat) => TaxiEntry(LocalDateTime.parse(date, dateFmt), long.toDouble, lat.toDouble)
    }

    if(in.nonEmpty) {
      var currentEntry = in.head

      in.drop(1).filter { e =>
        val deltaS = currentEntry.timestamp.until(e.timestamp, ChronoUnit.SECONDS)
        val distance = Haversine.haversine(e.latitude, e.longitude, currentEntry.latitude, currentEntry.longitude)
        val speed = distance / deltaS * 3600F

        val corrupt = e.latitude == 0D && e.longitude == 0D
        val same = currentEntry.latitude == e.latitude && currentEntry.longitude == e.longitude
        val tooFarAway =
          filterFarAwayTaxis &&
          Haversine.haversine(currentEntry.latitude, currentEntry.longitude,BeijingLat, BeijingLong) > filterRadius

        val goodEntry = speed <= 120 && !tooFarAway
        if (goodEntry) {
          distanceMap(taxiID) = distanceMap.getOrElse(taxiID, 0D) + distance
          currentEntry = e
        }

        goodEntry && !same && !corrupt
      }.map { e => (taxiID, e) }.toList
    } else {
      println("INFO: No Taxi data for ID: " + taxiID)
      List.empty
    }
  }}

  val outputStream = new FileOutputStream(properties.getFilePath("importer.output") + ".gz")
  val gzipOutputStream = new GZIPOutputStream(outputStream)
  val out = new DataOutputStream(gzipOutputStream)

  val sorted = Await.result(
    Future.sequence(converted.toList),
    1 hour
  ).flatten.sortBy(_._2.timestamp)

  //.sortBy(-_.size).take(5000).map(l => (l.head._1, l)).sortBy(x => -distanceMap(x._1)).take(500).flatMap {
  //case (id, x) =>
  //  print(id + ", ")
  //  x
  //}.sortBy(_._2.timestamp)

  println("")
  println("First Date: " + sorted.head._2.timestamp)
  println("Last Date : " + sorted.last._2.timestamp)

  println(s"Writing ${sorted.size} entries ...")
  sorted.foreach{ case (taxiID, data) =>
    out.writeShort(taxiID)
    out.writeInt(data.timestamp.atZone(defaultZone).toEpochSecond.toInt)
    out.writeFloat(data.longitude.toFloat)
    out.writeFloat(data.latitude.toFloat)
  }

  out.flush()
  out.close()

  println(s"Wrote ${sorted.size} data points")
  val metaOut = new FileWriter(new File(properties.getFilePath("importer.output") + ".properties"))
  //metaOut.write(
  //  s"""
  //     | {
  //     |   "start_time": "${sorted.head._2.timestamp.toString.replace("T", " ")}"
  //     |   "end_time": "${sorted.last._2.timestamp.toString.replace("T", " ")}"
  //     |   "records": ${sorted.size}
  //     | }
  //  """.stripMargin
  //)

  val propMetaOut = new Properties()
  propMetaOut.setProperty("start_time", s"${sorted.head._2.timestamp.toString.replace("T", " ")}")
  propMetaOut.setProperty("end_time", s"${sorted.last._2.timestamp.toString.replace("T", " ")}")
  propMetaOut.setProperty("records", sorted.size.toString)
  propMetaOut.store(metaOut, "")

  metaOut.flush()
  metaOut.close()


  println("Done!")
}
