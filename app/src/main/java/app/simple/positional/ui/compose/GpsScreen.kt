package app.simple.positional.ui.compose

import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import kotlinx.coroutines.delay

enum class MapRotation { Compass, Bearing, North }

@Immutable
data class GpsInfo(
    val provider: List<InfoRow> = emptyList(),
    val location: List<InfoRow> = emptyList(),
    val movement: List<InfoRow> = emptyList(),
    val coordinates: List<InfoRow> = emptyList(),
    val address: String = "",
    val target: AnnotatedString = AnnotatedString(""),
    val speed: Float = 0f,
    val fixed: Boolean = false,
    val enabled: Boolean = true,
    val custom: Boolean = false,
)

@Stable
class GpsState {
    var message by mutableStateOf<Int?>(null)
    var pin by mutableIntStateOf(R.drawable.ic_place_custom)
    var targetMode by mutableStateOf(false)
    var targetSet by mutableStateOf(false)
    var toolsLeft by mutableStateOf(false)
    var rotation by mutableStateOf(MapRotation.North)
    var volumeKeys by mutableStateOf(false)
    var fullScreen by mutableStateOf(false)
    var sheet by mutableStateOf(false)
    var pressed by mutableStateOf<IntOffset?>(null)
}

interface GpsActions {
    fun menu()
    fun customLocation()
    fun pinCustomization()
    fun locate(close: Boolean)
    fun toggleTargetMode()
    fun confirmTarget()
    fun targetCoordinates()
    fun removeTarget()
    fun toggleToolsSide()
    fun rotate(rotation: MapRotation)
    fun sheet(open: Boolean)
    fun save()
    fun copy()
    fun share()
    fun locationDetails()
    fun coordinateDetails()
    fun showTarget()
    fun setPressedTarget()
    fun navigatePressed()
    fun zoom(zoomIn: Boolean)
    fun recenter()
}

@Composable
fun GpsScreen(state: GpsState, info: GpsInfo, map: View, topInset: Dp, landscape: Boolean, actions: GpsActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.volumeKeys) { if (state.volumeKeys) runCatching { focus.requestFocus() } }
    val keys = Modifier
        .focusRequester(focus)
        .focusable(state.volumeKeys)
        .onKeyEvent { event ->
            if (!state.volumeKeys || event.type != KeyEventType.KeyDown) return@onKeyEvent false
            when (event.key) {
                Key.VolumeUp -> actions.zoom(true)
                Key.VolumeDown -> actions.zoom(false)
                Key.MediaPlayPause -> actions.recenter()
                else -> return@onKeyEvent false
            }
            true
        }
    MapPanelScaffold(
        map = map,
        topInset = topInset,
        landscape = landscape,
        fullScreen = state.fullScreen,
        toolsLeft = state.toolsLeft,
        modifier = keys,
        toolbar = {
            MapToolbar(stringResource(R.string.gps_location)) {
                MapToolButton(if (info.custom) R.drawable.ic_place_custom else state.pin, tint = false, onLongClick = actions::pinCustomization, onClick = actions::customLocation)
                IconAction(R.drawable.ic_menu, stringResource(R.string.gps_location), actions::menu)
            }
        },
        tools = { horizontal -> GpsTools(state, info, actions, horizontal) },
        overlay = {
            if (state.targetMode) MapCrosshair()
            state.pressed?.let { point ->
                Box(Modifier.offset { point }) {
                    DropdownMenu(expanded = true, onDismissRequest = { state.pressed = null }) {
                        MenuEntry(R.string.target) { actions.setPressedTarget() }
                        MenuEntry(R.string.navigate) { actions.navigatePressed() }
                    }
                }
            }
        },
        side = { GpsDetails(state, info, actions) },
        bottom = { SheetHandle { actions.sheet(true) } },
    )
    if (state.sheet && !landscape) InfoSheet(state, info, actions)
}

@Composable
private fun GpsTools(state: GpsState, info: GpsInfo, actions: GpsActions, horizontal: Boolean) {
    if (!horizontal) AlignToolButton(state.toolsLeft, actions::toggleToolsSide)
    LocationToolButton(info.fixed, info.enabled, actions::locate)
    if (state.targetSet) {
        ConfirmToolButton(R.drawable.ic_clear, actions::removeTarget)
    } else {
        MapToolButton(if (state.targetMode) R.drawable.ic_clear else R.drawable.ic_crosshair, onLongClick = actions::targetCoordinates, onClick = actions::toggleTargetMode)
    }
    if (state.targetMode) MapToolButton(R.drawable.ic_check, onClick = actions::confirmTarget)
    MapToolButton(R.drawable.ic_compass_on, alpha = toggleAlpha(state.rotation == MapRotation.Compass)) { actions.rotate(MapRotation.Compass) }
    if (!info.custom) MapToolButton(R.drawable.ic_bearing_rotation, alpha = toggleAlpha(state.rotation == MapRotation.Bearing)) { actions.rotate(MapRotation.Bearing) }
    MapToolButton(R.drawable.ic_north_only, alpha = toggleAlpha(state.rotation == MapRotation.North)) { actions.rotate(MapRotation.North) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(state: GpsState, info: GpsInfo, actions: GpsActions) {
    ModalBottomSheet(
        onDismissRequest = { actions.sheet(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            GpsDetails(state, info, actions)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GpsDetails(state: GpsState, info: GpsInfo, actions: GpsActions) {
    var shareMenu by remember { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE)
            state.message = null
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(state.message ?: R.string.gps_info, modifier = Modifier.weight(1f), label = "message") {
                Text(stringResource(it), style = MaterialTheme.typography.titleLarge)
            }
            if (!info.custom) IconAction(R.drawable.ic_save, stringResource(R.string.location_saved), actions::save)
            IconAction(R.drawable.ic_content_copy, stringResource(R.string.info_copied), actions::copy)
        }
        InfoSection(stringResource(R.string.gps_provider), info.provider)
        InfoSection(stringResource(R.string.gps_location), info.location, modifier = Modifier.combinedClickable(onClick = actions::locationDetails))
        InfoSection(stringResource(R.string.gps_movement), info.movement)
        Speedometer(info.speed)
        if (info.custom) {
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
            Text(
                stringResource(R.string.specified_location_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.textSecondary),
                modifier = Modifier.padding(10.dp),
            )
        }
        TargetSection(info.target, Modifier.combinedClickable(onClick = actions::showTarget))
        Box {
            InfoSection(
                stringResource(R.string.gps_coordinates),
                info.coordinates + InfoRow(stringResource(R.string.gps_address) + ":", info.address),
                graphic = painterResource(if (info.custom) R.drawable.ic_place_custom else state.pin),
                modifier = Modifier.combinedClickable(onLongClick = { shareMenu = true }, onClick = actions::coordinateDetails),
            )
            DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                MenuEntry(R.string.share) {
                    shareMenu = false
                    actions.share()
                }
            }
        }
    }
}

@Composable
private fun TargetSection(target: AnnotatedString, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp)) {
        Text(
            stringResource(R.string.target),
            fontFamily = PositionalFonts.light,
            fontSize = textSize(R.dimen.info_text_primary_size),
            color = colorResource(R.color.textPrimary),
        )
        Text(
            target,
            fontFamily = PositionalFonts.light,
            fontSize = textSize(R.dimen.info_text_secondary_size),
            color = colorResource(R.color.textSecondary),
        )
    }
}

private const val MESSAGE = 3000L
