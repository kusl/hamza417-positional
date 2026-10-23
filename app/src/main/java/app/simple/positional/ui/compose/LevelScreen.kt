package app.simple.positional.ui.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import app.simple.positional.R
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.Tilt

private const val MOTION_SCALE = 19.6f
private val levelColor = Color(0xFF37A4CF)
private val tiltedColor = Color(0xFFBF4848)

@Composable
fun LevelScreen(tilt: () -> Tilt, square: Boolean, topInset: Dp, onToggleStyle: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 1.5f else 1f,
        spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessVeryLow),
        label = "levelScale",
    )
    val level by remember { derivedStateOf { tilt().isLevel } }
    val indicatorSize = dimensionResource(R.dimen.level_indicator)
    val indicatorPx = with(LocalDensity.current) { indicatorSize.toPx() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PanelToolbar(stringResource(R.string.level), topInset, divider = false) {
                IconAction(if (square) R.drawable.ic_circle else R.drawable.ic_square, stringResource(R.string.level), onToggleStyle)
            }
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        })
                    },
            ) {
                val width = constraints.maxWidth.toFloat()
                val height = constraints.maxHeight.toFloat()
                val limitX = ((width - indicatorPx) / 2f).coerceAtLeast(0f)
                val limitY = ((height - indicatorPx) / 2f).coerceAtLeast(0f)
                val position = {
                    val t = tilt()
                    Offset(
                        (t.x * width / MOTION_SCALE).coerceIn(-limitX, limitX),
                        (-t.y * height / MOTION_SCALE).coerceIn(-limitY, limitY),
                    )
                }
                Image(
                    painterResource(if (square) R.drawable.level_indicator_square else R.drawable.level_indicator),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { position().round() }
                        .size(indicatorSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                )
                Image(
                    painterResource(if (square) R.drawable.level_dot_square else R.drawable.level_dot),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (level) levelColor else tiltedColor),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { (position() * -0.3f).round() }
                        .size(dimensionResource(R.dimen.level_dot))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                )
                TiltText("X:", { tilt().horizontal }, Modifier.align(Alignment.BottomCenter).padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 24.dp))
                TiltText("Y:", { tilt().vertical }, Modifier.align(Alignment.CenterStart).padding(start = 8.dp).vertical())
            }
        }
    }
}

@Composable
private fun TiltText(label: String, value: () -> Float, modifier: Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(label) }
            append(" ${round(value().toDouble(), 2)}°")
        },
        fontFamily = PositionalFonts.regular,
        fontSize = textSize(R.dimen.level_text_size),
        color = colorResource(R.color.textTertiary),
        modifier = modifier,
    )
}
