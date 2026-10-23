package app.simple.positional.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simple.positional.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val orbitColor = Color(0x642C3E50)
private val sunGlow = Color(0x50F5B041)
private val earthGlow = Color(0x702980B9)

@Composable
fun SunPositionDiagram(sunAzimuth: Float, moonAzimuth: Float, @DrawableRes moonGraphic: Int, modifier: Modifier = Modifier) {
    val sun = painterResource(R.drawable.ic_sunrise)
    val moon = painterResource(moonGraphic)
    val earth = painterResource(R.drawable.ic_earth)
    val measurer = rememberTextMeasurer()
    val textColor = colorResource(R.color.textPrimary).copy(alpha = 0.4f)
    val style = TextStyle(color = textColor, fontSize = 11.sp, fontFamily = PositionalFonts.regular)
    val labels = listOf(
        stringResource(R.string.north_N) to Offset(0f, -1f),
        stringResource(R.string.east_E) to Offset(1f, 0f),
        stringResource(R.string.south_S) to Offset(0f, 1f),
        stringResource(R.string.west_W) to Offset(-1f, 0f),
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        val moonRadius = radius * 0.25f
        val dashed = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f)))
        drawCircle(textColor, radius, center, style = dashed)
        drawCircle(textColor, moonRadius, center, style = dashed)
        drawLine(textColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 2f, pathEffect = dashed.pathEffect)
        val inset = 20.dp.toPx()
        labels.forEach { (label, direction) ->
            val layout = measurer.measure(label, style)
            val anchor = center + direction * (radius - inset)
            drawText(layout, topLeft = anchor - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        val sunPoint = orbit(center, radius, sunAzimuth)
        val moonPoint = orbit(center, moonRadius, moonAzimuth)
        drawLine(orbitColor, center, sunPoint, strokeWidth = 1f)
        drawLine(orbitColor, center, moonPoint, strokeWidth = 1f)
        glow(sunGlow, sunPoint, 28.dp.toPx())
        glow(earthGlow, center, 24.dp.toPx())
        icon(sun, sunPoint, 14.dp.toPx())
        icon(moon, moonPoint, 5.dp.toPx())
        icon(earth, center, 8.dp.toPx())
    }
}

private fun orbit(center: Offset, radius: Float, azimuth: Float): Offset {
    val radians = Math.toRadians(azimuth.toDouble())
    return Offset(center.x + radius * sin(radians).toFloat(), center.y - radius * cos(radians).toFloat())
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float) {
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
}

private fun DrawScope.icon(painter: Painter, center: Offset, half: Float) {
    translate(center.x - half, center.y - half) {
        with(painter) { draw(Size(half * 2, half * 2)) }
    }
}
