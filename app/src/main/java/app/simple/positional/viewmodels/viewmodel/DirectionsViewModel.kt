package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.database.instances.DirectionDatabase
import app.simple.positional.model.DirectionModel
import app.simple.positional.ui.compose.CoordinateInput
import app.simple.positional.ui.compose.CustomLocationForm
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectionsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DirectionDatabase.getInstance(application).directionDao()

    val form = CustomLocationForm()

    var editing by mutableStateOf<Long?>(null)
        private set

    val directions: StateFlow<List<DirectionModel>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    fun open(name: String?, latitude: Double, longitude: Double, dateAdded: Long = System.currentTimeMillis()) {
        form.fill(name.orEmpty(), CoordinateInput.format(latitude), CoordinateInput.format(longitude))
        editing = dateAdded
    }

    fun close() {
        editing = null
    }

    fun save(latitude: Double, longitude: Double) {
        val dateAdded = editing ?: return
        val direction = DirectionModel(latitude, longitude, form.address.trim(), dateAdded)
        persist { dao.insertDirection(direction) }
        close()
    }

    fun delete(direction: DirectionModel) = persist { dao.deleteDirection(direction) }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { block() } }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
