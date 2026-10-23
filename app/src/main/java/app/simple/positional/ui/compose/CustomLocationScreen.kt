package app.simple.positional.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.math.MathExtensions
import app.simple.positional.model.Locations
import app.simple.positional.util.DMSConverter

@Stable
class CustomLocationForm {
    var address by mutableStateOf("")
    var latitude by mutableStateOf("")
    var longitude by mutableStateOf("")

    fun fill(address: String, latitude: String, longitude: String) {
        this.address = address
        this.latitude = latitude
        this.longitude = longitude
    }
}

object CoordinateInput {
    fun parse(latitude: String, longitude: String): Pair<Double, Double>? {
        val lat = latitude.toCoordinate() ?: return null
        val lon = longitude.toCoordinate() ?: return null
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
    }

    fun format(value: Double): String = MathExtensions.round(value, 6).toString()

    private fun String.toCoordinate(): Double? = trim().replace(',', '.').toDoubleOrNull()
}

interface CustomLocationActions {
    fun search()
    fun save(latitude: Double, longitude: Double)
    fun setLocation(latitude: Double, longitude: Double, save: Boolean)
    fun select(location: Locations)
    fun useLocation(location: Locations)
    fun delete(location: Locations)
    fun deleteAll()
    fun help()
    fun failed()
}

@Composable
fun CustomLocationScreen(form: CustomLocationForm, locations: List<Locations>?, topInset: Dp, actions: CustomLocationActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(top = topInset)) {
            Header(form, actions) { confirmDelete = true }
            OutlinedTextField(
                value = form.address,
                onValueChange = { form.address = it },
                label = { Text(stringResource(R.string.gps_address)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                CoordinateField(form.latitude, R.string.specified_hint_latitude, Modifier.weight(1f)) { form.latitude = it }
                Spacer(Modifier.width(10.dp))
                CoordinateField(form.longitude, R.string.specified_hint_longitude, Modifier.weight(1f)) { form.longitude = it }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp), thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
            Box(Modifier.fillMaxSize()) {
                when {
                    locations == null -> Unit
                    locations.isEmpty() -> Image(
                        painterResource(R.drawable.art_empty),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp).align(Alignment.Center),
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(locations, key = { it.date }) { LocationCard(it, actions) }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.deleteAll()
                }) { Text(stringResource(R.string.sure)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
            },
            title = { Text(stringResource(R.string.delete_all), style = MaterialTheme.typography.titleLarge) },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Composable
private fun Header(form: CustomLocationForm, actions: CustomLocationActions, onDeleteAll: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val iconTint = colorResource(R.color.iconColor)
    fun submit(block: (Double, Double) -> Unit) {
        menu = false
        CoordinateInput.parse(form.latitude, form.longitude)?.let { (lat, lon) -> block(lat, lon) } ?: actions.failed()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.specified_hint_header), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = actions::search) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = iconTint)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    painterResource(R.drawable.ic_menu),
                    contentDescription = stringResource(R.string.open_specified_location_documentation),
                    tint = iconTint,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                MenuEntry(R.string.save) { submit(actions::save) }
                MenuEntry(R.string.set_and_save) { submit { lat, lon -> actions.setLocation(lat, lon, save = true) } }
                MenuEntry(R.string.set_only) { submit { lat, lon -> actions.setLocation(lat, lon, save = false) } }
                MenuEntry(R.string.delete_all) {
                    menu = false
                    onDeleteAll()
                }
                MenuEntry(R.string.help) {
                    menu = false
                    actions.help()
                }
            }
        }
    }
}

@Composable
private fun MenuEntry(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label), style = MaterialTheme.typography.titleMedium) }, onClick = onClick)
}

@Composable
internal fun CoordinateField(value: String, label: Int, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationCard(location: Locations, actions: CustomLocationActions) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    val latitude = remember(location.latitude) { DMSConverter.getFormattedLatitude(location.latitude, context) }
    val longitude = remember(location.longitude) { DMSConverter.getFormattedLongitude(location.longitude, context) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(Dp.Hairline, MaterialTheme.colorScheme.outline, shape)
            .combinedClickable(onClick = { actions.select(location) }, onLongClick = { actions.useLocation(location) })
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(location.address, style = MaterialTheme.typography.titleMedium)
            Text(latitude, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(longitude, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { actions.delete(location) }) {
            Icon(painterResource(R.drawable.ic_clear), contentDescription = stringResource(R.string.delete), tint = colorResource(R.color.iconColor))
        }
    }
}
