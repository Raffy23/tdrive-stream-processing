package tdrive.viz.util

import java.nio.FloatBuffer

import org.joml._
import org.lwjgl.system.MemoryStack

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.09.2018
  */
package object implicits {

  implicit class RichVec2f(val vector: Vector2f) extends AnyVal {
    @inline final def toFloatBuffer(implicit stack: MemoryStack): FloatBuffer = vector.get(stack.mallocFloat(2))
  }

  implicit class RichVec3f(val vector: Vector3f) extends AnyVal {
    @inline final def +(v: Vector3f): Vector3f = vector.add(v, new Vector3f())
    @inline final def -(v: Vector3f): Vector3f = vector.sub(v, new Vector3f())
    @inline final def *(v: Vector3f): Vector3f = vector.mul(v, new Vector3f())
    @inline final def x(v: Vector3f): Vector3f = vector.cross(v, new Vector3f())

    @inline final def *(v: Float): Vector3f = vector.mul(v, new Vector3f())

    @inline final def += (v: Vector3f): Vector3f = vector.add(v)
    @inline final def -= (v: Vector3f): Vector3f = vector.sub(v)
    @inline final def /= (v: Vector3f): Vector3f = vector.div(v)
    @inline final def *= (v: Vector3f): Vector3f = vector.mul(v)
    @inline final def `x=` (v: Vector3f): Vector3f = vector.cross(v)

    @inline final def /= (v: Float): Vector3f = vector.div(v)
    @inline final def *= (v: Float): Vector3f = vector.mul(v)

    @inline final def toFloatBuffer(implicit stack: MemoryStack): FloatBuffer = vector.get(stack.mallocFloat(3))
  }

  implicit class RichVec4f(val vector: Vector4f) extends AnyVal {
    @inline final def toFloatBuffer(implicit stack: MemoryStack): FloatBuffer = vector.get(stack.mallocFloat(4))
  }

  implicit class RichFloat(val num: Float) extends AnyVal {
    @inline final def *(v: Vector2f): Vector2f = v.mul(v, new Vector2f())
    @inline final def *(v: Vector3f): Vector3f = v.mul(v, new Vector3f())
    @inline final def *(v: Vector4f): Vector4f = v.mul(v, new Vector4f())
  }


  implicit class RichMat4f(val mat: Matrix4f) extends AnyVal {
    @inline final def toFloatBuffer(implicit stack: MemoryStack): FloatBuffer = mat.get(stack.mallocFloat(16))
  }
  implicit class RichMat3f(val mat: Matrix3f) extends AnyVal {
    @inline final def toFloatBuffer(implicit stack: MemoryStack): FloatBuffer = mat.get(stack.mallocFloat(9))
  }


  //implicit class RichMatrix4(val mat: Matrix4) extends AnyVal {
  //  @inline final def toFloatBuffer(implicit stack: MemoryStack): FloatBuffer = {
  //    val out = stack.mallocFloat(16).put(mat.`val`)
  //    out.flip()
  //
  //    out
  //  }
  //}


}
