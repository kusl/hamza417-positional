package app.simple.positional.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R

@Composable
fun <T> SavedListScreen(
    title: String,
    entries: List<T>?,
    topInset: Dp,
    key: (T) -> Any,
    action: @Composable () -> Unit,
    row: @Composable (T) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 16.dp)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                        if (entries != null) {
                            Text(
                                "${stringResource(R.string.total)} ${entries.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (entries != null) items(entries, key = key) { row(it) }
            }
            if (entries?.isEmpty() == true) {
                Image(painterResource(R.drawable.art_lost), contentDescription = null, modifier = Modifier.size(200.dp).align(Alignment.Center))
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(24.dp)) { action() }
        }
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.create_new))
    }
}

@Composable
fun SavedItem(title: String, lines: List<String>, highlighted: Boolean, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(Dp.Hairline, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (highlighted) MaterialTheme.colorScheme.primary else Color.Unspecified)
            lines.filter { it.isNotBlank() }.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
fun IconAction(icon: Int, description: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = description, tint = colorResource(R.color.iconColor))
    }
}

@Composable
internal fun MenuEntry(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label), style = MaterialTheme.typography.titleMedium) }, onClick = onClick)
}

@Composable
fun ConfirmDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.sure)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
fun NameNoteDialog(
    title: String,
    isValid: (String) -> Boolean,
    onDismiss: () -> Unit,
    initial: Pair<String, String> = "" to "",
    onDraft: (String, String) -> Unit = { _, _ -> },
    extra: (@Composable () -> Unit)? = null,
    onSave: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial.first) }
    var note by rememberSaveable { mutableStateOf(initial.second) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = isValid(name.trim()), onClick = { onSave(name.trim(), note.trim()) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                extra?.invoke()
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onDraft(name, note)
                    },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        onDraft(name, note)
                    },
                    label = { Text(stringResource(R.string.note)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}
