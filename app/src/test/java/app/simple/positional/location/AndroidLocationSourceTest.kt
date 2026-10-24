package app.simple.positional.location

import android.location.LocationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidLocationSourceTest {
    private val all = setOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)

    @Test
    fun prefersGpsWithFinePermission() {
        assertEquals(LocationManager.GPS_PROVIDER, AndroidLocationSource.bestProvider(true) { it in all })
    }

    @Test
    fun fallsBackToNetworkWhenGpsIsOff() {
        assertEquals(LocationManager.NETWORK_PROVIDER, AndroidLocationSource.bestProvider(true) { it != LocationManager.GPS_PROVIDER })
    }

    @Test
    fun usesPassiveWhenLocationIsOff() {
        assertEquals(LocationManager.PASSIVE_PROVIDER, AndroidLocationSource.bestProvider(true) { it == LocationManager.PASSIVE_PROVIDER })
    }

    @Test
    fun approximatePermissionSkipsGps() {
        assertEquals(LocationManager.NETWORK_PROVIDER, AndroidLocationSource.bestProvider(false) { it in all })
    }

    @Test
    fun approximatePermissionWithoutNetworkHasNoProvider() {
        assertNull(AndroidLocationSource.bestProvider(false) { it == LocationManager.GPS_PROVIDER })
    }
}
