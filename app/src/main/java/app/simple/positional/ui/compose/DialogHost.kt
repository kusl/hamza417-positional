package app.simple.positional.ui.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.simple.positional.R

interface PanelDialog

data class ErrorDialog(val message: String) : PanelDialog

@Stable
class DialogHost {
    var current by mutableStateOf<PanelDialog?>(null)
        private set

    fun show(dialog: PanelDialog) {
        current = dialog
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun MessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        title = { Text(stringResource(R.string.error), style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        containerColor = MaterialTheme.colorScheme.background,
    )
}
