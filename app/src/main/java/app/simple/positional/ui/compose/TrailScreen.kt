package app.simple.positional.ui.compose

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.constants.TrailIcons
import app.simple.positional.model.TrailPoint

@Immutable
data class TrailPointItem(
    val point: TrailPoint,
    val icon: Int,
    val name: String,
    val note: String,
    val coordinates: String,
    val accuracy: String,
    val date: String,
)

@Stable
class TrailState {
    var trail by mutableStateOf("")
    var summary by mutableStateOf(emptyList<InfoRow>())
    var points by mutableStateOf(emptyList<TrailPointItem>())
    var fixed by mutableStateOf(false)
    var enabled by mutableStateOf(true)
    var toolsLeft by mutableStateOf(false)
    var compass by mutableStateOf(false)
    var wrapped by mutableStateOf(false)
    var fullScreen by mutableStateOf(false)
    var sheet by mutableStateOf(false)
    var pickingMarker by mutableStateOf(false)
}

interface TrailActions {
    fun trails()
    fun addTrail()
    fun menu()
    fun locate(close: Boolean)
    fun addPoint()
    fun removeLast()
    fun wrap()
    fun toggleCompass()
    fun toggleToolsSide()
    fun sheet(open: Boolean)
    fun markerPicked(icon: Int, quick: Boolean)
    fun focus(item: TrailPointItem)
    fun delete(item: TrailPointItem)
    fun copy(item: TrailPointItem)
    fun navigate(item: TrailPointItem)
    fun share(item: TrailPointItem)
}

@Composable
fun TrailScreen(state: TrailState, map: View, topInset: Dp, landscape: Boolean, actions: TrailActions) {
    MapPanelScaffold(
        map = map,
        topInset = topInset,
        landscape = landscape,
        fullScreen = state.fullScreen,
        toolsLeft = state.toolsLeft,
        toolbar = {
            MapToolbar(stringResource(R.string.trail)) {
                IconAction(R.drawable.ic_add, stringResource(R.string.trail), actions::addTrail)
                IconAction(R.drawable.ic_saved_trails, stringResource(R.string.trail), actions::trails)
                IconAction(R.drawable.ic_menu, stringResource(R.string.trail), actions::menu)
            }
        },
        tools = { horizontal ->
            if (!horizontal) AlignToolButton(state.toolsLeft, actions::toggleToolsSide)
            LocationToolButton(state.fixed, state.enabled, actions::locate)
            if (state.points.isNotEmpty()) {
                MapToolButton(if (state.wrapped) R.drawable.ic_close_fullscreen else R.drawable.ic_full_screen, onClick = actions::wrap)
            }
            MapToolButton(if (state.compass) R.drawable.ic_compass_on else R.drawable.ic_compass_off, onClick = actions::toggleCompass)
            MapToolButton(R.drawable.ic_add_location, onClick = actions::addPoint)
            if (state.points.isNotEmpty()) ConfirmToolButton(R.drawable.ic_backspace, actions::removeLast)
        },
        side = { TrailPoints(state, actions) },
        bottom = { SheetHandle { actions.sheet(true) } },
    )
    if (state.sheet && !landscape) TrailSheet(state, actions)
    if (state.pickingMarker) MarkerPicker(actions) { state.pickingMarker = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrailSheet(state: TrailState, actions: TrailActions) {
    ModalBottomSheet(
        onDismissRequest = { actions.sheet(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            TrailPoints(state, actions)
        }
    }
}

@Composable
private fun TrailPoints(state: TrailState, actions: TrailActions) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoSection(state.trail.ifEmpty { stringResource(R.string.not_available) }, state.summary, modifier = Modifier.weight(1f))
            IconAction(R.drawable.ic_add_location, stringResource(R.string.trail), actions::addPoint)
        }
        if (state.points.isEmpty()) {
            Image(
                painterResource(R.drawable.art_trail),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentScale = ContentScale.FillWidth,
            )
        }
        state.points.forEach { TrailPointRow(it, actions) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrailPointRow(item: TrailPointItem, actions: TrailActions) {
    var menu by remember { mutableStateOf(false) }
    val secondary = colorResource(R.color.textSecondary)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onLongClick = { menu = true }) { actions.focus(item) }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(item.icon), contentDescription = null, tint = colorResource(R.color.iconColor), modifier = Modifier.size(22.dp))
            }
            Box(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.note, style = MaterialTheme.typography.bodyMedium, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.coordinates, style = MaterialTheme.typography.bodyMedium, color = secondary)
                Text(item.accuracy, style = MaterialTheme.typography.bodyMedium, color = secondary)
                Text(item.date, style = MaterialTheme.typography.bodyMedium, color = secondary)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            MenuEntry(R.string.navigate) {
                menu = false
                actions.navigate(item)
            }
            MenuEntry(R.string.share) {
                menu = false
                actions.share(item)
            }
            MenuEntry(R.string.copy) {
                menu = false
                actions.copy(item)
            }
            MenuEntry(R.string.delete) {
                menu = false
                actions.delete(item)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkerPicker(actions: TrailActions, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) } },
        containerColor = MaterialTheme.colorScheme.background,
        text = {
            LazyVerticalGrid(GridCells.Fixed(MARKER_COLUMNS)) {
                itemsIndexed(TrailIcons.icons.toList()) { index, icon ->
                    Box(
                        Modifier
                            .size(64.dp)
                            .combinedClickable(
                                onLongClick = {
                                    onDismiss()
                                    actions.markerPicked(index, true)
                                },
                            ) {
                                onDismiss()
                                actions.markerPicked(index, false)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(icon), contentDescription = null, tint = colorResource(R.color.iconColor), modifier = Modifier.size(32.dp))
                    }
                }
            }
        },
    )
}

@Composable
fun MarkerIconRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
        TrailIcons.icons.forEachIndexed { index, icon ->
            val highlight = if (index == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
            Box(
                Modifier
                    .size(48.dp)
                    .background(highlight, CircleShape)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = colorResource(R.color.iconColor), modifier = Modifier.size(26.dp))
            }
        }
    }
}

private const val MARKER_COLUMNS = 3
