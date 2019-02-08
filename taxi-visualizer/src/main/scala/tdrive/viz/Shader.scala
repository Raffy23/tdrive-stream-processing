package tdrive.viz

import com.typesafe.scalalogging.LazyLogging
import org.joml._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL32._
import org.lwjgl.opengl.GL43._
import org.lwjgl.system.MemoryStack
import util.memory
import util.implicits._


import scala.collection.mutable
import scala.io.Source

/**
  * Created by: 
  *
  * @author Raphael
  * @version 16.08.2018
  */
class Shader protected (val name: String, shaderProgram: Int, source: Shader.SourceProperties) {
  private var program: Int = shaderProgram
  private var uniformNameCache = new mutable.TreeMap[String, Int]()

  def use(): Unit = glUseProgram(program)
  def delete(): Unit = glDeleteProgram(program)

  def setUniform(location: Int, value: Boolean): Unit = glUniform1i(location, if(value) 1 else 0)
  def setUniform(location: Int, value: Int): Unit =  glUniform1i(location, value)
  def setUniform(location: Int, value: Float): Unit =  glUniform1f(location, value)
  def setUniform(location: Int, value: Vector2f): Unit = memory(implicit stack => glUniform2fv(location, value.toFloatBuffer))
  def setUniform(location: Int, value: Vector3f): Unit = memory(implicit stack => glUniform3fv(location, value.toFloatBuffer))
  def setUniform(location: Int, value: Vector4f): Unit = memory(implicit stack => glUniform4fv(location, value.toFloatBuffer))
  def setUniform(location: Int, value: Matrix3f): Unit = memory(implicit stack => glUniformMatrix3fv(location, false, value.toFloatBuffer))
  def setUniform(location: Int, value: Matrix4f): Unit = memory(implicit stack => glUniformMatrix4fv(location, false, value.toFloatBuffer))
  //def setUniform(location: Int, value: Matrix4 ): Unit = memory(implicit stack => glUniformMatrix4fv(location, false, value.toFloatBuffer))

  def setUniform(name: String, value: Boolean): Unit = glUniform1i(lookupLocation(name), if(value) 1 else 0)
  def setUniform(name: String, value: Int): Unit =  glUniform1i(lookupLocation(name), value)
  def setUniform(name: String, value: Float): Unit =  glUniform1f(lookupLocation(name), value)
  def setUniform(name: String, value: Vector2f): Unit = memory(implicit stack => glUniform2fv(lookupLocation(name), value.toFloatBuffer))
  def setUniform(name: String, value: Vector3f): Unit = memory(implicit stack => glUniform3fv(lookupLocation(name), value.toFloatBuffer))
  def setUniform(name: String, value: Vector4f): Unit = memory(implicit stack => glUniform4fv(lookupLocation(name), value.toFloatBuffer))
  def setUniform(name: String, value: Matrix3f): Unit = memory(implicit stack => glUniformMatrix3fv(lookupLocation(name), false, value.toFloatBuffer))
  def setUniform(name: String, value: Matrix4f): Unit = memory(implicit stack => glUniformMatrix4fv(lookupLocation(name), false, value.toFloatBuffer))
  //def setUniform(name: String, value: Matrix4 ): Unit = memory(implicit stack => glUniformMatrix4fv(lookupLocation(name), false, value.toFloatBuffer))

  def getUniformLocation(name: String): Int = glGetUniformLocation(program, name)

  private def lookupLocation(name: String): Int = uniformNameCache.getOrElseUpdate(name, glGetUniformLocation(program, name))
  
  def reload()(implicit stack: MemoryStack): Unit = {
    if(source.file) {
      delete()

      import source._
      val vSource = if (vertex != null) Source.fromFile(vertex).getLines().mkString("\n") else null
      val fSource = if (fragment != null) Source.fromFile(fragment).getLines().mkString("\n") else null
      val gSource = if (geometry != null) Source.fromFile(geometry).getLines().mkString("\n") else null
      val cSource = if (compute != null) Source.fromFile(compute).getLines().mkString("\n") else null

      import Shader._
      val vShader = compileShader(vSource, GL_VERTEX_SHADER)
      val fShader = compileShader(fSource, GL_FRAGMENT_SHADER)
      val gShader = compileShader(gSource, GL_GEOMETRY_SHADER)
      val cShader = compileShader(cSource, GL_COMPUTE_SHADER)

      program = linkProgram(List(vShader, fShader, gShader, cShader))
    }
  }

}

object Shader extends LazyLogging {

  protected case class SourceProperties(file: Boolean, vertex: String, fragment: String, geometry: String, compute: String)

  def fromFile(name: String, vertex: String, fragment: String, geometry: String = null)(implicit stack: MemoryStack): Shader = {
    val vSource = Source.fromFile(vertex).getLines().mkString("\n")
    val fSource = Source.fromFile(fragment).getLines().mkString("\n")
    val gSource = if (geometry != null) Source.fromFile(geometry).getLines().mkString("\n") else null

    val vShader = compileShader(vSource, GL_VERTEX_SHADER)
    val fShader = compileShader(fSource, GL_FRAGMENT_SHADER)
    val gShader = compileShader(gSource, GL_GEOMETRY_SHADER)

    val program = linkProgram(List(vShader, fShader, gShader))

    new Shader(name, program, SourceProperties(file = true, vertex, fragment, geometry, null))
  }

  def fromFile(name: String, compute: String)(implicit stack: MemoryStack): Shader = {
    new Shader(
      name,
      linkProgram(List(compileShader(Source.fromFile(compute).getLines().mkString("\n"), GL_COMPUTE_SHADER))),
      SourceProperties(file = true, null, null, null, compute)
    )
  }

  private def compileShader(code: String, `type`: Int)(implicit stack: MemoryStack): Option[Int] = {
    if (code == null) return None
    implicit val shader: Int = glCreateShader(`type`)

    glShaderSource(shader, code)
    glCompileShader(shader)
    logOnShaderError(s"Unable to compile ${shaderTypeToString(`type`)}:")

    Some(shader)
  }

  private def linkProgram(shaders: List[Option[Int]])(implicit stack: MemoryStack): Int = {
    implicit val program: Int = glCreateProgram()

    shaders.foreach(shader => shader.foreach(shader => glAttachShader(program, shader)))
    glLinkProgram(program)

    logOnProgramError("Unable to link Shader:")
    shaders.foreach(shader => shader.foreach(glDeleteShader))

    program
  }

  private def logOnProgramError(message: String)(implicit program: Int, stack: MemoryStack): Unit = {
    val success = stack.mallocInt(1)

    glGetProgramiv(program, GL_LINK_STATUS, success)
    if (success.get == 0)
      logger.error(message + "\n" + glGetProgramInfoLog(program))

  }

  private def logOnShaderError(message: String)(implicit shader: Int, stack: MemoryStack): Unit = {
    val success = stack.mallocInt(1)

    glGetShaderiv(shader, GL_COMPILE_STATUS, success)
    if (success.get == 0)
      logger.error(message + "\n" + glGetShaderInfoLog(shader))

  }

  private def shaderTypeToString(`type`: Int): String = `type` match {
    case GL_VERTEX_SHADER => "Vertex Shader"
    case GL_FRAGMENT_SHADER => "Fragment Shader"
    case GL_COMPUTE_SHADER => "Compute Shader"
  }

}
