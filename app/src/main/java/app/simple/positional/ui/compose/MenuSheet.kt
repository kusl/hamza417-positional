package app.simple.positional.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

sealed interface MenuItem {
    data class Toggle(@StringRes val label: Int, val checked: Boolean, val enabled: Boolean = true, val onChange: (Boolean) -> Unit) : MenuItem
    data class Action(@StringRes val label: Int, val onClick: () -> Unit) : MenuItem
}

@Stable
class MenuSheetState {
    private var builder: (() -> List<MenuItem>)? = null
    var items by mutableStateOf<List<MenuItem>?>(null)
        private set

    fun show(build: () -> List<MenuItem>) {
        builder = build
        items = build()
    }

    fun refresh() {
        items = builder?.invoke()
    }

    fun dismiss() {
        builder = null
        items = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(state: MenuSheetState) {
    val items = state.items ?: return
    ModalBottomSheet(
        onDismissRequest = state::dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            items.forEach { item ->
                when (item) {
                    is MenuItem.Toggle -> Column(Modifier.alpha(if (item.enabled) 1f else DISABLED)) {
                        PreferenceItem(
                            title = stringResource(item.label),
                            trailing = { Switch(checked = item.checked, onCheckedChange = null, enabled = item.enabled) },
                        ) {
                            if (item.enabled) {
                                item.onChange(!item.checked)
                                state.refresh()
                            }
                        }
                    }
                    is MenuItem.Action -> PreferenceItem(title = stringResource(item.label)) {
                        state.dismiss()
                        item.onClick()
                    }
                }
            }
        }
    }
}

private const val DISABLED = 0.4f
