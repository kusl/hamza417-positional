package app.simple.positional.ui.compose

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R

@Stable
class MeasureState {
    var selected by mutableStateOf(false)
    var name by mutableStateOf("")
    var current by mutableStateOf("")
    var hasPoints by mutableStateOf(false)
    var fixed by mutableStateOf(false)
    var enabled by mutableStateOf(true)
    var toolsLeft by mutableStateOf(false)
    var compass by mutableStateOf(false)
    var wrapped by mutableStateOf(false)
    var fullScreen by mutableStateOf(false)
}

interface MeasureActions {
    fun addMeasure()
    fun measures()
    fun menu()
    fun locate(close: Boolean)
    fun addPoint()
    fun removeLast()
    fun wrap()
    fun toggleCompass()
    fun toggleToolsSide()
}

@Composable
fun MeasureScreen(state: MeasureState, summary: List<InfoRow>, map: View, topInset: Dp, actions: MeasureActions) {
    MapPanelScaffold(
        map = map,
        topInset = topInset,
        landscape = false,
        fullScreen = state.fullScreen,
        toolsLeft = state.toolsLeft,
        toolbar = {
            MapToolbar(stringResource(R.string.measure)) {
                IconAction(R.drawable.ic_add, stringResource(R.string.measure), actions::addMeasure)
                IconAction(R.drawable.ic_square_foot, stringResource(R.string.measure), actions::measures)
                IconAction(R.drawable.ic_menu, stringResource(R.string.measure), actions::menu)
            }
        },
        tools = {
            AlignToolButton(state.toolsLeft, actions::toggleToolsSide)
            LocationToolButton(state.fixed, state.enabled, actions::locate)
            if (state.selected && state.hasPoints) {
                MapToolButton(if (state.wrapped) R.drawable.ic_close_fullscreen else R.drawable.ic_full_screen, onClick = actions::wrap)
            }
            MapToolButton(if (state.compass) R.drawable.ic_compass_on else R.drawable.ic_compass_off, onClick = actions::toggleCompass)
            if (state.selected) {
                MapToolButton(R.drawable.ic_add_circle, onClick = actions::addPoint)
                if (state.hasPoints) ConfirmToolButton(R.drawable.ic_backspace, actions::removeLast)
            }
        },
        overlay = { if (state.selected) MapCrosshair() },
        bottom = { if (state.selected) MeasureCard(state, summary) },
    )
}

@Composable
private fun MeasureCard(state: MeasureState, summary: List<InfoRow>) {
    Surface(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = dimensionResource(R.dimen.trail_tools_margin))
            .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius()),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 5.dp,
    ) {
        Column(Modifier.padding(horizontal = 6.dp)) {
            InfoSection(state.name, summary)
            if (state.current.isNotEmpty()) {
                Text(
                    state.current,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(R.color.textPrimary),
                    modifier = Modifier.padding(start = 10.dp, bottom = 12.dp),
                )
            }
        }
    }
}
