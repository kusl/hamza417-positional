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

object ReverseGeocoder {
    suspend fun addressLine(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context.applicationContext, LocaleHelper.getAppLocale())
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.lookup(latitude, longitude)
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(latitude, longitude, 1) }.getOrNull().orEmpty()
            }
        }
        return addresses.firstOrNull()?.getAddressLine(0)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun Geocoder.lookup(latitude: Double, longitude: Double): List<Address> = suspendCancellableCoroutine { continuation ->
        runCatching {
            getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            })
        }.onFailure { if (continuation.isActive) continuation.resume(emptyList()) }
    }
}
