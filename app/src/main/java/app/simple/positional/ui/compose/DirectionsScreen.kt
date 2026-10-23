package app.simple.positional.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.model.DirectionModel
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.TimeFormatter.formatDate
import com.google.android.gms.maps.model.LatLng

interface DirectionsActions {
    fun create()
    fun createFromTarget()
    fun set(direction: DirectionModel)
    fun edit(direction: DirectionModel)
    fun useAsTarget(direction: DirectionModel)
    fun navigate(direction: DirectionModel)
    fun delete(direction: DirectionModel)
    fun search()
    fun save(latitude: Double, longitude: Double)
    fun dismissEditor()
}

@Composable
fun DirectionsScreen(directions: List<DirectionModel>?, editing: Boolean, form: CustomLocationForm, topInset: Dp, actions: DirectionsActions) {
    var addMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DirectionModel?>(null) }
    SavedListScreen(
        title = stringResource(R.string.targets),
        entries = directions,
        topInset = topInset,
        key = { it.dateAdded },
        action = {
            Box {
                AddButton { addMenu = true }
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    MenuEntry(R.string.create_new) {
                        addMenu = false
                        actions.create()
                    }
                    MenuEntry(R.string.save_from_target) {
                        addMenu = false
                        actions.createFromTarget()
                    }
                }
            }
        },
    ) { direction -> DirectionRow(direction, actions) { pendingDelete = direction } }
    if (editing) DirectionTargetDialog(form, actions)
    pendingDelete?.let { direction ->
        ConfirmDialog(stringResource(R.string.delete), onDismiss = { pendingDelete = null }) {
            pendingDelete = null
            actions.delete(direction)
        }
    }
}

@Composable
private fun DirectionRow(direction: DirectionModel, actions: DirectionsActions, onDelete: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val coordinates = remember(direction.latitude, direction.longitude) {
        DMSConverter.getDMS(LatLng(direction.latitude, direction.longitude), context)
    }
    val date = remember(direction.dateAdded) { direction.dateAdded.formatDate() }
    val pick = { block: () -> Unit ->
        menu = false
        block()
    }
    SavedItem(direction.name.orEmpty(), listOf(coordinates, date), highlighted = false, onClick = { actions.set(direction) }) {
        Box {
            IconAction(R.drawable.ic_menu, null) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                MenuEntry(R.string.button_set) { pick { actions.set(direction) } }
                MenuEntry(R.string.edit) { pick { actions.edit(direction) } }
                MenuEntry(R.string.use_as_target) { pick { actions.useAsTarget(direction) } }
                MenuEntry(R.string.navigate) { pick { actions.navigate(direction) } }
                MenuEntry(R.string.delete) { pick(onDelete) }
            }
        }
    }
}

@Composable
private fun DirectionTargetDialog(form: CustomLocationForm, actions: DirectionsActions) {
    val coordinates = CoordinateInput.parse(form.latitude, form.longitude)
    AlertDialog(
        onDismissRequest = actions::dismissEditor,
        confirmButton = {
            TextButton(
                enabled = coordinates != null && form.address.isNotBlank(),
                onClick = { coordinates?.let { (latitude, longitude) -> actions.save(latitude, longitude) } },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = actions::dismissEditor) { Text(stringResource(android.R.string.cancel)) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.target), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconAction(R.drawable.ic_search, null, actions::search)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = form.address,
                    onValueChange = { form.address = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    CoordinateField(form.latitude, R.string.specified_hint_latitude, Modifier.weight(1f)) { form.latitude = it }
                    Spacer(Modifier.width(10.dp))
                    CoordinateField(form.longitude, R.string.specified_hint_longitude, Modifier.weight(1f)) { form.longitude = it }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}
