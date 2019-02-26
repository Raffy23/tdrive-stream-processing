package tdrive.webclient.leaflet

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.02.2019
  */
@JSGlobal
@js.native
object L extends js.Object {

  def map(id: String): Map = js.native

  def tileLayer(uri: String, options: LayerOptions): Layer = js.native

  def marker(coordinates: js.Array[Double]): Marker = js.native
  def marker(coordinates: LatLng): Marker = js.native

  def circle(coordinates: js.Array[Double], options: CircleOptions): Circle = js.native
  def circle(coordinates: LatLng, options: CircleOptions): Circle = js.native

  def latLng(lat: Double, long: Double): LatLng = js.native

}
