package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.database.instances.TrailPointDatabase
import app.simple.positional.model.TrailPoint
import app.simple.positional.preferences.TrailPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class TrailDataViewModel(application: Application) : AndroidViewModel(application) {
    private val name = MutableStateFlow(TrailPreferences.getCurrentTrail())
    val trail: StateFlow<String> = name.asStateFlow()

    val points: StateFlow<List<TrailPoint>> = name
        .flatMapLatest { if (it.isEmpty()) flowOf(emptyList()) else dao(it).observeAll() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun select(trail: String) {
        name.value = trail
    }

    fun save(point: TrailPoint) = persist { dao(it).insertTrailData(point) }

    fun delete(point: TrailPoint?) {
        point ?: return
        persist { dao(it).deleteTrailData(point) }
    }

    private fun dao(trail: String) = TrailPointDatabase.getInstance(getApplication(), trail).trailDataDao()

    private fun persist(block: suspend (String) -> Unit) {
        val trail = name.value
        if (trail.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { withContext(NonCancellable) { block(trail) } }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
