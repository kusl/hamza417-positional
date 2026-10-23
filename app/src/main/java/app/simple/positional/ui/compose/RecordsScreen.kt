package app.simple.positional.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.simple.positional.R
import app.simple.positional.util.TimeFormatter.formatDate

@Immutable
data class Record<T>(val key: Any, val value: T, val name: String, val note: String, val date: Long, val selected: Boolean)

interface RecordActions<T> {
    fun open(record: T)
    fun add(name: String, note: String)
    fun delete(record: T)
    fun isValidName(name: String): Boolean
}

@Composable
fun <T> RecordsScreen(title: String, records: List<Record<T>>?, topInset: Dp, actions: RecordActions<T>) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Record<T>?>(null) }
    SavedListScreen(
        title = title,
        entries = records,
        topInset = topInset,
        key = { it.key },
        action = { AddButton { adding = true } },
    ) { record ->
        val date = remember(record.date) { record.date.formatDate() }
        SavedItem(record.name, listOf(record.note, date), record.selected, onClick = { actions.open(record.value) }) {
            IconAction(R.drawable.ic_clear, stringResource(R.string.delete)) { pendingDelete = record }
        }
    }
    if (adding) {
        NameNoteDialog(stringResource(R.string.create_new), actions::isValidName, onDismiss = { adding = false }) { name, note ->
            adding = false
            actions.add(name, note)
        }
    }
    pendingDelete?.let { record ->
        ConfirmDialog(stringResource(R.string.delete), onDismiss = { pendingDelete = null }) {
            pendingDelete = null
            actions.delete(record.value)
        }
    }
}
