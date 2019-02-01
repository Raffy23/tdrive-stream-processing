package tdrive.importer.util

import java.util.zip.{ZipEntry, ZipFile}

import scala.util.matching.Regex
import scala.collection.JavaConversions._

/**
  * Created by:
  *
  * @author Raphael
  * @version 11.01.2019
  */
object IOUtil {

  val DataFilePattern: Regex = "release/taxi_log_2008_by_id/(\\d+).txt".r
  val TaxiDataPattern: Regex = "(\\d+),([^,]+),([^,]+),([^,]+)".r

  def readTaxis(zis: ZipFile): Stream[(Int, ZipEntry)] =
    zis.entries().toStream.map( f => f.getName match {
      case DataFilePattern(taxiID) => Some((taxiID.toInt, f))
      case _ => None
    })
      .filter(_.isDefined)
      .map(_.get)

}
