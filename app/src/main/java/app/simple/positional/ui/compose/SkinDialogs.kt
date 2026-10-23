package app.simple.positional.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.constants.CompassBloom.compassBloomRes
import app.simple.positional.constants.LocationPins
import app.simple.positional.math.RotationPhysics
import app.simple.positional.preferences.CompassPreferences
import app.simple.positional.preferences.GPSPreferences

@Composable
private fun PickerDialog(title: String, onDismiss: () -> Unit, actions: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                actions()
            }
        },
        text = { Column { content() } },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
private fun SkinPager(count: Int, initial: Int, onSettled: (Int) -> Unit, page: @Composable (Int) -> Unit) {
    val state = rememberPagerState(initialPage = initial.coerceIn(0, count - 1)) { count }
    LaunchedEffect(state.settledPage) { onSettled(state.settledPage) }
    HorizontalPager(state = state, modifier = Modifier.fillMaxWidth().height(PAGER_HEIGHT.dp)) { index ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { page(index) }
    }
    Text(
        "${state.currentPage + 1}/$count",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
fun ClockNeedleDialog(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    PickerDialog(stringResource(R.string.needles), onDismiss) {
        SkinPager(clockNeedleSkins.size, selected, onSelect) { index ->
            val size = dimensionResource(R.dimen.clock_needle_dimensions)
            Image(painterResource(R.drawable.clock_face), contentDescription = null, modifier = Modifier.size(size))
            clockNeedleSkins[index].forEachIndexed { hand, res ->
                Image(painterResource(res), contentDescription = null, modifier = Modifier.size(size).rotate(NEEDLE_ANGLES.getOrElse(hand) { 0f }))
            }
        }
    }
}

@Composable
fun BloomSkinDialog(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    PickerDialog(stringResource(R.string.bloom_skins), onDismiss) {
        SkinPager(compassBloomRes.size, selected, onSelect) { index ->
            BLOOM_ANGLES.forEach { angle -> SkinImage(compassBloomRes[index], angle) }
        }
    }
}

@Composable
private fun SkinImage(@DrawableRes res: Int, angle: Float) {
    Image(painterResource(res), contentDescription = null, modifier = Modifier.size(dimensionResource(R.dimen.compass_bloom_dimension)).rotate(angle))
}

@Composable
fun PinCustomizationDialog(onDismiss: () -> Unit) {
    var size by remember { mutableFloatStateOf(GPSPreferences.getPinSize().toFloat()) }
    var opacity by remember { mutableFloatStateOf(GPSPreferences.getPinOpacity().toFloat()) }
    PickerDialog(
        stringResource(R.string.customize_pin),
        onDismiss,
        actions = {
            IconAction(R.drawable.ic_reset, stringResource(R.string.customize_pin)) {
                size = PIN_SIZE_DEFAULT
                opacity = PIN_OPACITY_MAX
                GPSPreferences.setPinSize(size.toInt())
                GPSPreferences.setPinOpacity(opacity.toInt())
            }
        },
    ) {
        SkinPager(LocationPins.locationsPins.size, GPSPreferences.getPinSkin(), GPSPreferences::setPinSkin) { index ->
            Image(
                painterResource(LocationPins.locationsPins[index]),
                contentDescription = null,
                modifier = Modifier.size((PIN_PREVIEW_UNIT * size).dp).alpha(opacity / PIN_OPACITY_MAX),
            )
        }
        LabeledSlider(R.string.size, size, PIN_SIZE_MIN..PIN_SIZE_MAX, steps = (PIN_SIZE_MAX - PIN_SIZE_MIN).toInt() - 1) {
            size = it
            GPSPreferences.setPinSize(it.toInt())
        }
        LabeledSlider(R.string.opacity, opacity, PIN_OPACITY_MIN..PIN_OPACITY_MAX) {
            opacity = it
            GPSPreferences.setPinOpacity(it.toInt())
        }
    }
}

@Composable
fun PhysicalPropertiesDialog(onDismiss: () -> Unit, onHelp: () -> Unit) {
    var enabled by remember { mutableStateOf(CompassPreferences.isUsingPhysicalProperties()) }
    var inertia by remember { mutableFloatStateOf(CompassPreferences.getRotationalInertia()) }
    var damping by remember { mutableFloatStateOf(CompassPreferences.getDampingCoefficient()) }
    var magnetic by remember { mutableFloatStateOf(CompassPreferences.getMagneticCoefficient()) }
    PickerDialog(
        stringResource(R.string.physical_properties),
        onDismiss,
        actions = {
            IconAction(R.drawable.ic_help_outline, stringResource(R.string.help), onHelp)
            IconAction(R.drawable.ic_reset, stringResource(R.string.physical_properties)) {
                inertia = RotationPhysics.INERTIA
                damping = RotationPhysics.DAMPING
                magnetic = RotationPhysics.MAGNETIC
                CompassPreferences.setRotationalInertia(inertia)
                CompassPreferences.setDampingCoefficient(damping)
                CompassPreferences.setMagneticCoefficient(magnetic)
            }
        },
    ) {
        PreferenceItem(
            title = stringResource(R.string.use_physical_properties),
            trailing = { Switch(checked = enabled, onCheckedChange = null) },
        ) {
            enabled = !enabled
            CompassPreferences.setUsePhysicalProperties(enabled)
        }
        Column(Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)) {
            LabeledSlider(R.string.rotational_inertia, inertia, 0f..INERTIA_MAX, enabled) {
                inertia = it
                CompassPreferences.setRotationalInertia(physicsValue(it))
            }
            LabeledSlider(R.string.damping_coefficient, damping, 0f..DAMPING_MAX, enabled) {
                damping = it
                CompassPreferences.setDampingCoefficient(physicsValue(it))
            }
            LabeledSlider(R.string.coefficient_of_magnetic_field, magnetic, 0f..MAGNETIC_MAX, enabled) {
                magnetic = it
                CompassPreferences.setMagneticCoefficient(physicsValue(it))
            }
        }
    }
}

fun physicsValue(value: Float): Float = value.coerceAtLeast(PHYSICS_MIN)

@Composable
private fun LabeledSlider(
    label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, steps = steps, enabled = enabled)
}

private const val PAGER_HEIGHT = 240
private const val PIN_PREVIEW_UNIT = 12f
private const val PIN_SIZE_MIN = 1f
private const val PIN_SIZE_MAX = 12f
private const val PIN_SIZE_DEFAULT = 4f
private const val PIN_OPACITY_MIN = 10f
private const val PIN_OPACITY_MAX = 255f
private const val INERTIA_MAX = 50f
private const val DAMPING_MAX = 50f
private const val MAGNETIC_MAX = 15000f
private const val PHYSICS_MIN = 0.1f
private const val DISABLED_ALPHA = 0.5f
private val NEEDLE_ANGLES = listOf(0f, 90f, 245f)
private val BLOOM_ANGLES = listOf(0f, 45f, 90f, 135f)
