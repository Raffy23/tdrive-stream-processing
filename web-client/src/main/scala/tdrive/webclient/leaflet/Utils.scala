package tdrive.webclient.leaflet

import scala.language.implicitConversions
import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.02.2019
  */
object Utils {

  def createMapBoxLayer(accessToken: String): Layer =
    L.tileLayer(
      "https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}",
      LayerOptions(
        attribution =
          """Map data &copy; <a href=\"https://www.openstreetmap.org/\">OpenStreetMap</a> contributors,
            |<a href=\"https://creativecommons.org/licenses/by-sa/2.0/\">CC-BY-SA</a>, Imagery ©
            |<a href=\"https://www.mapbox.com/\">Mapbox</a>
            |""".stripMargin,
        maxZoom = 18,
        id = "mapbox.streets",
        accessToken
      )
    )

}
