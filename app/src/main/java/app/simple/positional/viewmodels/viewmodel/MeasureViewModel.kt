package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.database.instances.MeasureDatabase
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.math.GeoDistance
import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.singleton.SharedPreferences.changes
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.util.DistanceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MeasureViewModel(application: Application) : WrappedViewModel(application) {
    private val dao = MeasureDatabase.getInstance(application).measureDao()
    private val selected = preference(MeasurePreferences.LAST_SELECTED_MEASURE) { MeasurePreferences.getLastSelectedMeasure() }
    private val writes = Mutex()

    val measures: StateFlow<List<Measure>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    val measure: StateFlow<Measure?> = selected
        .flatMapLatest { dao.observeById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    val summary: StateFlow<List<InfoRow>> = combine(measure, changes(MainPreferences.unit)) { measure, _ -> summary(measure?.points.orEmpty()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val tools: StateFlow<MapTools> = preference(MeasurePreferences.TOOLS_GRAVITY, MeasurePreferences.COMPASS_ROTATION, MeasurePreferences.POLYLINES_WRAPPED) {
        MapTools(MeasurePreferences.isToolsGravityToLeft(), MeasurePreferences.isCompassRotation(), MeasurePreferences.arePolylinesWrapped())
    }

    private fun summary(points: List<MeasurePoint>): List<InfoRow> {
        val context = getContext()
        val meters = GeoDistance.path(points.map { it.latitude to it.longitude })
        return listOf(
            InfoRow(context.getString(R.string.total), points.size.toString()),
            InfoRow(context.getString(R.string.gps_displacement), DistanceText.format(context, meters, MainPreferences.isMetric())),
        )
    }

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
        val id = MeasurePreferences.getLastSelectedMeasure()
        persist {
            val current = dao.getMeasureById(id) ?: return@persist
            dao.updateMeasure(Measure(current.dateCreated, current.name, current.note, ArrayList(change(current.points))))
        }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { writes.withLock { block() } } }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
