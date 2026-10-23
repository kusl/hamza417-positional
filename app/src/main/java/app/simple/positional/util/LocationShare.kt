package app.simple.positional.util

import android.content.Context
import android.content.Intent
import app.simple.positional.R
import app.simple.positional.math.MathExtensions.round

object LocationShare {
    fun text(label: String, latitude: Double, longitude: Double, address: String): String {
        val lat = round(latitude, 6)
        val lon = round(longitude, 6)
        return "$label: $lat, $lon\n\n$address\n\nhttps://www.google.com/maps/search/?api=1&query=$lat,$lon\n\ngeo:$lat,$lon?q=$lat,$lon"
    }

    fun share(context: Context, latitude: Double, longitude: Double, address: String) {
        val text = text(context.getString(R.string.gps_coordinates), latitude, longitude, address)
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }
}
