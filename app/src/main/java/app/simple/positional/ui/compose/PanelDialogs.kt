package app.simple.positional.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.sensors.SensorAccuracy

@Composable
fun CalibrationDialog(accelerometer: Int, magnetometer: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.compass_calibrate), contentDescription = null, modifier = Modifier.size(CALIBRATION_IMAGE.dp))
                Text(stringResource(R.string.compass_calibration_notice), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
                AccuracyLine(R.string.accelerometer_accuracy, accelerometer)
                AccuracyLine(R.string.magnetometer_accuracy, magnetometer)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
private fun AccuracyLine(label: Int, accuracy: Int) {
    val value = stringResource(SensorAccuracy.label(accuracy))
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(label)) }
            append(" ")
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun TargetCoordinatesDialog(form: CustomLocationForm, onSearch: () -> Unit, onDismiss: () -> Unit, onSave: (Double, Double) -> Unit) {
    val parsed = CoordinateInput.parse(form.latitude, form.longitude)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let { onSave(it.first, it.second) } }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.target), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconAction(R.drawable.ic_search, stringResource(R.string.target), onSearch)
            }
        },
        text = {
            Row {
                CoordinateField(form.latitude, R.string.specified_hint_latitude, Modifier.weight(1f)) { form.latitude = it }
                Spacer(Modifier.width(10.dp))
                CoordinateField(form.longitude, R.string.specified_hint_longitude, Modifier.weight(1f)) { form.longitude = it }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

private const val CALIBRATION_IMAGE = 160
