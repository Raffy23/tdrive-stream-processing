package tdrive.webclient

import io.circe.generic.auto._
import io.circe.parser.decode
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement
import tdrive.shared.dto._
import tdrive.webclient.leaflet.{L, Marker, Utils}

import scala.collection.mutable

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

  case class TaxiState(marker: Marker, var wasRendered: Boolean = false)

  def main(args: Array[String]): Unit = {
    import dom._

    document.addEventListener("DOMContentLoaded", (_: Any) => {
      val map = prepareLeafletMap()
      val ws  = new WebSocket(s"ws://127.0.0.1:8080") //TODO
      ws.onclose = (_: CloseEvent) => window.alert("Websocket closed!")

      val taxis = new mutable.TreeMap[Int, TaxiState]()
      ws.onmessage = (message: MessageEvent) => {
        decode[TaxiMessage](message.data.toString).fold(
          error => {
            ws.close()
            window.alert(error.toString)
          }, {
            case TaxiLocation(id, long, lat) =>
              val convertedCoords = L.latLng(lat, long)
              val state = taxis.getOrElseUpdate(id, TaxiState(Marker(lat, long)))

              if (map.getBounds().contains(convertedCoords)) {
                state.marker.setLatLng(convertedCoords)
                if (!state.wasRendered)
                  state.marker.addTo(map)
              } else {
                state.marker.removeFrom(map)
                state.wasRendered = false
              }

            case TaxiLeftArea(id) => //println(s"$id left area")
            case TaxiSpeed(id, speed) => //println(s"$id drives at $speed km/h")
            case TaxiSpeeding(id, speed) => //println(s"$id is speeding with $speed km/h")
          }
        )
      }

      //Marker(BeijingLat, BeijingLong).addTo(map).setLatLng(L.latLng(BeijingLat, BeijingLong))

    }, useCapture = false)

  }

  def prepareLeafletMap(): leaflet.Map = {
    // Set Element to absolute height
    val leafletMap = dom.document.getElementById("leaflet-map").asInstanceOf[HTMLDivElement]
    leafletMap.setAttribute("style", "height:"+ (dom.window.innerHeight-8*2) + "px")

    // Set view to Beijing and create mapbox layer
    val mymap = L.map("leaflet-map").setView(L.latLng(BeijingLat, BeijingLong), 13)
    Utils.createMapBoxLayer(mapBoxAccessToken).addTo(mymap)

    mymap
  }

}
