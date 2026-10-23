package app.simple.positional.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.math.ClockMotion
import app.simple.positional.math.TimeConverter.getHoursInDegrees
import app.simple.positional.math.TimeConverter.getHoursInDegreesFor24
import app.simple.positional.math.TimeConverter.getMinutesInDegrees
import app.simple.positional.math.TimeConverter.getSecondsInDegrees
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime

@Immutable
data class ClockTime(val local: List<InfoRow> = emptyList(), val utc: List<InfoRow> = emptyList())

@Immutable
data class SkyReport(
    val sunPosition: List<InfoRow>,
    val sunTime: List<InfoRow>,
    val twilight: List<InfoRow>,
    val moonPosition: List<InfoRow>,
    val moonTime: List<InfoRow>,
    val moonIllumination: List<InfoRow>,
    val moonDates: List<InfoRow>,
    val sunAzimuth: Float,
    val moonAzimuth: Float,
    val moonGraphic: Int,
)

@Stable
class ClockState {
    val hour = RotationState(0.5f, 8f, 5000f)
    val minute = RotationState(0.5f, 8f, 5000f)
    val second = RotationState()
    val sweep = RotationState()
    var zone by mutableStateOf<ZoneId>(ZoneId.systemDefault())
    var motion by mutableStateOf(ClockMotion.TICK)
    var face24 by mutableStateOf(false)
    var needles by mutableStateOf(clockNeedleSkins[0].toList())
    var daytime by mutableStateOf(true)
    var customLocation by mutableStateOf(false)
    var pin by mutableIntStateOf(R.drawable.ic_pin_01)
    var time by mutableStateOf(ClockTime())
    var sky by mutableStateOf<SkyReport?>(null)

    fun show(now: ZonedDateTime) {
        hour.update(if (face24) getHoursInDegreesFor24(now) else getHoursInDegrees(now), true)
        minute.update(getMinutesInDegrees(now), true)
        val seconds = getSecondsInDegrees(now, motion.fractional)
        second.update(seconds, motion.animated)
        sweep.update(seconds - 90f, motion.animated)
        daytime = now.hour in 6..18
    }
}

@Composable
fun ClockScreen(state: ClockState, topInset: Dp, onTimezone: () -> Unit, onMenu: () -> Unit, onLocation: () -> Unit) {
    val motion = state.motion
    LaunchedEffect(state, motion, state.zone, state.face24) {
        listOf(state.second, state.sweep).forEach { it.configure(motion.inertia, motion.damping, motion.magnetic) }
        while (true) {
            state.show(ZonedDateTime.now(state.zone))
            if (motion.intervalMillis > 0) delay(motion.intervalMillis - System.currentTimeMillis() % motion.intervalMillis)
            withFrameMillis { }
        }
    }
    val clockSize = dimensionResource(R.dimen.clock_image_dimensions)
    val needleSize = dimensionResource(R.dimen.clock_needle_dimensions)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PanelToolbar(stringResource(R.string.clock), topInset) {
                IconAction(R.drawable.ic_timezone, stringResource(R.string.clock_options), onTimezone)
                IconAction(R.drawable.ic_menu, stringResource(R.string.clock_options), onMenu)
                IconAction(if (state.customLocation) R.drawable.ic_place_custom else state.pin, stringResource(R.string.clock_options), onLocation)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.toolbar_padding))
                    .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 24.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 100.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_clock_shadow), null, Modifier.size(dimensionResource(R.dimen.clock_image_shadow_size)))
                    RotatingImage(R.drawable.clock_trail, state.sweep, Modifier.size(clockSize).padding(5.dp))
                    Image(painterResource(if (state.face24) R.drawable.clock_face_24 else R.drawable.clock_face), null, Modifier.size(clockSize))
                    Image(
                        painterResource(if (state.daytime) R.drawable.ic_day else R.drawable.ic_night),
                        contentDescription = null,
                        modifier = Modifier.offset(y = (-30).dp).size(dimensionResource(R.dimen.clock_day_night_indicator)),
                    )
                    RotatingImage(state.needles[0], state.hour, Modifier.size(needleSize))
                    RotatingImage(state.needles[1], state.minute, Modifier.size(needleSize))
                    RotatingImage(state.needles[2], state.second, Modifier.height(needleSize))
                }
                if (state.customLocation) {
                    Text(
                        stringResource(R.string.specified_location_notice),
                        fontFamily = PositionalFonts.light,
                        color = colorResource(R.color.textSecondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 10.dp),
                    )
                }
                InfoSection(stringResource(R.string.local_time), state.time.local, painterResource(R.drawable.ic_access_time))
                InfoSection(stringResource(R.string.utc_time), state.time.utc)
                state.sky?.let { SkySections(it) }
            }
        }
    }
}

@Composable
private fun SkySections(sky: SkyReport) {
    SunPositionDiagram(sky.sunAzimuth, sky.moonAzimuth, sky.moonGraphic, Modifier.fillMaxWidth().padding(top = 20.dp).height(250.dp))
    InfoSection(stringResource(R.string.sun_position), sky.sunPosition)
    InfoSection(stringResource(R.string.sun_time), sky.sunTime, painterResource(R.drawable.ic_sunrise))
    InfoSection(stringResource(R.string.twilight), sky.twilight, painterResource(R.drawable.ic_twilight))
    InfoSection(stringResource(R.string.moon_position), sky.moonPosition)
    InfoSection(stringResource(R.string.moon_time), sky.moonTime, painterResource(R.drawable.ic_moon_phase))
    InfoSection(stringResource(R.string.moon_illumination), sky.moonIllumination, painterResource(sky.moonGraphic))
    InfoSection(stringResource(R.string.moon_dates), sky.moonDates)
}
