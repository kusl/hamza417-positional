package app.simple.positional.ui.compose

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import kotlinx.coroutines.delay

@Immutable
data class PanelItem(val tag: String, @param:DrawableRes val icon: Int, @param:StringRes val label: Int, @param:ColorRes val color: Int)

object PanelCatalog {
    const val CLOCK = "clock"
    const val COMPASS = "compass"
    const val DIRECTION = "direction"
    const val LOCATION = "location"
    const val TRAIL = "trail"
    const val MEASURE = "Measure"
    const val LEVEL = "level"
    const val SETTINGS = "settings"

    val tags = listOf(CLOCK, COMPASS, DIRECTION, LOCATION, TRAIL, MEASURE, LEVEL, SETTINGS)

    fun resolve(tag: String?): String = tag?.takeIf { it in tags } ?: LOCATION

    fun step(tag: String, delta: Int): String {
        val index = tags.indexOf(resolve(tag))
        return tags[Math.floorMod(index + delta, tags.size)]
    }

    fun items(): List<PanelItem> = listOf(
        PanelItem(CLOCK, R.drawable.ic_clock, R.string.clock, R.color.time_bb_color),
        PanelItem(COMPASS, R.drawable.ic_compass, R.string.compass, R.color.compass_bb_color),
        PanelItem(DIRECTION, R.drawable.ic_near_me, R.string.direction, R.color.direction_bb_color),
        PanelItem(LOCATION, LocationPins.getLocationPin(), R.string.gps_location, R.color.location_bb_color),
        PanelItem(TRAIL, R.drawable.ic_trail_line, R.string.trail, R.color.trail_bb_color),
        PanelItem(MEASURE, R.drawable.ic_square_foot, R.string.measure, R.color.measure_bb_color),
        PanelItem(LEVEL, R.drawable.ic_level, R.string.level, R.color.level_bb_color),
        PanelItem(SETTINGS, R.drawable.ic_settings, R.string.settings, R.color.settings_bb_color),
    )
}

@Composable
fun PanelSwitcher(current: String, visible: Boolean, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = remember(current) { PanelCatalog.items() }
    val item = items.first { it.tag == PanelCatalog.resolve(current) }
    var picker by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf(true) }
    LaunchedEffect(current) {
        label = true
        delay(LABEL_TIME)
        label = false
    }
    val threshold = with(LocalDensity.current) { SWIPE.dp.toPx() }
    AnimatedVisibility(visible, modifier = modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        Surface(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(dimensionResource(R.dimen.floating_button_margin)),
            shape = RoundedCornerShape(cornerRadius()),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier
                    .clickable { picker = true }
                    .pointerInput(current) {
                        var drag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { drag = 0f },
                            onDragEnd = { if (kotlin.math.abs(drag) > threshold) onSelect(PanelCatalog.step(current, if (drag < 0) 1 else -1)) },
                        ) { change, amount ->
                            change.consume()
                            drag += amount
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(label) {
                    Text(stringResource(item.label), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
                }
                Box(Modifier.size(dimensionResource(R.dimen.bottom_nav_height)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(item.icon), contentDescription = stringResource(item.label), tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
    if (picker) PanelPicker(items, current, onDismiss = { picker = false }) {
        picker = false
        onSelect(it)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelPicker(items: List<PanelItem>, current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyVerticalGrid(
            GridCells.Adaptive(CELL.dp),
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.tag }) { item ->
                Column(
                    Modifier
                        .clickable { onSelect(item.tag) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(colorResource(item.color).copy(alpha = if (item.tag == current) 1f else 0.85f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(item.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Text(
                        stringResource(item.label),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

private const val LABEL_TIME = 1200L
private const val SWIPE = 24
private const val CELL = 88
