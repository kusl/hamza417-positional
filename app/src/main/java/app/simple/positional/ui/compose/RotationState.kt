package app.simple.positional.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import app.simple.positional.math.RotationPhysics
import kotlinx.coroutines.flow.first

@Stable
class RotationState(inertia: Float = RotationPhysics.INERTIA, damping: Float = RotationPhysics.DAMPING, magnetic: Float = RotationPhysics.MAGNETIC) {
    private val physics = RotationPhysics(inertia, damping, magnetic)
    private var requests by mutableIntStateOf(0)
    var angle by mutableFloatStateOf(0f)
        private set

    fun configure(inertia: Float, damping: Float, magnetic: Float) = physics.configure(inertia, damping, magnetic)

    fun update(value: Float, animate: Boolean) {
        if (animate) {
            physics.aim(value)
            if (!physics.isSettled) requests++
        } else {
            physics.snap(value)
            angle = value
        }
    }

    suspend fun glideTo(value: Float, durationMillis: Int) {
        val start = angle
        val end = start + RotationPhysics.shortestDelta(start.toDouble(), value.toDouble()).toFloat()
        animate(start, end, animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing)) { current, _ ->
            physics.snap(current)
            angle = current
        }
    }

    suspend fun run() {
        while (true) {
            snapshotFlow { requests }.first { !physics.isSettled }
            do {
                val moving = withFrameMillis { physics.step(it) }
                angle = physics.angle.toFloat()
            } while (moving)
        }
    }
}

fun Modifier.rotation(state: RotationState): Modifier = graphicsLayer { rotationZ = state.angle }

@Composable
fun RotatingImage(@DrawableRes res: Int, state: RotationState, modifier: Modifier = Modifier) {
    LaunchedEffect(state) { state.run() }
    Image(painterResource(res), contentDescription = null, modifier = modifier.rotation(state))
}
