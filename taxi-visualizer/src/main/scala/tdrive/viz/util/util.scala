package tdrive.viz

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush

import scala.util.{Failure, Success, Try}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 16.08.2018
  */
package object util {

  //
  // Some syntactic sugar stuff for OpenGL
  //
  object OGLConstants {
    val GL_FALSE = false
    val GL_TRUE = true
  }

  sealed trait CTypes
  object `float` extends CTypes
  object `int` extends CTypes
  object `double` extends CTypes
  object `long` extends CTypes

  def sizeof(t: CTypes): Int = t match {
    case `float` => java.lang.Float.BYTES
    case `int` => java.lang.Integer.BYTES
    case `double` => java.lang.Double.BYTES
    case `long` => java.lang.Long.BYTES
  }

  def sizeof(t: Array[Any]): Int = t.length

  //
  // Function to guard memory stack to prevent out of stack memory errors
  //
  def memory[R](func: MemoryStack => R): R = {
    try {
      val stack = stackPush
      try {
        func(stack)
      } finally if (stack != null) stack.close()
    } catch {
      case ex: Exception => throw new RuntimeException(ex)
    }
  }


  // This seems not to work with Memory Stack ?
  def using[T <: AutoCloseable, R](resource: => T)(func: T => R): Try[R] = {
    Try(func(resource)).transform(
      result => {
        resource.close()
        Success(result)
      },
      failure => {
        Try(resource.close()).transform(
          _ => Failure(failure),
          closeFail => {
            failure.addSuppressed(closeFail)
            Failure(failure)
          })
      }
    )
  }

}
