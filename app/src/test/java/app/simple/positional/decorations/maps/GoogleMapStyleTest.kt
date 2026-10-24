package app.simple.positional.decorations.maps

import app.simple.positional.R
import com.google.android.gms.maps.GoogleMap
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleMapStyleTest {
    @Test
    fun mapTypeFollowsSatelliteAndLabels() {
        assertEquals(GoogleMap.MAP_TYPE_NORMAL, GoogleMapStyle.type(satellite = false, label = true))
        assertEquals(GoogleMap.MAP_TYPE_NORMAL, GoogleMapStyle.type(satellite = false, label = false))
        assertEquals(GoogleMap.MAP_TYPE_HYBRID, GoogleMapStyle.type(satellite = true, label = true))
        assertEquals(GoogleMap.MAP_TYPE_SATELLITE, GoogleMapStyle.type(satellite = true, label = false))
    }

    @Test
    fun highContrastWinsOverNightMode() {
        assertEquals(R.raw.map_high_contrast_labelled, GoogleMapStyle.raw(label = true, highContrast = true, night = true))
        assertEquals(R.raw.map_high_contrast_non_labelled, GoogleMapStyle.raw(label = false, highContrast = true, night = false))
    }

    @Test
    fun styleFollowsNightMode() {
        assertEquals(R.raw.maps_dark_labelled, GoogleMapStyle.raw(label = true, highContrast = false, night = true))
        assertEquals(R.raw.maps_dark_no_label, GoogleMapStyle.raw(label = false, highContrast = false, night = true))
        assertEquals(R.raw.maps_light_labelled, GoogleMapStyle.raw(label = true, highContrast = false, night = false))
        assertEquals(R.raw.maps_light_no_label, GoogleMapStyle.raw(label = false, highContrast = false, night = false))
    }
}
