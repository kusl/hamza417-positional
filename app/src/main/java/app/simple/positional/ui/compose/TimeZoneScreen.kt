package app.simple.positional.ui.compose

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class TimeZoneEntry(val id: String, val offset: String, val time: String)

object TimeZoneCatalog {
    fun entries(now: Instant, twelveHour: Boolean, locale: Locale): List<TimeZoneEntry> {
        val offset = DateTimeFormatter.ofPattern("xxx", locale)
        val time = DateTimeFormatter.ofPattern(if (twelveHour) "hh:mm a" else "HH:mm", locale)
        return ZoneId.getAvailableZoneIds().sorted().map { id ->
            val zoned = now.atZone(ZoneId.of(id))
            TimeZoneEntry(id, zoned.format(offset), zoned.format(time))
        }
    }

    fun filter(entries: List<TimeZoneEntry>, query: String): List<TimeZoneEntry> {
        val text = query.trim()
        if (text.isEmpty()) return entries
        return entries.filter { it.id.contains(text, ignoreCase = true) || it.offset.contains(text) }
    }
}

@Composable
fun TimeZoneScreen(selected: String, twelveHour: Boolean, locale: Locale, topInset: Dp, onSelect: (String) -> Unit) {
    val all by produceState<List<TimeZoneEntry>?>(null) {
        value = withContext(Dispatchers.Default) { TimeZoneCatalog.entries(Instant.now(), twelveHour, locale) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(all, query) { all?.let { TimeZoneCatalog.filter(it, query) }.orEmpty() }
    val state = rememberLazyListState()
    LaunchedEffect(all) {
        visible.indexOfFirst { it.id == selected }.takeIf { it > 0 }?.let { state.scrollToItem(it) }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(top = topInset)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_timezone)) },
                singleLine = true,
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = colorResource(R.color.iconColor)) },
                trailingIcon = { if (query.isNotEmpty()) IconAction(R.drawable.ic_clear, null) { query = "" } },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyColumn(state = state, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                items(visible, key = { it.id }) { entry -> TimeZoneRow(entry, entry.id == selected) { onSelect(entry.id) } }
            }
        }
    }
}

@Composable
private fun TimeZoneRow(entry: TimeZoneEntry, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp)) {
            if (selected) Icon(painterResource(R.drawable.ic_circle_stroke), contentDescription = null, tint = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.id, style = MaterialTheme.typography.titleMedium, color = if (selected) accent else Color.Unspecified)
            Text(entry.offset, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(entry.time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
