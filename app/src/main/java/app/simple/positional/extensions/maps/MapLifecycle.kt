package app.simple.positional.extensions.maps

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView

class MapLifecycle(private val mapView: MapView) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = mapView.onStart()
    override fun onResume(owner: LifecycleOwner) = mapView.onResume()
    override fun onPause(owner: LifecycleOwner) = mapView.onPause()
    override fun onStop(owner: LifecycleOwner) = mapView.onStop()
    override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
}
