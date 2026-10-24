package app.simple.positional.extensions.maps

import app.simple.positional.model.MeasurePoint
import app.simple.positional.model.TrailPoint
import com.google.android.gms.maps.model.LatLng

interface MapsCallbacks {
    fun onMapInitialized() {}

    fun onMapClicked() {}

    fun onMapLongClicked(latLng: LatLng) {}

    fun onLineDeleted(trailPoint: TrailPoint?) {}

    fun onLineDeleted(measurePoint: MeasurePoint?) {}

    fun onLineAdded(measurePoint: MeasurePoint) {}

    fun onCameraDistance(latLng: LatLng?) {}
}
