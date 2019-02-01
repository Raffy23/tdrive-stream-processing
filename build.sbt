import sbt.Compile
import sbtassembly.AssemblyPlugin.autoImport.assembly

name := "Taxi - Drive"
version := "0.1"

scalaVersion := "2.12.8"

val apacheFlinkVersion = "1.7.1"

lazy val taxiProcessor = (project in file("taxi-processor"))
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-scala" % apacheFlinkVersion % "provided",
      "org.apache.flink" %% "flink-cep-scala" % apacheFlinkVersion % "provided",
      "org.apache.flink" %% "flink-streaming-scala" % apacheFlinkVersion % "provided",
      "org.apache.flink" %% "flink-connector-kafka" % apacheFlinkVersion,
      "net.debasishg" %% "redisclient" % "3.9"
    ),

    assembly / mainClass := Some("tdrive.TaxiJob"),
    Compile / run := Defaults.runTask(
      Compile / fullClasspath,
      Compile / run / mainClass,
      Compile / run / runner
    ).evaluated,


    Compile / run / fork := true,
    Global / cancelable := true,

    assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false)
  )
  .dependsOn(sharedJVM)

lazy val taxiDataImporter = (project in file("importer"))
  .dependsOn(sharedJVM)

lazy val kafkaIngestor = (project in file("kafka-ingestor"))
  .settings(
    libraryDependencies += "org.apache.kafka" %% "kafka" % "2.1.0"
  )

lazy val taxiWebServer = (project in file("web-server"))
lazy val taxiWebClient = (project in file("web-client"))

val lwjglVersion = "3.2.1"
val jomlVersion = "1.9.12"
val lwjglNatives = "natives-" + {
  val osName = sys.props("os.name").toLowerCase

  if (osName.contains("windows")) "windows"
  else if (osName.contains("linux")) "linux"
  else if (osName.contains("mac")) "macos"
  else "unknown"
}

lazy val taxiVisualizer = (project in file("taxi-visualizer"))
  .settings(
    libraryDependencies ++= Seq(
      "org.lwjgl" % "lwjgl" % lwjglVersion,
      "org.lwjgl" % "lwjgl-glfw" % lwjglVersion,
      "org.lwjgl" % "lwjgl-opengl" % lwjglVersion,

      "org.lwjgl" % "lwjgl" % lwjglVersion classifier lwjglNatives,
      "org.lwjgl" % "lwjgl-glfw" % lwjglVersion classifier lwjglNatives,
      "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier lwjglNatives,

      "org.joml" % "joml" % jomlVersion,

      //"com.github.hoary" % "JavaAV-Project" % "1.0",

      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    )
  )

lazy val sharedJVM = project in file("shared")
