package tdrive.viz.util

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.09.2018
  */
object SystemUtils {
  def getPID: Long = java.lang.management.ManagementFactory.getRuntimeMXBean.getName.split("@")(0).toLong
}
