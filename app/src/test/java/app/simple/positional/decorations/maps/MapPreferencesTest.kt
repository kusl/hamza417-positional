package app.simple.positional.decorations.maps

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MapPreferencesTest {
    @Test
    fun appliesOnlyChangedValues() = runBlocking {
        val applied = mutableListOf<Int>()
        val preferences = MapPreferences()
        preferences.add(1, flowOf(1, 2, 2, 3)) { applied += it }
        preferences.launchIn(this).join()
        assertEquals(listOf(2, 3), applied)
    }

    @Test
    fun resumeAppliesChangeMadeWhilePausedOnce() = runBlocking {
        var stored = 1
        val applied = mutableListOf<Int>()
        val preferences = MapPreferences()
        preferences.add(stored, flow { emit(stored) }) { applied += it }
        preferences.launchIn(this).join()
        stored = 5
        preferences.launchIn(this).join()
        preferences.launchIn(this).join()
        assertEquals(listOf(5), applied)
    }

    @Test
    fun bindingsRunIndependently() = runBlocking {
        val applied = mutableListOf<String>()
        val preferences = MapPreferences()
        preferences.add(false, flowOf(true)) { applied += "compass" }
        preferences.add("a", flowOf("a")) { applied += "look" }
        preferences.launchIn(this).join()
        assertEquals(listOf("compass"), applied)
    }

    @Test
    fun emptyBindingsFinish() = runBlocking {
        MapPreferences().launchIn(this).join()
    }
}
