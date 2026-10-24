package app.simple.positional.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object Geocoding {
    @Suppress("DEPRECATION")
    suspend fun addressLine(context: Context, latitude: Double, longitude: Double): String? = context.geocoder()?.let { geocoder ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            await { geocoder.getFromLocation(latitude, longitude, 1, it) }
        } else {
            blocking { geocoder.getFromLocation(latitude, longitude, 1) }
        }
    }?.firstOrNull()?.getAddressLine(0)

    @Suppress("DEPRECATION")
    suspend fun search(context: Context, text: String, limit: Int): List<Address> = context.geocoder()?.let { geocoder ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            await { geocoder.getFromLocationName(text, limit, it) }
        } else {
            blocking { geocoder.getFromLocationName(text, limit) }
        }
    }.orEmpty()

    private fun Context.geocoder(): Geocoder? =
        if (Geocoder.isPresent()) Geocoder(applicationContext, LocaleHelper.getAppLocale()) else null

    private suspend fun blocking(lookup: () -> List<Address>?): List<Address> =
        withContext(Dispatchers.IO) { runCatching(lookup).getOrNull().orEmpty() }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun await(lookup: (Geocoder.GeocodeListener) -> Unit): List<Address> = suspendCancellableCoroutine { continuation ->
        val listener = object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (continuation.isActive) continuation.resume(addresses)
            }

            override fun onError(errorMessage: String?) {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }
        runCatching { lookup(listener) }.onFailure { if (continuation.isActive) continuation.resume(emptyList()) }
    }
}
