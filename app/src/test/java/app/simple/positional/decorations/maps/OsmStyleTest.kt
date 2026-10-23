package app.simple.positional.decorations.maps

import app.simple.positional.math.GeoDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmStyleTest {
    @Test
    fun styleUsesOsmTilesAndOverlayLayers() {
        val json = OsmStyle.json(dark = false)
        assertTrue(json.contains("https://tile.openstreetmap.org/{z}/{x}/{y}.png"))
        assertTrue(json.contains("OpenStreetMap contributors"))
        listOf(OsmStyle.PIN, OsmStyle.ACCURACY, OsmStyle.LINE, OsmStyle.GUIDE, OsmStyle.MARKERS).forEach {
            assertTrue(json.contains("\"id\":\"$it\""))
            assertTrue(json.contains("\"$it\":{\"type\":\"geojson\""))
        }
        assertFalse(json.contains("raster-brightness-min"))
    }

    @Test
    fun darkStyleInvertsTiles() {
        assertTrue(OsmStyle.json(dark = true).contains("\"raster-brightness-min\":1"))
    }

    @Test
    fun geometryUsesLongitudeFirst() {
        assertEquals("""{"type":"Point","coordinates":[2.5,48.5]}""", OsmStyle.point(48.5, 2.5))
        assertEquals("""{"type":"LineString","coordinates":[[2.0,1.0],[4.0,3.0]]}""", OsmStyle.line(listOf(1.0 to 2.0, 3.0 to 4.0)))
    }

    @Test
    fun shortLinesAreEmpty() {
        assertEquals(OsmStyle.EMPTY_FEATURES, OsmStyle.line(emptyList()))
        assertEquals(OsmStyle.EMPTY_FEATURES, OsmStyle.line(listOf(1.0 to 2.0)))
    }

    @Test
    fun markersCarryTheirIcon() {
        assertEquals(
            """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2.0,1.0]},"properties":{"icon":"trail-3"}}]}""",
            OsmStyle.markers(listOf(Triple(1.0, 2.0, "trail-3"))),
        )
        assertEquals(OsmStyle.EMPTY_FEATURES, OsmStyle.markers(emptyList()))
    }

    @Test
    fun circleIsClosedAndAtRadius() {
        val points = OsmStyle.circlePoints(37.0, -76.5, 250.0)
        assertEquals(points.first().first, points.last().first, 1e-9)
        assertEquals(points.first().second, points.last().second, 1e-9)
        points.forEach { assertEquals(250.0, GeoDistance.meters(37.0, -76.5, it.first, it.second), 1.0) }
    }

    @Test
    fun polygonWrapsTheRing() {
        assertTrue(OsmStyle.circle(0.0, 0.0, 10.0).startsWith("""{"type":"Polygon","coordinates":[[["""))
    }
}
