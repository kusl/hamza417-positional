package app.simple.positional.ui.compose

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.simple.positional.BuildConfig
import app.simple.positional.R
import app.simple.positional.constants.LauncherBackground
import app.simple.positional.constants.LocationPins
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val night = LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val (background, colors) = remember(night) {
        if (night) {
            val index = LauncherBackground.vectorBackgroundNight.indices.random()
            LauncherBackground.vectorBackgroundNight[index] to LauncherBackground.vectorNightColors[index]
        } else {
            val index = LauncherBackground.vectorBackground.indices.random()
            LauncherBackground.vectorBackground[index] to LauncherBackground.vectorColors[index]
        }
    }
    val gradient = listOf(Color(colors[0]), Color(colors[1]))
    var touches by remember { mutableIntStateOf(0) }
    var touch by remember { mutableStateOf<Offset?>(null) }
    var last by remember { mutableStateOf(Offset.Zero) }
    val indicator by animateFloatAsState(if (touch != null) 1f else 0f, label = "indicator")
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f) }
    LaunchedEffect(touches, touch == null) {
        if (touch != null) return@LaunchedEffect
        delay(if (touches == 0) START_DELAY else RELEASE_DELAY)
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touch = down.position
                    last = down.position
                    do {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let {
                            touch = it.position
                            last = it.position
                        }
                    } while (event.changes.any { it.pressed })
                    touch = null
                    touches++
                }
            },
    ) {
        LauncherArt(background, Modifier.fillMaxSize(), crop = true)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(
                painterResource(LocationPins.getLocationPin()),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.launcher_app_icon_dimen))
                    .scale(entrance.value)
                    .gradientTint(gradient),
            )
            Text(
                if (BuildConfig.DEBUG) "Positional (Debug)" else stringResource(R.string.app_name),
                fontFamily = FontFamily(Font(R.font.logo)),
                fontSize = textSize(R.dimen.launcher_app_title),
                color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = entrance.value },
            )
        }
        if (indicator > 0f) {
            val radius = INDICATOR.dp
            Canvas(Modifier.fillMaxSize()) {
                val center = last
                val r = radius.toPx() * indicator
                if (r > 0f) drawCircle(Brush.radialGradient(listOf(gradient[1], Color.Transparent), center, r), r, center, alpha = indicator)
            }
        }
    }
}

private fun Modifier.gradientTint(colors: List<Color>): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val brush = Brush.linearGradient(colors)
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcIn)
        }
    }

private const val START_DELAY = 2000L
private const val RELEASE_DELAY = 1000L
private const val INDICATOR = 60
