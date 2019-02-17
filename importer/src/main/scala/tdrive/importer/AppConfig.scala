package tdrive.importer

import java.nio.file.Paths

import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.02.2019
  */
object AppConfig {

  import pureconfig.generic.auto._
  case class ImporterConfig(importer: Config)
  case class Config(inputFile: String, outputFile: String, filterTaxis: Boolean,
                    filterRadius: Float, whitelist: List[Int])


  def conf(args: Array[String], default: String): Config = pureconfig
    .loadConfig[ImporterConfig](Paths.get(Try(args(0)).getOrElse(default)))
    .fold(e => throw new RuntimeException(e.toString), x => x.importer)

}

