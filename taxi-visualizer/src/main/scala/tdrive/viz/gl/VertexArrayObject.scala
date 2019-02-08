package tdrive.viz.gl

import org.lwjgl.opengl.GL20.{glEnableVertexAttribArray, glVertexAttribPointer}
import org.lwjgl.opengl.GL30.{glBindVertexArray, glDeleteVertexArrays, glGenVertexArrays}
import org.lwjgl.opengl.GL33.glVertexAttribDivisor

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2018
  */
object VertexArrayObject {
  @inline final def create(): VertexArrayObject = new VertexArrayObject(glGenVertexArrays)
}

class VertexArrayObject(val vao: Int) extends AnyVal {

  @inline final def bind(f: VertexArrayObject => Unit): Unit = {
    glBindVertexArray(vao)
    f(this)
    glBindVertexArray(0)
  }

  @inline final def attribute(index: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, pointer: Long): Unit = {
    glVertexAttribPointer(index, size, `type`, normalized, stride, pointer)
    glEnableVertexAttribArray(index)
  }

  @inline final def attributeDivisor(index: Int, divisor: Int): Unit = {
    glVertexAttribDivisor(index, divisor)
  }

  @inline final def destroy(): Unit = {
    glDeleteVertexArrays(vao)
  }

  override def toString: String = s"VAO($vao)"

}
