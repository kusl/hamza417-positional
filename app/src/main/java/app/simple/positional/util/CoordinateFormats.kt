package app.simple.positional.util

import android.content.Context
import app.simple.positional.R
import app.simple.positional.ui.compose.InfoRow
import gov.nasa.worldwind.geom.Angle
import gov.nasa.worldwind.geom.coords.MGRSCoord

object CoordinateFormats {
    fun sections(context: Context, latitude: Double, longitude: Double): List<Pair<String, List<InfoRow>>> {
        val lat = context.getString(R.string.gps_latitude)
        val lon = context.getString(R.string.gps_longitude)
        val utm = UTMConverter.getUTM(latitude, longitude)
        val mgrs = runCatching { MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(latitude), Angle.fromDegreesLongitude(longitude)).toString() }
            .getOrDefault(context.getString(R.string.not_available))
        return listOf(
            "DD°MM'SS.SSS" to listOf(InfoRow(lat, DMSConverter.latitudeAsDMS(latitude, context)), InfoRow(lon, DMSConverter.longitudeAsDMS(longitude, context))),
            "DD°MM.MMM'" to listOf(InfoRow(lat, DMSConverter.latitudeAsDM(latitude, context)), InfoRow(lon, DMSConverter.longitudeAsDM(longitude, context))),
            "DD.DDD°" to listOf(InfoRow(lat, DMSConverter.latitudeAsDD(latitude)), InfoRow(lon, DMSConverter.longitudeAsDD(longitude))),
            "MGRS" to listOf(InfoRow(mgrs)),
            "UTM" to listOf(
                InfoRow(context.getString(R.string.utm_zone), utm.zone),
                InfoRow(context.getString(R.string.utm_easting), utm.easting),
                InfoRow(context.getString(R.string.utm_northing), utm.northing),
                InfoRow(context.getString(R.string.utm_meridian), utm.centralMeridian),
            ),
        )
    }

    fun text(sections: List<Pair<String, List<InfoRow>>>): String = sections.joinToString("\n\n") { (title, rows) ->
        (listOf(title) + rows.map { "${it.label} ${it.value}".trim() }).joinToString("\n")
    }
}
