package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.database.instances.LocationDatabase
import app.simple.positional.model.Locations
import app.simple.positional.ui.compose.CustomLocationForm
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomLocationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LocationDatabase.getInstance(application).locationDao()

    val form = CustomLocationForm()

    val locations: StateFlow<List<Locations>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    fun save(location: Locations) = persist { dao.insertLocation(location) }

    fun delete(location: Locations) = persist { dao.deleteLocation(location) }

    fun deleteAll() = persist { dao.deleteAll() }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { block() } }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
