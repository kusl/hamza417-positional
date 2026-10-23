package app.simple.positional.services

import android.location.Location
import java.util.concurrent.CopyOnWriteArraySet

object LocationBus {
    interface Listener {
        fun onLocation(location: Location)
        fun onProvider(provider: String?)
    }

    private val listeners = CopyOnWriteArraySet<Listener>()

    fun register(listener: Listener) {
        listeners += listener
    }

    fun unregister(listener: Listener) {
        listeners -= listener
    }

    fun publishLocation(location: Location) = listeners.forEach { it.onLocation(location) }

    fun publishProvider(provider: String?) = listeners.forEach { it.onProvider(provider) }
}
