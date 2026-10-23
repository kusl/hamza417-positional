package app.simple.positional.ui.compose

import androidx.annotation.DimenRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.simple.positional.R

@Immutable
data class InfoRow(val label: String, val value: String = "")

@Composable
fun textSize(@DimenRes id: Int): TextUnit = with(LocalDensity.current) { dimensionResource(id).toSp() }

@Composable
fun PanelToolbar(title: String, topInset: Dp, divider: Boolean = true, actions: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset)
                .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            actions()
        }
        if (divider) HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun InfoSection(title: String, rows: List<InfoRow>, graphic: Painter? = null) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp)) {
        if (graphic != null) {
            Image(
                graphic,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.info_graphics_dimension)).align(Alignment.CenterEnd),
            )
        }
        Column(Modifier.fillMaxWidth()) {
            Text(
                title,
                fontFamily = PositionalFonts.light,
                fontSize = textSize(R.dimen.info_text_primary_size),
                color = colorResource(R.color.textPrimary),
            )
            val secondary = colorResource(R.color.textSecondary)
            val size = textSize(R.dimen.info_text_secondary_size)
            rows.forEach { row ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(row.label) }
                        if (row.value.isNotEmpty()) {
                            append(" ")
                            append(row.value)
                        }
                    },
                    fontFamily = PositionalFonts.light,
                    fontSize = size,
                    color = secondary,
                    modifier = Modifier.padding(top = if (row.value.isEmpty()) 8.dp else 0.dp),
                )
            }
        }
    }
}

fun Modifier.vertical(): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(placeable.height, placeable.width) {
        placeable.place((placeable.height - placeable.width) / 2, (placeable.width - placeable.height) / 2)
    }
}.rotate(-90f)
