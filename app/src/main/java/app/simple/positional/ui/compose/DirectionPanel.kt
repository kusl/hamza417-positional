package app.simple.positional.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.math.Angle.normalizeEulerAngle
import kotlin.math.abs

@Immutable
data class DirectionInfo(
    val target: String = "",
    val coordinates: List<InfoRow> = emptyList(),
    val displacement: String = "",
    val bearing: Float = 0f,
)

@Stable
class DirectionState {
    val dial = RotationState(0.5f, 8f, 5000f)
    val arrow = RotationState(1f, 8f, 5000f)
    var bearing = 0f

    fun onHeading(azimuth: Float) {
        dial.update(-azimuth, true)
        arrow.update((bearing - azimuth).normalizeEulerAngle(false), true)
    }
}

@Composable
fun DirectionPanel(state: DirectionState, info: DirectionInfo, topInset: Dp, onCalibrate: () -> Unit, onTarget: () -> Unit, onMenu: () -> Unit) {
    val azimuth by remember(state) { derivedStateOf { abs(state.dial.angle.normalizeEulerAngle(true).toInt()) } }
    val bearing by remember(state) { derivedStateOf { abs(state.arrow.angle.normalizeEulerAngle(false).toInt()) } }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PanelToolbar(stringResource(R.string.direction), topInset) {
                IconAction(R.drawable.ic_compass_calibration, stringResource(R.string.compass_options), onCalibrate)
                IconAction(R.drawable.ic_near_me, stringResource(R.string.target), onTarget)
                IconAction(R.drawable.ic_menu, stringResource(R.string.direction), onMenu)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.toolbar_padding))
                    .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "$azimuth°",
                    fontFamily = PositionalFonts.light,
                    fontSize = textSize(R.dimen.compass_direction_text_size),
                    color = colorResource(R.color.textPrimary),
                    modifier = Modifier.padding(top = 24.dp),
                )
                Box(Modifier.padding(vertical = 100.dp), contentAlignment = Alignment.Center) {
                    RotatingImage(R.drawable.compass_dial, state.dial, Modifier.size(dimensionResource(R.dimen.compass_dial)))
                    RotatingImage(R.drawable.ic_direction_indicator, state.arrow, Modifier.size(dimensionResource(R.dimen.compass_bloom_dimension)))
                    Text("$bearing°", fontFamily = PositionalFonts.degrees, fontSize = textSize(R.dimen.compass_degrees_text_size), color = MaterialTheme.colorScheme.primary)
                }
                InfoSection(
                    stringResource(R.string.gps_location),
                    listOf(
                        InfoRow("${stringResource(R.string.target)}:", info.target),
                        InfoRow(stringResource(R.string.gps_bearing), "$bearing°"),
                        InfoRow(stringResource(R.string.gps_displacement), info.displacement),
                    ),
                )
                InfoSection(stringResource(R.string.gps_coordinates), info.coordinates)
                InfoSection(stringResource(R.string.compass), listOf(InfoRow(stringResource(R.string.moon_azimuth), "$azimuth°")))
            }
        }
    }
}
