package app.simple.positional.ui.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import app.simple.positional.R
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.SeriesStats
import app.simple.positional.math.UnitConverter.toFeet
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.CoordinateFormats
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.LocationShare
import app.simple.positional.util.ReverseGeocoder

@Composable
private fun InfoDialog(title: String, onDismiss: () -> Unit, action: Pair<String, () -> Unit>? = null, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = action?.let { (label, onClick) -> @Composable { TextButton(onClick = onClick) { Text(label) } } },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { content() } },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
fun LocationSummaryDialog(latitude: Double, longitude: Double, custom: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val notAvailable = stringResource(R.string.not_available)
    val address by produceState(if (custom) MainPreferences.getAddress() else "-", latitude, longitude) {
        if (!custom) value = ReverseGeocoder.addressLine(context, latitude, longitude) ?: notAvailable
    }
    InfoDialog(
        stringResource(R.string.gps_location),
        onDismiss,
        action = stringResource(R.string.share) to { LocationShare.share(context, latitude, longitude, address) },
    ) {
        InfoSection(
            stringResource(R.string.gps_coordinates),
            listOf(
                InfoRow(stringResource(R.string.local_timezone), ClockPreferences.getTimeZone()),
                InfoRow(stringResource(R.string.gps_latitude), DMSConverter.getFormattedLatitude(latitude, context)),
                InfoRow(stringResource(R.string.gps_longitude), DMSConverter.getFormattedLongitude(longitude, context)),
                InfoRow(stringResource(R.string.gps_address) + ":", address),
            ),
        )
    }
}

@Composable
fun LocationDetailsDialog(rows: List<InfoRow>, accuracy: List<Float>, altitude: List<Float>, metric: Boolean, onDismiss: () -> Unit) {
    InfoDialog(stringResource(R.string.gps_location), onDismiss) {
        InfoSection(stringResource(R.string.gps_location), rows)
        SeriesSection(stringResource(R.string.gps_accuracy), accuracy, metric)
        SeriesSection(stringResource(R.string.gps_altitude), altitude, metric)
    }
}

@Composable
private fun SeriesSection(title: String, values: List<Float>, metric: Boolean) {
    val stats = SeriesStats.of(values)
    val unit = stringResource(if (metric) R.string.meter else R.string.feet)
    fun format(value: Double) = "${round(if (metric) value else value.toFeet(), 2)} $unit"
    Sparkline(values, Modifier.fillMaxWidth().height(SPARKLINE_HEIGHT.dp).padding(horizontal = 10.dp, vertical = 8.dp))
    InfoSection(
        title,
        listOf(
            InfoRow(stringResource(R.string.min), format(stats.min)),
            InfoRow(stringResource(R.string.max), format(stats.max)),
            InfoRow(stringResource(R.string.avg), format(stats.average)),
        ),
    )
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val low = values.min()
        val span = (values.max() - low).takeIf { it > 0f } ?: 1f
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - (value - low) / span * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CoordinateDetailsDialog(latitude: Double, longitude: Double, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sections = remember(latitude, longitude) { CoordinateFormats.sections(context, latitude, longitude) }
    val copied = stringResource(R.string.info_copied)
    InfoDialog(
        stringResource(R.string.gps_coordinates),
        onDismiss,
        action = stringResource(R.string.copy) to {
            context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("Coordinates", CoordinateFormats.text(sections)))
            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        },
    ) {
        sections.forEach { (title, rows) -> InfoSection(title, rows) }
    }
}

private const val SPARKLINE_HEIGHT = 90
