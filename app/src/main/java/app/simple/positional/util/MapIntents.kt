package app.simple.positional.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import app.simple.positional.R

object MapIntents {
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    fun navigate(context: Context, latitude: Double, longitude: Double) {
        val navigation = Intent(Intent.ACTION_VIEW, "google.navigation:q=$latitude,$longitude&mode=d".toUri()).setPackage(GOOGLE_MAPS)
        runCatching { context.startActivity(navigation) }
            .recoverCatching { context.startActivity(geo(latitude, longitude)) }
            .onFailure { Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show() }
    }

    fun open(context: Context, latitude: Double, longitude: Double) {
        runCatching { context.startActivity(geo(latitude, longitude)) }
            .onFailure { Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show() }
    }

    private fun geo(latitude: Double, longitude: Double) = Intent(Intent.ACTION_VIEW, "geo:$latitude,$longitude?q=$latitude,$longitude".toUri())
}
