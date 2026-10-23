package app.simple.positional.ui.compose

import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.simple.positional.R
import app.simple.positional.preferences.MainPreferences
import kotlinx.coroutines.delay

enum class MapRotation { Compass, Bearing, North }

@Stable
class GpsState(val custom: Boolean) {
    var provider by mutableStateOf(emptyList<InfoRow>())
    var location by mutableStateOf(emptyList<InfoRow>())
    var movement by mutableStateOf(emptyList<InfoRow>())
    var coordinates by mutableStateOf(emptyList<InfoRow>())
    var address by mutableStateOf("")
    var target by mutableStateOf(AnnotatedString(""))
    var speed by mutableFloatStateOf(0f)
    var message by mutableStateOf<Int?>(null)
    var fixed by mutableStateOf(false)
    var enabled by mutableStateOf(true)
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
fun GpsScreen(state: GpsState, map: View, topInset: Dp, landscape: Boolean, actions: GpsActions) {
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
    Surface(Modifier.fillMaxSize().then(keys), color = MaterialTheme.colorScheme.background) {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    MapLayer(state, map, actions)
                    MapTools(state, actions, horizontal = true, modifier = Modifier.align(Alignment.BottomCenter).padding(dimensionResource(R.dimen.trail_tools_margin)))
                }
                VerticalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 24.dp),
                ) {
                    GpsToolbar(state, actions, Modifier.padding(top = topInset))
                    GpsInfo(state, actions)
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                MapLayer(state, map, actions)
                AnimatedVisibility(
                    !state.fullScreen,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    GpsToolbar(state, actions, Modifier.padding(top = topInset).padding(dimensionResource(R.dimen.toolbar_padding) / 2))
                }
                MapTools(
                    state,
                    actions,
                    horizontal = false,
                    modifier = Modifier
                        .align(if (state.toolsLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = dimensionResource(R.dimen.trail_tools_margin)),
                )
                AnimatedVisibility(!state.fullScreen, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
                    SheetHandle { actions.sheet(true) }
                }
            }
            if (state.sheet) InfoSheet(state, actions)
        }
    }
}

@Composable
private fun BoxScope.MapLayer(state: GpsState, map: View, actions: GpsActions) {
    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
    if (state.targetMode) {
        Icon(
            painterResource(R.drawable.ic_crosshair),
            contentDescription = null,
            tint = colorResource(R.color.iconColor),
            modifier = Modifier.size(dimensionResource(R.dimen.app_button_size)).align(Alignment.Center),
        )
    }
    state.pressed?.let { point ->
        Box(Modifier.offset { point }) {
            DropdownMenu(expanded = true, onDismissRequest = { state.pressed = null }) {
                MenuEntry(R.string.target) { actions.setPressedTarget() }
                MenuEntry(R.string.navigate) { actions.navigatePressed() }
            }
        }
    }
}

@Composable
private fun GpsToolbar(state: GpsState, actions: GpsActions, modifier: Modifier = Modifier) {
    val radius = with(LocalDensity.current) { MainPreferences.getCornerRadius().toDp() }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(radius), color = MaterialTheme.colorScheme.background, shadowElevation = 5.dp) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.gps_location), style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.weight(1f))
            ToolButton(if (state.custom) R.drawable.ic_place_custom else state.pin, tint = false, onLongClick = actions::pinCustomization, onClick = actions::customLocation)
            IconAction(R.drawable.ic_menu, stringResource(R.string.gps_location), actions::menu)
        }
    }
}

@Composable
private fun MapTools(state: GpsState, actions: GpsActions, horizontal: Boolean, modifier: Modifier) {
    var confirm by remember { mutableStateOf(false) }
    val content: @Composable () -> Unit = {
        if (!horizontal) ToolButton(if (state.toolsLeft) R.drawable.ic_arrow_right else R.drawable.ic_arrow_left, onClick = actions::toggleToolsSide)
        ToolButton(locationIcon(state), onLongClick = { actions.locate(true) }) { actions.locate(false) }
        Box {
            ToolButton(
                if (state.targetSet || state.targetMode) R.drawable.ic_clear else R.drawable.ic_crosshair,
                onLongClick = { if (!state.targetSet) actions.targetCoordinates() },
            ) { if (state.targetSet) confirm = true else actions.toggleTargetMode() }
            DropdownMenu(expanded = confirm, onDismissRequest = { confirm = false }) {
                MenuEntry(R.string.sure) {
                    confirm = false
                    actions.removeTarget()
                }
                MenuEntry(R.string.button_cancel) { confirm = false }
            }
        }
        if (state.targetMode) ToolButton(R.drawable.ic_check, onClick = actions::confirmTarget)
        ToolButton(R.drawable.ic_compass_on, alpha = rotationAlpha(state, MapRotation.Compass)) { actions.rotate(MapRotation.Compass) }
        if (!state.custom) ToolButton(R.drawable.ic_bearing_rotation, alpha = rotationAlpha(state, MapRotation.Bearing)) { actions.rotate(MapRotation.Bearing) }
        ToolButton(R.drawable.ic_north_only, alpha = rotationAlpha(state, MapRotation.North)) { actions.rotate(MapRotation.North) }
    }
    val radius = with(LocalDensity.current) { MainPreferences.getCornerRadius().toDp() }
    Surface(modifier, shape = RoundedCornerShape(radius), color = MaterialTheme.colorScheme.background, shadowElevation = 10.dp) {
        val padding = Modifier.padding(dimensionResource(R.dimen.trail_tools_padding))
        if (horizontal) {
            Row(padding, horizontalArrangement = Arrangement.SpaceEvenly) { content() }
        } else {
            Column(padding, horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
    }
}

@Composable
private fun rotationAlpha(state: GpsState, rotation: MapRotation): Float =
    animateFloatAsState(if (state.rotation == rotation) 1f else 0.4f, label = "rotation").value

@Composable
private fun locationIcon(state: GpsState): Int {
    var blink by remember { mutableStateOf(false) }
    val searching = state.enabled && !state.fixed
    LaunchedEffect(searching) {
        while (searching) {
            delay(BLINK)
            blink = !blink
        }
    }
    return when {
        !state.enabled -> R.drawable.ic_gps_off
        state.fixed || blink -> R.drawable.ic_gps_fixed
        else -> R.drawable.ic_gps_not_fixed
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolButton(@DrawableRes icon: Int, alpha: Float = 1f, tint: Boolean = true, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .size(dimensionResource(R.dimen.app_button_size) + 8.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        val modifier = Modifier.size(24.dp)
        if (tint) {
            Icon(painterResource(icon), contentDescription = null, tint = colorResource(R.color.iconColor), modifier = modifier)
        } else {
            Image(painterResource(icon), contentDescription = null, modifier = modifier)
        }
    }
}

@Composable
private fun SheetHandle(onClick: () -> Unit) {
    Box(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 16.dp),
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.background, shadowElevation = 5.dp, onClick = onClick) {
            Icon(
                painterResource(R.drawable.ic_expand_up),
                contentDescription = stringResource(R.string.gps_info),
                tint = colorResource(R.color.iconColor),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).size(dimensionResource(R.dimen.app_button_size)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(state: GpsState, actions: GpsActions) {
    ModalBottomSheet(
        onDismissRequest = { actions.sheet(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            GpsInfo(state, actions)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GpsInfo(state: GpsState, actions: GpsActions) {
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
            if (!state.custom) IconAction(R.drawable.ic_save, stringResource(R.string.location_saved), actions::save)
            IconAction(R.drawable.ic_content_copy, stringResource(R.string.info_copied), actions::copy)
        }
        InfoSection(stringResource(R.string.gps_provider), state.provider)
        InfoSection(stringResource(R.string.gps_location), state.location, modifier = Modifier.combinedClickable(onClick = actions::locationDetails))
        InfoSection(stringResource(R.string.gps_movement), state.movement)
        Speedometer(state.speed)
        if (state.custom) {
            HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
            Text(
                stringResource(R.string.specified_location_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.textSecondary),
                modifier = Modifier.padding(10.dp),
            )
        }
        TargetSection(state.target, Modifier.combinedClickable(onClick = actions::showTarget))
        Box {
            InfoSection(
                stringResource(R.string.gps_coordinates),
                state.coordinates + InfoRow(stringResource(R.string.gps_address) + ":", state.address),
                graphic = painterResource(if (state.custom) R.drawable.ic_place_custom else state.pin),
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

private const val BLINK = 500L
private const val MESSAGE = 3000L
