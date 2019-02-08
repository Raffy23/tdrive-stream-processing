package tdrive.viz.gl

import org.lwjgl.opengl.GL11.{GL_TEXTURE_2D, glBindTexture, glDeleteTextures}
import org.lwjgl.opengl.GL13.glActiveTexture

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.09.2018
  */
@inline class GLTexture(val texture_id: Int) extends AnyVal {

  @inline final def bind(texture_unit: Int, `type`: Int = GL_TEXTURE_2D): Unit = {
    glActiveTexture(texture_unit)
    glBindTexture(`type`, texture_id)
  }

  @inline final def destroy(): Unit = glDeleteTextures(texture_id)

}
