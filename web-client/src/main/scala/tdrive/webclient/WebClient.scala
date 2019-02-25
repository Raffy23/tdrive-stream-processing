package tdrive.webclient

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement
import tdrive.webclient.leaflet.{CircleOptions, L, Utils}

import Utils.convertTupleToArray

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.02.2019
  */
object WebClient {

  private val mapBoxAccessToken = ""

  private val BeijingLat  = 39.904211
  private val BeijingLong = 116.407395

  def main(args: Array[String]): Unit = {

    dom.document.addEventListener("DOMContentLoaded", (_: Any) => {
      val leafletMap = dom.document.getElementById("leaflet-map").asInstanceOf[HTMLDivElement]
      leafletMap.setAttribute("style", "height:"+ (dom.window.innerHeight-8*2) + "px")


      println(L.latLng(BeijingLat, BeijingLong))

      val mymap = L.map("leaflet-map").setView((BeijingLat, BeijingLong), 13)
      Utils.createMapBoxLayer(mapBoxAccessToken).addTo(mymap)

      L.marker((BeijingLat, BeijingLong)).addTo(mymap)
      L.circle((BeijingLat, BeijingLong), CircleOptions(
        color = "red",
        fillColor = "#f03",
        fillOpacity = 0.2,
        radius = 10000 //m
      )).addTo(mymap)
    }, useCapture = false)

  }

}
