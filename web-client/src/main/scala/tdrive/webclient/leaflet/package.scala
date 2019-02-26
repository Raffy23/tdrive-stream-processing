package tdrive.webclient

import org.scalajs.dom.raw.HTMLElement

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
    def setView(center: LatLng, zoom: Int): Map = js.native

    def getBounds(): LatLngBounds = js.native
  }

  @js.native
  trait Layer extends js.Object { self =>
    def addTo(map: Map): self.type = js.native
    def remove(): self.type = js.native
    def removeFrom(map: Map): self.type = js.native
    def getPane(name: String): HTMLElement = js.native
    def getAttribution(): String = js.native
  }

  @js.native
  trait Marker extends Layer {
    def toGeoJSON(): js.Object = js.native
    def getLatLng(): LatLng = js.native
    def setLatLng(latLng: LatLng): Unit = js.native
    def setOpacity(opacity: Double): Unit = js.native
  }
  object Marker {
    def apply(lat: Double, lng: Double): Marker = L.marker(L.latLng(lat, lng))
  }

  @js.native
  trait Circle extends Layer {}

  @js.native
  trait LatLng extends js.Object {
    def warp(): LatLng = js.native
  }

  @js.native
  trait LatLngBounds extends js.Object {
    def getCenter: LatLng = js.native

    def contains(bounds: LatLngBounds): Boolean = js.native
    def contains(bounds: LatLng): Boolean = js.native
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
