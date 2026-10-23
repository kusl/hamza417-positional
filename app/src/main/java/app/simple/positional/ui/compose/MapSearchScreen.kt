package app.simple.positional.ui.compose

import android.location.Address
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.simple.positional.R

object MapSearchQuery {
    const val DELAY = 800L
    const val MAX_RESULTS = 10
    private const val MIN_LENGTH = 3

    fun normalize(text: String): String? = text.trim().takeIf { it.length >= MIN_LENGTH }
}

interface MapSearchActions {
    fun addressChanged(text: String)
    fun pick(address: Address)
    fun cancel()
    fun confirm()
}

@Composable
fun MapSearchScreen(form: CustomLocationForm, suggestions: List<Address>, map: View, actions: MapSearchActions) {
    val iconTint = colorResource(R.color.iconColor)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.specified_hint_header), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = actions::cancel) {
                    Icon(painterResource(R.drawable.ic_clear), contentDescription = stringResource(android.R.string.cancel), tint = iconTint)
                }
                IconButton(onClick = actions::confirm) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = stringResource(android.R.string.ok), tint = iconTint)
                }
            }
            OutlinedTextField(
                value = form.address,
                onValueChange = actions::addressChanged,
                label = { Text(stringResource(R.string.gps_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            suggestions.forEach { address ->
                Text(
                    address.getAddressLine(0).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { actions.pick(address) }.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                CoordinateField(form.latitude, R.string.specified_hint_latitude, Modifier.weight(1f)) { form.latitude = it }
                Spacer(Modifier.width(10.dp))
                CoordinateField(form.longitude, R.string.specified_hint_longitude, Modifier.weight(1f)) { form.longitude = it }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp), thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
                Icon(
                    painterResource(R.drawable.ic_crosshair),
                    contentDescription = null,
                    tint = colorResource(R.color.iconRegular),
                    modifier = Modifier.size(30.dp).align(Alignment.Center),
                )
            }
        }
    }
}
