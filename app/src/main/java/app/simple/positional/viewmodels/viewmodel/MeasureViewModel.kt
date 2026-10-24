package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import app.simple.positional.database.instances.MeasureDatabase
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import app.simple.positional.preferences.MeasurePreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MeasureViewModel(application: Application) : WrappedViewModel(application) {
    private val dao = MeasureDatabase.getInstance(application).measureDao()
    private val selected = MutableStateFlow(MeasurePreferences.getLastSelectedMeasure())
    private val writes = Mutex()

    val measures: StateFlow<List<Measure>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    val measure: StateFlow<Measure?> = selected
        .flatMapLatest { dao.observeById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    fun addMeasure(name: String, note: String) {
        val created = Measure(System.currentTimeMillis(), name, note)
        persist {
            dao.insertMeasure(created)
            MeasurePreferences.setLastSelectedMeasure(created.dateCreated)
        }
    }

    fun addMeasurePoint(point: MeasurePoint) = editPoints { it + point }

    fun removeMeasurePoint(point: MeasurePoint?) {
        point ?: return
        editPoints { it - point }
    }

    fun deleteMeasure(measure: Measure) = persist {
        dao.deleteMeasure(measure)
        if (MeasurePreferences.getLastSelectedMeasure() == measure.dateCreated) MeasurePreferences.setLastSelectedMeasure(-1)
    }

    private fun editPoints(change: (List<MeasurePoint>) -> List<MeasurePoint>) {
        val id = selected.value
        persist {
            val current = dao.getMeasureById(id) ?: return@persist
            dao.updateMeasure(Measure(current.dateCreated, current.name, current.note, ArrayList(change(current.points))))
        }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { writes.withLock { block() } } }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MeasurePreferences.LAST_SELECTED_MEASURE) selected.value = MeasurePreferences.getLastSelectedMeasure()
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
