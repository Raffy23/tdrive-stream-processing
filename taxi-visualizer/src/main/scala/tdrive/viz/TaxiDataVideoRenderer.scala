package tdrive.viz

import java.awt.Color
import java.awt.image.{BufferedImage, DataBufferByte}
import java.io.{BufferedReader, InputStreamReader}
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}
import java.util.zip.ZipFile

import javax.imageio.ImageIO
import org.joml.{Matrix4f, Vector2f}
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.{glfwPollEvents, glfwTerminate}
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GL32._
import org.lwjgl.opengl.GL42._
import org.lwjgl.opengl.GL43._
import tdrive.shared.Haversine
import tdrive.viz.gl.VertexArrayObject
import tdrive.viz.util.{IOUtils, _}

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 08.02.19
  */
object TaxiDataVideoRenderer  extends App {

  val SRC_WIDTH  = 1920
  val SRC_HEIGHT = 1080

  val TaxiDataPattern = "(\\d+),([^,]+),([^,]+),([^,]+)".r
  val zis = new ZipFile("./T-drive Taxi Trajectories.zip")


  //val whitelist = List(
  //  8554, 313, 9859, 258, 8276, 3975, 10173, 8717, 259, 9250, 9181, 9578, 318, 2342, 9949, 3617,
  //  8766, 5968, 6221, 1172, 8692, 6612, 1563, 4635, 901, 8603, 7149, 2154, 4347, 2655, 2796, 5638,
  //  6335, 7597, 116, 2750, 3473, 4091, 5970, 1332, 3234, 1631, 5125, 3658, 2728, 5063, 3983, 5303,
  //  10113, 2293, 5238, 5450, 152, 2839, 1176, 3857, 9725, 9151, 3713, 9385, 3980, 3659, 4167, 4138,
  //  808, 2435, 8557, 2587, 9937, 8326, 8553, 4819, 9644, 3671, 7002, 658, 1353, 751, 2294, 1338,
  //  5719, 7709, 3107, 2586, 304, 9724, 6876, 2283, 9152, 4232, 5515, 9556, 7610, 3848, 9766, 9863,
  //  1033, 9032, 3163, 4620
  //)

  val whitelist = List(8554,313,258,8276,3975,10173,8717,259,9250,9181,9578,318,2342,9949,3617,
    8766,5968,6221,1172,8692,6612,1563,4635,901,8603,7149,2154,4347,2655,2796,6335,7597,116,
    2750,3473,4091,5970,3234,3658,2728,5063,3983,5303,10113,5450,152,3857,9151,3713,3980,8557,
    2587,9937,8553,4819,9644,3671,7002,658,1353
  )

  //val whitelist = List(2560, 8179, 366, 8717, 534, 4798, 1277, 5860, 8662, 9415, 6665, 2669, 9946,
  //  9945, 9944, 750, 2884, 6464, 4177, 3961, 9468, 9579, 9949, 5071, 8696, 10287, 28, 9109, 7971,
  //  9537, 8568, 9138, 9548, 8094, 10011, 4363, 3572, 10012, 3899, 6068, 315, 6876, 1336, 1622, 1359,
  //  9905, 618, 1574, 3365, 8594) // 50 taxis sorted by total km in 50 km radius

  var minLong = Double.MaxValue
  var maxLong = Double.MinValue
  var minLat  = Double.MaxValue
  var maxLat  = Double.MinValue

  // TODO: Rewrite to importer file / kafka source
  val taxiDrivenKM = new TrieMap[Int, Double]()
  val inputTmp = IOUtils
    .readTaxis(zis)
    .filter(whitelist contains _._1)
    .map{ case (taxiID, zipEntry) =>
      (
        taxiID,
        Future {
          val dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

          val source = Source
            .fromInputStream(zis.getInputStream(zipEntry))
            .getLines()
            .toStream
            .map { case TaxiDataPattern(_, date, long, lat) =>
              ((dateFmt.parse(date).getTime / 1000).toInt, long.toFloat, lat.toFloat)
            }

          if (source.nonEmpty) {
            var current = source.head

            source.drop(1).filter{ case e @ (timestamp, long, lat) =>
              val deltaS = timestamp - current._1
              val distance = Haversine.haversine(lat, long, current._3, current._2)
              val speed = distance / deltaS * 3600

              val corrupt = lat == 0D && long == 0D
              val same = current._2 == long && current._3 == lat
              val tooFarAway = Haversine.haversine(lat, long,39.904211, 116.407395) > 15

              val goodEntry = speed <= 100 && !tooFarAway
              if (goodEntry) {
                taxiDrivenKM(taxiID) = taxiDrivenKM.getOrElse(taxiID, 0D) + distance
                //println(distance)
                //println(speed)
                current = e
              }

              goodEntry && !same && !corrupt
            }.map {
              case (date, long, lat) =>
                val tLong = long.toDouble
                val tLat  = lat.toDouble

                minLong = math.min(minLong, tLong)
                maxLong = math.max(maxLong, tLong)
                minLat  = math.min(minLat , tLat )
                maxLat  = math.max(maxLat , tLat )

                (date.toInt, tLong, tLat)
            }.toList
          } else {
            List.empty
          }

        }
      )
    }.map{ case (taxiID, data) =>
    def normalize(x: Double, min: Double, max: Double): Double = (x - min) / (max - min)
    def normLat(x: Double): Double  = normalize(x, minLat, maxLat) * SRC_HEIGHT
    def normLong(x: Double): Double = normalize(x, minLong, maxLong) * SRC_WIDTH

    data.map { data =>
      (
        taxiID,
        data.map(_._1),
        {
          val nativeBuffer = BufferUtils.createFloatBuffer(data.size * 2)
          data.foreach { case (_, long, lat) =>
            nativeBuffer.put(normLong(long).toFloat)
            nativeBuffer.put(normLat(lat).toFloat)
          }

          nativeBuffer
        }
      )
    }
  }.toList

  val taxiData = Await.result(Future.sequence(inputTmp), 1 hour)
  //val filtered = taxiDrivenKM.toList.sortBy(-_._2).take(50).map(_._1)
  //println(taxiDrivenKM.toList.sortBy(-_._2).take(50).map(_._1))

  zis.close()

  val framePosition = Array.fill[Int](10358/*taxiData.size*/)(0)
  val pframePosition = Array.fill[Int](10358/*taxiData.size*/)(0)

  val quadSize = 2
  val vertices = Array(
    0f      , 0f       , 0.0f,
    quadSize, 0f       , 0.0f,
    0f      , quadSize , 0.0f,
    quadSize, 0f       , 0.0f,
    quadSize, quadSize , 0.0f,
    0f      , quadSize , 0.0f
  )

  // Init OpenGL
  val debugFlag = false

  if( debugFlag ) {
    println(s"[DEBUG] Attach Renderdoc to PID: ${SystemUtils.getPID}")
    println("[DEBUG] Press enter when Renderdoc is attached ....")
    System.in.read()
    println("[DEBUG] glfw will be initialized ...")
  }


  // https://github.com/g-truc/glm/blob/0ceb2b755fb155d593854aefe3e45d416ce153a4/glm/ext/matrix_clip_space.inl#L4
  val ortho = new Matrix4f(
    2F / SRC_WIDTH, 0, 0, 0,
    0, 2F / -SRC_HEIGHT, 0, 0,
    0, 0, -1, 0,
    -1, 1, 0, 1
  )

  val window = new Window("Hello World!", 800, 640, mode = 0, resizeable = false, debug = debugFlag)

  // Upload Buffers as SSBO

  val VAO = VertexArrayObject.create()
  val PlaneVBO = glGenBuffers()

  VAO.bind { _ =>
    glBindBuffer(GL_ARRAY_BUFFER, PlaneVBO)
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

    VAO.attribute(0, 3, GL_FLOAT, normalized = false, 3 * sizeof(`float`), 0)
  }

  val phi = (1 + math.sqrt(5D)) / 2

  val ColorVBO = glGenBuffers()
  val colorNativeBuffer = BufferUtils.createFloatBuffer(10358*4)
  (0 to 10357).foreach{ id =>
    val hue = id*phi - (id * phi).floor
    val color = Color.getHSBColor(hue.toFloat, 1.0F, 1.0F)

    colorNativeBuffer.put(color.getRed/255F)
    colorNativeBuffer.put(color.getGreen/255F)
    colorNativeBuffer.put(color.getBlue/255F)
    colorNativeBuffer.put(1F)
  }

  colorNativeBuffer.flip()
  glBindBuffer(GL_SHADER_STORAGE_BUFFER, ColorVBO)
  glBufferData(GL_SHADER_STORAGE_BUFFER, colorNativeBuffer, GL_STATIC_DRAW)
  glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)

  def createFBO(): (Int, Int) = {
    val FBO = glGenFramebuffers()
    glBindFramebuffer(GL_FRAMEBUFFER, FBO)

    val renderedTexture = glGenTextures()
    glBindTexture(GL_TEXTURE_2D, renderedTexture)
    glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, SRC_WIDTH, SRC_HEIGHT, 0,GL_RGB, GL_UNSIGNED_BYTE, 0)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTexture, 0)
    glDrawBuffers(Array(GL_COLOR_ATTACHMENT0))
    glBindFramebuffer(GL_FRAMEBUFFER, 0)

    (FBO, renderedTexture)
  }

  val fbo1 = createFBO()
  val fbo2 = createFBO()
  val fbo3 = createFBO()
  val fbo4 = createFBO()

  if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE){
    System.err.println("Error while setting up framebuffers!")
    System.exit(-1)
  }

  val shader = memory(implicit stack => {
    Shader.fromFile("triangle", "./resources/shader/taxi-cpu.vert", "./resources/shader/taxi-cpu.frag")
  })


  val taxiBuffers = taxiData.filter(_._2.nonEmpty).map{ case (id, times, nativeBuffer) =>
    (
      id,
      times,
      nativeBuffer,
      {
        val VAO = VertexArrayObject.create()
        val VBO = glGenBuffers()

        nativeBuffer.flip()

        VAO.bind{VAO =>
          glBindBuffer(GL_ARRAY_BUFFER, PlaneVBO)
          VAO.attribute(0, 3, GL_FLOAT, normalized = false, 3 * sizeof(`float`), 0)
          VAO.attributeDivisor(0, 0)
          glBindBuffer(GL_ARRAY_BUFFER, 0)

          glBindBuffer(GL_SHADER_STORAGE_BUFFER, VBO)
          glBufferData(GL_SHADER_STORAGE_BUFFER, nativeBuffer, GL_STATIC_DRAW)
          glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, VBO)
          glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        }

        (VAO, VBO)
      }
    )
  }

  if (glGetError() != GL_NO_ERROR) {
    System.err.println("Some error while uploading buffers")
  }

  if (debugFlag) window.show()
  window.setVsync(false)
  window.focus()

  glClearColor(0.16f, 0.16f, 0.16f, 1)

  val defaultZone = ZoneId.systemDefault()
  val start = LocalDateTime.parse("2008-02-02T13:30:45", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  val end = LocalDateTime.parse("2008-02-08T17:39:19", DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  var current = start

  val position = new Vector2f(40f,105f)

  shader.use()
  shader.setUniform("projection", ortho)

  glDisable(GL_DEPTH_TEST)

  var last = System.currentTimeMillis()
  var frames = 0

  val shader_ColorID = shader.getUniformLocation("colorID")
  val shader_Base    = shader.getUniformLocation("base")

  val screenBuffer = BufferUtils.createByteBuffer(SRC_HEIGHT * SRC_WIDTH * 3)

  ImageIO.setUseCache(false)
  val ffmpegBuilder = new ProcessBuilder(
    "ffmpeg", //"I:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
    "-y",
    "-threads", "7",
    "-f", "image2pipe",
    "-vcodec", "bmp",
    "-r", "60",
    "-framerate", "60",
    "-i", "-",
    "-c:v", "libx264",
    //"-preset", "veryslow",
    "./taxi_sortBy_count_distance_limit_50.mkv"
  )

  ffmpegBuilder.redirectErrorStream(true)

  val ffmpeg = ffmpegBuilder.start()
  val imgOut = ffmpeg.getOutputStream

  Future {
    val in = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream))

    while (ffmpeg.isAlive) {
      System.err.println(in.readLine())
    }

  }

  val fbos     = Array(fbo1._1, fbo2._1)//, fbo3._1, fbo4._1)
  val readBack = Array.fill[Boolean](fbos.length)(false)
  var currentFBO = fbos.length / 2

  val rendering = new AtomicBoolean(true)
  val ioTaskQueue = new ArrayBlockingQueue[(String,BufferedImage)](256)

  val backgroundTask = Future {
    while (rendering.get()) {
      val task = ioTaskQueue.poll(100, TimeUnit.MILLISECONDS)

      if (task != null) {
        val imageBuffer = task._2
        val strTimestamp = task._1

        val g = imageBuffer.getGraphics
        g.setColor(Color.WHITE)
        g.drawString(strTimestamp, 4, g.getFontMetrics.getHeight+2)

        ImageIO.write(imageBuffer, "bmp", imgOut)
      }
    }

    imgOut.flush()
    imgOut.close()
  }



  var imgPosition = 0
  val taxisToRender = taxiBuffers//.filter(filtered contains _._1)
  println(s"Rendering: ${taxisToRender.size} taxis")

  while (current.isBefore(end) && !window.isClosed) {
    val prev = current.minus(90, ChronoUnit.MINUTES).atZone(defaultZone).toEpochSecond
    val strTimestamp = current.toString.replace("T"," ")
    current = current.plus(1, ChronoUnit.MINUTES)

    glBindFramebuffer(GL_FRAMEBUFFER, fbos(currentFBO))
    glViewport(0, 0, SRC_WIDTH, SRC_HEIGHT)
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

    val currentS = current.atZone(defaultZone).toEpochSecond
    val async = taxisToRender.map{ case (id, times, _, x) => Future {
      var pos = times.indexWhere(_ > currentS, framePosition(id-1))
      pos = if (pos == -1) times.size - 1 else pos
      framePosition(id-1) = pos

      val tBase = times.indexWhere(_ > prev, pframePosition(id-1))
      pframePosition(id-1) = math.max(0, tBase)

      val base = if (tBase == -1) pos else tBase

      (id, pos, base, x)
    }}

    Await.result(Future.sequence(async), 1 minute).foreach{ case (id, pos, base, (vao, ssbo)) =>
      //println(pos-base)

      if (pos-base > 0)
        vao.bind { _ =>
          shader.setUniform(shader_ColorID, id.toInt - 1)
          shader.setUniform(shader_Base, (pos-base).toInt)

          glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssbo)
          glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, ColorVBO)

          glDrawArraysInstancedBaseInstance(GL_TRIANGLES, 0, 6, pos-base, base)
        }

      //(math.max(0, pos - 20) to math.min(pos, times.size-1)).foreach{ pos =>
      //  shader.setUniform("position", position.set(nativeBuffer.get(pos*2),nativeBuffer.get(pos*2+1)))
      //  VAO.bind{_ =>
      //    glDrawArrays(GL_TRIANGLES, 0, 6)
      //  }
      //}
    }

    readBack(currentFBO) = true
    if (readBack((currentFBO+1)%readBack.length)) {
      glBindFramebuffer(GL_FRAMEBUFFER, fbos((currentFBO+1)%readBack.length))
      glReadBuffer(GL_COLOR_ATTACHMENT0)
      glReadPixels(0, 0, SRC_WIDTH, SRC_HEIGHT, GL_BGR, GL_UNSIGNED_BYTE, screenBuffer)

      val imageBuffer = new BufferedImage(SRC_WIDTH, SRC_HEIGHT, BufferedImage.TYPE_3BYTE_BGR)
      val imgData = imageBuffer.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData
      screenBuffer.get(imgData)
      screenBuffer.clear()

      ioTaskQueue.put((strTimestamp, imageBuffer))

      readBack((currentFBO+1)%readBack.length) = false
    }

    currentFBO = (currentFBO+1) % fbos.length

    frames += 1
    if (System.currentTimeMillis() - last > 750) {
      println("FPS: " + frames + " (" + current + ")")
      frames = 0
      last = System.currentTimeMillis()
    }

    window.swapBuffers()
    glfwPollEvents()
  }

  println("Finished rendering, still muxing ...")
  Future {
    while(ioTaskQueue.size() > 0)
      Thread.sleep(1000)

    rendering.getAndSet(false)
    println("Finalizing output")
  }

  if (debugFlag) {
    window.setVsync(true)
    while (!window.isClosed) {
      window.swapBuffers()
      glfwPollEvents()
    }
  }

  Await.result(backgroundTask, 1 hour)

  window.close()
  window.destroy()
  glfwTerminate()

}
