package app.simple.positional.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R

private val itemShape = RoundedCornerShape(16.dp)

@Composable
fun PreferenceCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colorResource(R.color.textSecondary),
            modifier = Modifier.padding(15.dp),
        )
        HorizontalDivider(Modifier.padding(horizontal = 15.dp), thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (!summary.isNullOrEmpty()) {
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onLongClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    PreferenceItem(
        title = title,
        summary = summary,
        onLongClick = onLongClick,
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        onClick = { onCheckedChange(!checked) },
    )
}