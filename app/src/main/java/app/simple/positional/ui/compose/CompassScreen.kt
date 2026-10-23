package app.simple.positional.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.math.Angle
import app.simple.positional.math.Angle.normalizeEulerAngle
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.sensors.SensorAccuracy
import app.simple.positional.util.Direction
import app.simple.positional.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

fun axisRows(values: FloatArray): List<InfoRow> =
    listOf("X:", "Y:", "Z:").mapIndexed { index, axis -> InfoRow(axis, round(values[index].toDouble(), 3).toString()) }

@Stable
class CompassState {
    val dial = RotationState()
    val flowers = List(4) { RotationState() }
    var heading by mutableFloatStateOf(0f)
        private set
    var bloom by mutableStateOf<Int?>(null)
    var directionCode by mutableStateOf(true)
    var accelerometerAccuracy by mutableIntStateOf(-1)
    var magnetometerAccuracy by mutableIntStateOf(-1)
    var accelerometer by mutableStateOf(emptyList<InfoRow>())
    var magnetometer by mutableStateOf(emptyList<InfoRow>())
    var field by mutableStateOf(emptyList<InfoRow>())
    var physics = true
    private var dragging = false
    private var target = 0f
    private var touchStart = 0f
    private var dialStart = 0f
    private var release: Job? = null

    fun configure(inertia: Float, damping: Float, magnetic: Float) {
        dial.configure(inertia, damping, magnetic)
        flowers.forEach { it.configure(inertia, damping, magnetic) }
    }

    fun onHeading(azimuth: Float) {
        target = azimuth
        if (!dragging) show(azimuth, physics)
    }

    fun beginDrag(touchAngle: Float) {
        release?.cancel()
        dragging = true
        touchStart = touchAngle
        dialStart = dial.angle
    }

    fun drag(touchAngle: Float) {
        show(abs((touchAngle - touchStart + dialStart).normalizeEulerAngle(true)), false)
    }

    fun endDrag(scope: CoroutineScope) {
        release = scope.launch {
            delay(RELEASE_DELAY)
            if (!physics) dial.glideTo(-target, RELEASE_DURATION)
            dragging = false
            show(target, physics)
        }
    }

    private fun show(azimuth: Float, animate: Boolean) {
        heading = azimuth
        dial.update(-azimuth, animate)
        if (bloom == null) return
        flowers[0].update((azimuth * 2).normalizeEulerAngle(false), animate)
        flowers[1].update((azimuth * -3 + 45).normalizeEulerAngle(false), animate)
        flowers[2].update((azimuth + 90).normalizeEulerAngle(false), animate)
        flowers[3].update((azimuth * -4 + 135).normalizeEulerAngle(false), animate)
    }

    private companion object {
        const val RELEASE_DELAY = 1000L
        const val RELEASE_DURATION = 1000
    }
}

@Composable
fun CompassScreen(state: CompassState, topInset: Dp, onCalibrate: () -> Unit, onMenu: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val degrees by remember(state) { derivedStateOf { abs(state.dial.angle.normalizeEulerAngle(true).toInt()) } }
    val direction by remember(state, context) {
        derivedStateOf {
            val azimuth = state.heading.toDouble()
            val name = if (state.directionCode) Direction.getDirectionCodeFromAzimuth(context, azimuth) else Direction.getDirectionNameFromAzimuth(context, azimuth)
            name.uppercase(LocaleHelper.getAppLocale())
        }
    }
    val degreesColor by animateColorAsState(if (state.bloom != null) Color.White else MaterialTheme.colorScheme.primary, label = "degrees")
    val iconColor = colorResource(R.color.iconColor)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PanelToolbar(stringResource(R.string.compass), topInset) {
                IconAction(R.drawable.ic_compass_calibration, stringResource(R.string.compass_options), onCalibrate)
                IconAction(R.drawable.ic_menu, stringResource(R.string.compass_options), onMenu)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.toolbar_padding))
                    .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(painterResource(R.drawable.ic_navigation), contentDescription = null, tint = iconColor, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
                Text(direction, fontFamily = PositionalFonts.light, fontSize = textSize(R.dimen.compass_direction_text_size), color = colorResource(R.color.textPrimary))
                Box(
                    modifier = Modifier
                        .padding(vertical = 100.dp)
                        .pointerInput(state) {
                            fun angleAt(position: Offset) =
                                Angle.getAngle(position.x.toDouble(), position.y.toDouble(), size.width.toFloat(), size.height.toFloat())
                            detectDragGestures(
                                onDragStart = { state.beginDrag(angleAt(it)) },
                                onDragEnd = { state.endDrag(scope) },
                                onDragCancel = { state.endDrag(scope) },
                            ) { change, _ ->
                                change.consume()
                                state.drag(angleAt(change.position))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_drop_up),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = dimensionResource(R.dimen.degree_indicator_margin_top))
                            .size(dimensionResource(R.dimen.degrees_indicator_dimension)),
                    )
                    RotatingImage(R.drawable.compass_dial, state.dial, Modifier.size(dimensionResource(R.dimen.compass_dial)))
                    state.bloom?.let { res ->
                        val size = dimensionResource(R.dimen.compass_bloom_dimension)
                        state.flowers.forEach { RotatingImage(res, it, Modifier.size(size)) }
                    }
                    Text("$degrees°", fontFamily = PositionalFonts.degrees, fontSize = textSize(R.dimen.compass_degrees_text_size), color = degreesColor)
                }
                InfoSection(
                    stringResource(R.string.compass_accuracy),
                    listOf(
                        InfoRow(stringResource(R.string.accelerometer_accuracy), stringResource(SensorAccuracy.label(state.accelerometerAccuracy))),
                        InfoRow(stringResource(R.string.magnetometer_accuracy), stringResource(SensorAccuracy.label(state.magnetometerAccuracy))),
                    ),
                )
                InfoSection(stringResource(R.string.accelerometer), state.accelerometer)
                InfoSection(stringResource(R.string.magnetometer), state.magnetometer)
                InfoSection(stringResource(R.string.compass_field), state.field)
            }
        }
    }
}
