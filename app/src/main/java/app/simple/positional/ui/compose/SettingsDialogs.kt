package app.simple.positional.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import app.simple.positional.R
import app.simple.positional.util.AppIcons
import app.simple.positional.util.MapSource
import kotlin.math.roundToInt

@Composable
internal fun SettingsDialogHost(dialog: SettingsDialog, state: SettingsState, actions: SettingsActions, onDismiss: () -> Unit) {
    fun <T> select(action: (T) -> Unit): (T) -> Unit = {
        onDismiss()
        action(it)
    }
    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.theme),
            choices = SettingsLabels.themeModes.map { Choice(it, stringResource(SettingsLabels.theme(it)!!)) },
            selected = state.themeMode,
            onDismiss = onDismiss,
            onSelect = select(actions::setThemeMode),
        )
        SettingsDialog.ICON -> IconDialog(
            selected = state.iconIndex,
            onDismiss = onDismiss,
            onSelect = select(actions::setIcon),
        )
        SettingsDialog.CORNERS -> CornerDialog(
            initial = state.cornerRadius,
            onDismiss = onDismiss,
            onConfirm = select(actions::setCornerRadius),
        )
        SettingsDialog.UNITS -> ChoiceDialog(
            title = stringResource(R.string.units),
            choices = listOf(true, false).map { Choice(it, stringResource(SettingsLabels.unit(it))) },
            selected = state.isMetric,
            onDismiss = onDismiss,
            onSelect = select(actions::setMetric),
        )
        SettingsDialog.COORDINATES -> ChoiceDialog(
            title = stringResource(R.string.app_coordinates_format),
            choices = SettingsLabels.coordinateFormats.map { Choice(it, stringResource(SettingsLabels.coordinatesFormat(it)!!)) },
            selected = state.coordinatesFormat,
            onDismiss = onDismiss,
            onSelect = select(actions::setCoordinatesFormat),
        )
        SettingsDialog.PROVIDER -> ChoiceDialog(
            title = stringResource(R.string.location_provider),
            choices = SettingsLabels.providers.map {
                Choice(
                    it,
                    stringResource(SettingsLabels.locationProvider(it)!!),
                    stringResource(SettingsLabels.locationProviderDescription(it)!!),
                )
            },
            selected = state.locationProvider,
            note = stringResource(R.string.desc_location_provider_note),
            onDismiss = onDismiss,
            onSelect = select(actions::setLocationProvider),
        )
        SettingsDialog.MAP_SOURCE -> ChoiceDialog(
            title = stringResource(R.string.map_source),
            choices = MapSource.choices.map {
                Choice(it, stringResource(SettingsLabels.mapSource(it)!!), stringResource(SettingsLabels.mapSourceDescription(it)!!))
            },
            selected = state.mapSource,
            note = stringResource(R.string.desc_map_source_note),
            onDismiss = onDismiss,
            onSelect = select(actions::setMapSource),
        )
        SettingsDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.language),
            choices = SettingsLabels.languages().map { (code, name) ->
                Choice(code, name ?: stringResource(R.string.auto_system_default_language))
            },
            selected = state.language,
            onDismiss = onDismiss,
            onSelect = select(actions::setLanguage),
        )
    }
}


@Composable
private fun IconDialog(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        title = { Text(stringResource(R.string.icon), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppIcons.icons.indices.chunked(ICONS_PER_ROW).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { index ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (index == selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                    .clickable { onSelect(index) },
                            ) {
                                Image(mipmapPainter(AppIcons.icons[index].second), contentDescription = null, modifier = Modifier.size(50.dp))
                            }
                        }
                        repeat(ICONS_PER_ROW - row.size) { Box(Modifier.size(60.dp)) }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
private fun CornerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember { mutableFloatStateOf(initial.toFloat()) }
    val radius = value.roundToInt() / 5f
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(value.roundToInt()) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        title = { Text(stringResource(R.string.corner_radius), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(with(LocalDensity.current) { radius.toDp() }))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text("$radius px", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = SettingsLabels.CORNER_MIN.toFloat()..SettingsLabels.CORNER_MAX.toFloat(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
private fun mipmapPainter(@DrawableRes id: Int): Painter {
    val context = LocalContext.current
    return remember(id) {
        BitmapPainter(ResourcesCompat.getDrawable(context.resources, id, context.theme)!!.toBitmap(ICON_BITMAP_SIZE, ICON_BITMAP_SIZE).asImageBitmap())
    }
}

private const val ICONS_PER_ROW = 4
private const val ICON_BITMAP_SIZE = 192
