package app.simple.positional.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import app.simple.positional.util.PermissionUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class AndroidLocationSource(private val context: Context) : LocationSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun events(): Flow<LocationEvent> {
        val manager = context.getSystemService<LocationManager>() ?: return emptyFlow()
        if (!PermissionUtils.checkPermission(context)) return emptyFlow()
        val fine = PermissionUtils.hasFineLocation(context)
        return context.providerChanges()
            .onStart { emit(Unit) }
            .map { bestProvider(fine, manager::isProviderEnabled) }
            .distinctUntilChanged()
            .flatMapLatest { provider -> updates(manager, provider).onStart { emit(LocationEvent.Provider(provider)) } }
    }

    @SuppressLint("MissingPermission")
    private fun updates(manager: LocationManager, provider: String?): Flow<LocationEvent> = if (provider == null) emptyFlow() else callbackFlow {
        manager.getLastKnownLocation(provider)?.let { trySend(it.asLastKnown()) }
        val listener = LocationListenerCompat { trySend(LocationEvent.Fix(it)) }
        LocationManagerCompat.requestLocationUpdates(manager, provider, request, listener, Looper.getMainLooper())
        awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
    }

    companion object {
        private val request = LocationRequestCompat.Builder(LocationSource.INTERVAL)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(LocationSource.INTERVAL)
            .build()

        fun bestProvider(fine: Boolean, enabled: (String) -> Boolean): String? = when {
            fine && enabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            enabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            fine -> LocationManager.PASSIVE_PROVIDER
            else -> null
        }
    }
}
