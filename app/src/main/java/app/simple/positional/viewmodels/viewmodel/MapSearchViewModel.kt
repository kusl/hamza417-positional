package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.CoordinateInput
import app.simple.positional.ui.compose.CustomLocationForm
import app.simple.positional.ui.compose.MapSearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MapSearchViewModel(application: Application) : AndroidViewModel(application) {
    val form = CustomLocationForm()
    private val query = MutableStateFlow("")

    val suggestions: StateFlow<List<Address>> = query
        .debounce { if (it.isEmpty()) 0L else MapSearchQuery.DELAY }
        .map(MapSearchQuery::normalize)
        .distinctUntilChanged()
        .mapLatest { text -> text?.let { search(it) }.orEmpty().filter { it.hasLatitude() && it.hasLongitude() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val last = MainPreferences.getLastCoordinates()
        form.fill("", CoordinateInput.format(last[0].toDouble()), CoordinateInput.format(last[1].toDouble()))
    }

    fun onAddressChange(text: String) {
        form.address = text
        query.value = text
    }

    fun onCameraIdle(latitude: Double, longitude: Double) {
        form.latitude = CoordinateInput.format(latitude)
        form.longitude = CoordinateInput.format(longitude)
    }

    fun pick(address: Address) {
        query.value = ""
        form.fill(address.getAddressLine(0) ?: form.address, CoordinateInput.format(address.latitude), CoordinateInput.format(address.longitude))
    }

    private suspend fun search(text: String): List<Address> {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(getApplication())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.searchAsync(text)
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocationName(text, MapSearchQuery.MAX_RESULTS) }.getOrNull().orEmpty()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun Geocoder.searchAsync(text: String): List<Address> = suspendCancellableCoroutine { continuation ->
        getFromLocationName(text, MapSearchQuery.MAX_RESULTS, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (continuation.isActive) continuation.resume(addresses)
            }

            override fun onError(errorMessage: String?) {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        })
    }
}
