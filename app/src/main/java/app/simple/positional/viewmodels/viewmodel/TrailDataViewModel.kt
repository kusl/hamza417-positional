package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.constants.TrailIcons
import app.simple.positional.database.instances.TrailPointDatabase
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.math.GeoDistance
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.model.TrailPoint
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.singleton.SharedPreferences.changes
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.TrailInfo
import app.simple.positional.ui.compose.TrailPointItem
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.DistanceText
import app.simple.positional.util.TimeFormatter.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class TrailDataViewModel(application: Application) : WrappedViewModel(application) {
    private val name = preference(TrailPreferences.selectedTrail) { TrailPreferences.getCurrentTrail() }

    val tools: StateFlow<MapTools> = preference(TrailPreferences.toolsMenuGravity, TrailPreferences.compass, TrailPreferences.wrapped) {
        MapTools(TrailPreferences.isToolsGravityToLeft(), TrailPreferences.isCompassRotation(), TrailPreferences.arePolylinesWrapped())
    }

    val points: StateFlow<List<TrailPoint>> = name
        .flatMapLatest { if (it.isEmpty()) flowOf(emptyList()) else dao(it).observeAll() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val info: StateFlow<TrailInfo> = combine(name, points, changes(MainPreferences.unit, MainPreferences.coordinatesFormat)) { trail, points, _ ->
        info(getContext(), trail, points)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), TrailInfo())

    fun save(point: TrailPoint) = persist { dao(it).insertTrailData(point) }

    fun delete(point: TrailPoint?) {
        point ?: return
        persist { dao(it).deleteTrailData(point) }
    }

    private fun info(context: Context, trail: String, points: List<TrailPoint>): TrailInfo {
        val metric = MainPreferences.isMetric()
        val distance = GeoDistance.path(points.map { it.latitude to it.longitude })
        return TrailInfo(
            trail = trail,
            summary = listOf(
                InfoRow(context.getString(R.string.total), points.size.toString()),
                InfoRow(context.getString(R.string.gps_displacement), DistanceText.format(context, distance, metric)),
            ),
            points = points.asReversed().map { item(context, it, metric) },
        )
    }

    private fun item(context: Context, point: TrailPoint, metric: Boolean): TrailPointItem {
        val (accuracy, unit) = DistanceText.parts(point.accuracy.toDouble(), metric)
        return TrailPointItem(
            point = point,
            icon = TrailIcons.icons.getOrElse(point.iconPosition) { TrailIcons.icons[0] },
            name = point.name.orEmpty().ifEmpty { "--" },
            note = point.note.orEmpty().ifEmpty { context.getString(R.string.not_available) },
            coordinates = context.getString(
                R.string.coordinates_format,
                DMSConverter.getFormattedLatitude(point.latitude, context),
                DMSConverter.getFormattedLongitude(point.longitude, context),
            ),
            accuracy = context.getString(R.string.trail_data_accuracy, round(accuracy, 1).toString(), context.getString(unit)),
            date = point.timeAdded.formatDate(),
        )
    }

    private fun dao(trail: String) = TrailPointDatabase.getInstance(getApplication(), trail).trailDataDao()

    private fun persist(block: suspend (String) -> Unit) {
        val trail = TrailPreferences.getCurrentTrail()
        if (trail.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { withContext(NonCancellable) { block(trail) } }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
