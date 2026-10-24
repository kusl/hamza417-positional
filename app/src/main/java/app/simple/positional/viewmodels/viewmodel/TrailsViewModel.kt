package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.database.instances.TrailDatabase
import app.simple.positional.database.instances.TrailPointDatabase
import app.simple.positional.model.TrailEntry
import app.simple.positional.preferences.TrailPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrailsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TrailDatabase.getInstance(application).trailDao()

    val trails: StateFlow<List<TrailEntry>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    fun addTrail(trailEntry: TrailEntry) = persist {
        dao.insertTrail(trailEntry)
        TrailPreferences.setCurrentTrailName(trailEntry.trailName.orEmpty())
    }

    fun deleteTrail(trailEntry: TrailEntry) = persist {
        dao.deleteTrail(trailEntry)
        val name = trailEntry.trailName ?: return@persist
        if (TrailPreferences.getCurrentTrail() == name) TrailPreferences.setCurrentTrailName("")
        TrailPointDatabase.close(name)
        getApplication<Application>().deleteDatabase("$name.db")
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { withContext(NonCancellable) { block() } }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
