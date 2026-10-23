package app.simple.positional.decorations.maps

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object OsmStyle {
    const val PIN = "pin"
    const val ACCURACY = "accuracy"
    const val LINE = "line"
    const val GUIDE = "guide"
    const val MARKERS = "markers"
    const val ICON = "icon"
    const val EMPTY_FEATURES = """{"type":"FeatureCollection","features":[]}"""
    private const val TILES = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    private const val ATTRIBUTION = "© OpenStreetMap contributors"
    private const val EARTH_RADIUS = 6_371_008.8
    private const val CIRCLE_STEPS = 64
    private const val EMPTY = """{"type":"geojson","data":$EMPTY_FEATURES}"""
    private const val DARK = ""","paint":{"raster-brightness-min":1,"raster-brightness-max":0,"raster-saturation":-0.5}"""
    private const val OVERLAP = """"icon-allow-overlap":true,"icon-ignore-placement":true"""

    fun json(dark: Boolean): String {
        val raster = if (dark) DARK else ""
        val sources = listOf(ACCURACY, LINE, GUIDE, MARKERS, PIN).joinToString(",") { "\"$it\":$EMPTY" }
        return """{"version":8,"sources":{"osm":{"type":"raster","tiles":["$TILES"],"tileSize":256,"maxzoom":19,"attribution":"$ATTRIBUTION"},$sources},""" +
            """"layers":[{"id":"osm","type":"raster","source":"osm"$raster},""" +
            """{"id":"$ACCURACY","type":"fill","source":"$ACCURACY"},""" +
            """{"id":"$LINE","type":"line","source":"$LINE","layout":{"line-join":"round","line-cap":"round"},"paint":{"line-width":4}},""" +
            """{"id":"$GUIDE","type":"line","source":"$GUIDE","paint":{"line-width":2,"line-dasharray":[2,2]}},""" +
            """{"id":"$MARKERS","type":"symbol","source":"$MARKERS","layout":{"icon-image":["get","$ICON"],"icon-anchor":"bottom",$OVERLAP}},""" +
            """{"id":"$PIN","type":"symbol","source":"$PIN","layout":{"icon-image":"$PIN","icon-rotation-alignment":"map",$OVERLAP}}]}"""
    }

    fun point(latitude: Double, longitude: Double) = """{"type":"Point","coordinates":[$longitude,$latitude]}"""

    fun line(points: List<Pair<Double, Double>>) =
        if (points.size < 2) EMPTY_FEATURES else """{"type":"LineString","coordinates":[${coordinates(points)}]}"""

    fun markers(markers: List<Triple<Double, Double, String>>): String {
        val features = markers.joinToString(",") { (latitude, longitude, icon) -> marker(latitude, longitude, icon) }
        return """{"type":"FeatureCollection","features":[$features]}"""
    }

    private fun marker(latitude: Double, longitude: Double, icon: String) =
        """{"type":"Feature","geometry":${point(latitude, longitude)},"properties":{"$ICON":"$icon"}}"""

    fun circle(latitude: Double, longitude: Double, radius: Double) =
        """{"type":"Polygon","coordinates":[[${coordinates(circlePoints(latitude, longitude, radius))}]]}"""

    fun circlePoints(latitude: Double, longitude: Double, radius: Double): List<Pair<Double, Double>> {
        val lat = Math.toRadians(latitude)
        val lng = Math.toRadians(longitude)
        val distance = radius / EARTH_RADIUS
        return (0..CIRCLE_STEPS).map { step ->
            val bearing = 2 * Math.PI * step / CIRCLE_STEPS
            val pointLat = asin(sin(lat) * cos(distance) + cos(lat) * sin(distance) * cos(bearing))
            val pointLng = lng + atan2(sin(bearing) * sin(distance) * cos(lat), cos(distance) - sin(lat) * sin(pointLat))
            Math.toDegrees(pointLat) to Math.toDegrees(pointLng)
        }
    }

    private fun coordinates(points: List<Pair<Double, Double>>) = points.joinToString(",") { "[${it.second},${it.first}]" }
}
