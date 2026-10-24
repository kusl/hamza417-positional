package app.simple.positional.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import app.simple.positional.ui.compose.SettingsLabels
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface LocationEvent {
    data class Fix(val location: Location) : LocationEvent
    data class Provider(val name: String?) : LocationEvent
}

fun interface LocationSource {
    fun events(): Flow<LocationEvent>

    companion object {
        const val INTERVAL = 1_000L
        const val LAST_KNOWN = "Last Location"

        fun of(context: Context, provider: String): LocationSource =
            if (provider == SettingsLabels.PROVIDER_FUSED) FusedLocationSource(context) else AndroidLocationSource(context)
    }
}

internal fun Location.asLastKnown(): LocationEvent = LocationEvent.Fix(apply { provider = LocationSource.LAST_KNOWN })

internal fun Context.providerChanges(): Flow<Unit> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            trySend(Unit)
        }
    }
    val context = this@providerChanges
    ContextCompat.registerReceiver(context, receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
    awaitClose { context.unregisterReceiver(receiver) }
}
