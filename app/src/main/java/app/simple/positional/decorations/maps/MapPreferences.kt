package app.simple.positional.decorations.maps

import app.simple.positional.singleton.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class MapPreferences {
    private val flows = mutableListOf<Flow<Any?>>()

    fun <T> add(initial: T, values: Flow<T>, apply: (T) -> Unit) {
        var applied = initial
        flows += values.filter { it != applied }.onEach {
            applied = it
            apply(it)
        }
    }

    fun <T> watch(vararg keys: String, read: () -> T, apply: (T) -> Unit) = add(read(), SharedPreferences.watch(*keys, read = read), apply)

    fun launchIn(scope: CoroutineScope): Job = flows.merge().launchIn(scope)
}
