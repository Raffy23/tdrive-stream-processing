package tdrive.viz

import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.StrictLogging
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.{GL, GLUtil}
import org.lwjgl.opengl.GL11.{GL_DONT_CARE, glEnable, glViewport}
import org.lwjgl.opengl.GL43.{GL_DEBUG_OUTPUT_SYNCHRONOUS, glDebugMessageControl}
import org.lwjgl.system.MemoryUtil._
import tdrive.viz.util.memory

import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.09.2018
  */
object Window {
  protected val errorCallbackSet = new AtomicBoolean(false)
  protected implicit def convertBooleanToGLFWFlag(bool: Boolean): Int = if (bool) GLFW_TRUE else GLFW_FALSE
}

class Window(name: String, width_param: Int, height_param: Int, mode: Int = -1, refreshRate: Int = GLFW_DONT_CARE,
             visible: Boolean = false, resizeable: Boolean = true, debug: Boolean = false) extends StrictLogging {
  import Window._

  private var width = width_param
  private var height = height_param

  logger.info(s"Creating Window '$name' (${width}x$height@$refreshRate)")

  if(errorCallbackSet.compareAndSet(false, true)) {
    glfwSetErrorCallback(GLFWErrorCallback.create((error, description) => logger.error(s"[GLFW] $error: $description")))

    if (!glfwInit()) throw new IllegalArgumentException("Unable to initialize GLFW!")
  }

  //TODO: Port Monitor enumeration stuff

  glfwDefaultWindowHints()
  glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
  glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
  glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
  glfwWindowHint(GLFW_VISIBLE, visible)
  glfwWindowHint(GLFW_RESIZABLE, resizeable)
  glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, debug)
  glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_TRUE)
  glfwWindowHint(GLFW_REFRESH_RATE, refreshRate)
  glfwWindowHint(GLFW_DECORATED, if (mode > -1) GLFW_TRUE else GLFW_FALSE)

  private val window = glfwCreateWindow(width, height, name, if (mode > -1) NULL else glfwGetPrimaryMonitor, NULL)
  if (window == NULL) {
    glfwTerminate()
    logger.error("Unable to create window!")
    throw new RuntimeException("glfwCreateWindow failed!")
  }

  // Center Window on Primary monitor
  memory(stack => {
    val pWidth = stack.mallocInt(1)
    val pHeight = stack.mallocInt(1)

    glfwGetWindowSize(window, pWidth, pHeight)
    val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())

    glfwSetWindowPos(
      window,
      (vidMode.width() - pWidth.get) / 2,
      (vidMode.height() - pHeight.get) / 2
    )
  })

  glfwSetWindowSizeCallback(window, (_, width, height) => {
    if (width != 0 && height != 0 && width != this.width && height != this.height) {
      this.width = width
      this.height = height

      logger.info(s"Resizing window to ${width}x$height")
      // TODO: update camera stuff

      // Might brick offscreen rendering ...
      //glViewport(0, 0, width, height)
    }
  })

  glfwMakeContextCurrent(window)
  private val caps = GL.createCapabilities()

  if (!caps.GL_ARB_shader_draw_parameters) {
    glfwTerminate()
    logger.error("Does not have the GL_ARB_shader_draw_parameters extension!")
    throw new RuntimeException("GL_ARB_shader_draw_parameters missing from context!")
  }

  setVsync(true)

  // Setup Debug context if needed
  private val debugHandler = if (debug) {
    logger.info("OpenGL Context was created with 'debug = true'")

    // Allows backtracking to the offending API Call, place Breakpoint in the GLUtil function
    glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS)
    glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, Array.empty[Int], true)

    Some(GLUtil.setupDebugMessageCallback(System.out))
  } else {
    None
  }

  def getKey(key: Int): Int = glfwGetKey(window, key)

  def setCursorPositionCallback(callback: (Window, Double, Double) => Unit): Unit = glfwSetCursorPosCallback(window, (_, x, y) => callback(this, x, y))

  def hideCursor(): Unit = glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
  def showCursor(): Unit = glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)

  def focus(): Unit = glfwFocusWindow(window)

  def swapBuffers(): Unit = glfwSwapBuffers(window)

  def setVsync(bool: Boolean): Unit = glfwSwapInterval(if(bool) 1 else 0)

  def isClosed: Boolean = glfwWindowShouldClose(window)
  def close(value: Boolean = true): Unit = glfwSetWindowShouldClose(window, value)
  def show(): Unit = glfwShowWindow(window)
  def hide(): Unit = glfwHideWindow(window)

  def destroy(): Unit = {
    debugHandler.foreach(_.free())
    glfwDestroyWindow(window)
  }

}
