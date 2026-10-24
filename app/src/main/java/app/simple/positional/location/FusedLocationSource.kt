package app.simple.positional.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import app.simple.positional.ui.compose.SettingsLabels
import app.simple.positional.util.PermissionUtils
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class FusedLocationSource(private val context: Context) : LocationSource {
    override fun events(): Flow<LocationEvent> =
        if (!PermissionUtils.checkPermission(context)) emptyFlow() else merge(
            context.providerChanges().map { LocationEvent.Provider(SettingsLabels.PROVIDER_FUSED) },
            fixes(),
        )

    @SuppressLint("MissingPermission")
    private fun fixes(): Flow<LocationEvent> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        var fresh = false
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                fresh = true
                trySend(LocationEvent.Fix(location))
            }
        }
        client.lastLocation.addOnSuccessListener { last -> if (!fresh && last != null) trySend(last.asLastKnown()) }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        val request: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LocationSource.INTERVAL)
            .setMinUpdateIntervalMillis(LocationSource.INTERVAL)
            .build()
    }
}
