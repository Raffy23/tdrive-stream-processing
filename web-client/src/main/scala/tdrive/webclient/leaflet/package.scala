package tdrive.webclient

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.02.2019
  */
package object leaflet {

  type Leaflet = L.type

  @js.native
  trait Map extends js.Object {
    def setView(center: js.Array[Double], zoom: Int): Map = js.native
  }

  @js.native
  trait Layer extends js.Object {
    def addTo(map: Map): Unit = js.native
  }

  @js.native
  trait Marker extends js.Object {
    def addTo(map: Map): Unit = js.native
  }

  @js.native
  trait Circle extends js.Object {
    def addTo(map: Map): Unit = js.native
  }


  @js.native
  trait LayerOptions extends js.Object {
    val attribution: String
    val maxZoom: Int
    val id: String
    val accessToken: String
  }

  object LayerOptions {
    def apply(attribution: String, maxZoom: Int, id: String, accessToken: String): LayerOptions =
      js.Dynamic.literal(attribution = attribution, maxZoom = maxZoom, id = id,
        accessToken = accessToken)
      .asInstanceOf[LayerOptions]
  }

  @js.native
  trait CircleOptions extends js.Object {
    val color: String
    val fillColor: String
    val fillOpacity: Double
    val radius: Int
  }

  object CircleOptions {
    def apply(color: String, fillColor: String, fillOpacity: Double, radius: Int): CircleOptions =
      js.Dynamic.literal(color = color, fillColor = fillColor, fillOpacity = fillOpacity,
        radius = radius)
      .asInstanceOf[CircleOptions]
  }

}
