package tdrive.viz

import java.nio.file.Paths

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.02.2019
  */
object AppConfig {

  import pureconfig.generic.auto._
  case class VisualizerConfig(visualizer: Config)
  case class Config(inputFile: String, whitelist: List[Int], ffmpegBinary: String,
                    videoWidth: Int, videoHeight: Int, videoOutput: String)


  lazy val conf: Config = pureconfig
    .loadConfig[VisualizerConfig](Paths.get("./config/visualizer.conf"))
    .fold(e => throw new RuntimeException(e.toString), x => x.visualizer)

}
