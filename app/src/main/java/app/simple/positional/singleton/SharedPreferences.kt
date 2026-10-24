package app.simple.positional.singleton

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object SharedPreferences {
    const val preferences = "Preferences"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
        }
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences ?: throw NullPointerException()
    }

    fun getSharedPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
    }

    fun changes(vararg keys: String): Flow<Unit> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key in keys) trySend(Unit) }
        val preferences = getSharedPreferences()
        preferences.registerOnSharedPreferenceChangeListener(listener)
        send(Unit)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun <T> watch(vararg keys: String, read: () -> T): Flow<T> = changes(*keys).map { read() }.distinctUntilChanged()
}
