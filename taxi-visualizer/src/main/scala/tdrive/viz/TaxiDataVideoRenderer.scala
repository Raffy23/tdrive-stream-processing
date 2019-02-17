package tdrive.viz

import java.awt.Color
import java.awt.image.{BufferedImage, DataBufferByte}
import java.io.{BufferedReader, FileInputStream, InputStreamReader}
import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}
import java.util.zip.GZIPInputStream

import javax.imageio.ImageIO
import org.joml.Matrix4f
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
import tdrive.shared.Implicits.RichGzipInputStream
import tdrive.viz.gl.VertexArrayObject
import tdrive.viz.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by 
  *
  * @author Raphael Ludwig
  * @version 08.02.19
  */
object TaxiDataVideoRenderer  extends App {

  val phi = (1 + math.sqrt(5D)) / 2

  val SRC_WIDTH  = AppConfig.conf.videoWidth
  val SRC_HEIGHT = AppConfig.conf.videoHeight
  val whitelist = AppConfig.conf.whitelist.toSet

  println(s"Parsing data from ${AppConfig.conf.inputFile} ...")
  case class Taxi(id: Int, time: Long, long: Float, lat: Float)
  val taxiData = {
    //val defaultZone: ZoneId = TimeZone.getDefault.toZoneId
    val taxiDataSize = {
      java.lang.Short.BYTES + java.lang.Integer.BYTES + java.lang.Float.BYTES * 2
    }

    val in = new GZIPInputStream(new FileInputStream(AppConfig.conf.inputFile), 1024 * 8)
    val inBuffer = Array.fill[Byte](taxiDataSize)(0x0)

    val out = Stream
      .continually(in.readFully(inBuffer))
      .takeWhile(_ == taxiDataSize)
      .map { _ =>
        val buffer = ByteBuffer.wrap(inBuffer)
        Taxi(
          buffer.getShort,
          buffer.getInt.toLong,
          buffer.getFloat,
          buffer.getFloat
        )
      }.toVector

    in.close()

    if (whitelist.nonEmpty) out.filter(whitelist contains _.id)
    else out
  }

  val minLong = taxiData.map(_.long).min
  val maxLong = taxiData.map(_.long).max
  val minLat  = taxiData.map(_.lat).min
  val maxLat  = taxiData.map(_.lat).max

  println("Creating native Buffer for OpenGL ...")
  val nativeBuffer = BufferUtils.createFloatBuffer(taxiData.size * (4 /*rgba*/ + 2 /*lat,long*/))
  taxiData.foreach { taxi =>
    @inline def normalize(x: Double, min: Double, max: Double): Double = (x - min) / (max - min)
    @inline def normLat(x: Double): Double  = normalize(x, minLat, maxLat) * SRC_HEIGHT
    @inline def normLong(x: Double): Double = normalize(x, minLong, maxLong) * SRC_WIDTH

    val hue = taxi.id * phi - (taxi.id * phi).floor
    val color = Color.getHSBColor(hue.toFloat, 1.0F, 1.0F)

    nativeBuffer.put(color.getRed / 255F)
    nativeBuffer.put(color.getGreen / 255F)
    nativeBuffer.put(color.getBlue / 255F)
    nativeBuffer.put(0F)

    nativeBuffer.put(normLong(taxi.long).toFloat)
    nativeBuffer.put(normLat(taxi.lat).toFloat)
  }
  nativeBuffer.flip()

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

  val window = new Window("TaxiVisualizer", SRC_WIDTH, SRC_HEIGHT, mode = 0, resizeable = false, debug = debugFlag)

  val VAO = VertexArrayObject.create()
  val PlaneVBO = glGenBuffers()

  VAO.bind { _ =>
    glBindBuffer(GL_ARRAY_BUFFER, PlaneVBO)
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

    VAO.attribute(0, 3, GL_FLOAT, normalized = false, 3 * sizeof(`float`), 0)
  }

  val taxiDataVBO = glGenBuffers()
  glBindBuffer(GL_SHADER_STORAGE_BUFFER, taxiDataVBO)
  glBufferData(GL_SHADER_STORAGE_BUFFER, nativeBuffer, GL_STATIC_DRAW)
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
    Shader.fromResource("triangle", "shader/taxi-cpu.vert", "shader/taxi-cpu.frag")
  })

  if (debugFlag) window.show()
  window.setVsync(false)
  window.focus()

  glClearColor(0.16f, 0.16f, 0.16f, 1)

  val defaultZone = ZoneId.systemDefault()
  val start = LocalDateTime.parse("2008-02-02T13:30:45", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  val end = LocalDateTime.parse("2008-02-08T17:39:19", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  var current = start

  shader.use()
  shader.setUniform("projection", ortho)

  glDisable(GL_DEPTH_TEST)

  var last = System.currentTimeMillis()
  var frames = 0

  val screenBuffer = BufferUtils.createByteBuffer(SRC_HEIGHT * SRC_WIDTH * 3)

  ImageIO.setUseCache(false)
  val ffmpegBuilder = new ProcessBuilder(
    AppConfig.conf.ffmpegBinary,
    "-y",
    "-threads", "7",
    "-f", "image2pipe",
    "-vcodec", "bmp",
    "-r", "60",
    "-framerate", "60",
    "-i", "-",
    "-c:v", "libx264",
    "-preset", "veryslow",
   AppConfig.conf.videoOutput
  )

  ffmpegBuilder.redirectErrorStream(true)

  println("Starting ffmpeg ...")
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
  var prevPos  = 0
  var prevBase = 0
  println(s"Rendering: ${taxiData.size} entries")

  while (current.isBefore(end) && !window.isClosed) {
    val prev = current.minus(90, ChronoUnit.MINUTES).atZone(defaultZone).toEpochSecond
    val strTimestamp = current.toString.replace("T"," ")
    current = current.plus(1, ChronoUnit.MINUTES)

    glBindFramebuffer(GL_FRAMEBUFFER, fbos(currentFBO))
    glViewport(0, 0, SRC_WIDTH, SRC_HEIGHT)
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

    val currentS = current.atZone(defaultZone).toEpochSecond
    var pos      = taxiData.indexWhere(_.time > currentS, prevPos)
    var base     = taxiData.indexWhere(_.time > prev, prevBase)

    pos = if (pos == -1) taxiData.size - 1 else pos
    base = if (base == -1) pos else base

    prevPos = pos
    prevBase = math.max(0, base)

    if (pos-base > 0)
      VAO.bind { _ =>
        //shader.setUniform(shader_Base, pos-base)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, taxiDataVBO)
        glDrawArraysInstancedBaseInstance(GL_TRIANGLES, 0, 6, pos-base, base)
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

  println("Waiting for encoder to finish ...")
  Await.result(backgroundTask, 1 hour)

  println("Cleaning up ...")
  window.close()
  window.destroy()
  glfwTerminate()

}
