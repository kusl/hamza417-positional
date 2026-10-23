package app.simple.positional.ui.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.simple.positional.R

object SpeedometerDial {
    private const val RANGE = 300f
    private const val COMPENSATION = 60f
    private val dials = intArrayOf(
        R.drawable.speedometer_dial_normal_range,
        R.drawable.speedometer_dial_medium_range,
        R.drawable.speedometer_dial_high_range,
        R.drawable.speedometer_dial_max_range,
    )

    fun range(degrees: Float): Int = when {
        degrees <= RANGE -> 0
        degrees <= RANGE * 2 -> 1
        degrees <= RANGE * 3 -> 2
        else -> 3
    }

    fun dial(degrees: Float): Int = dials[range(degrees)]

    fun needle(degrees: Float): Float = degrees - range(degrees) * COMPENSATION
}

@Composable
fun Speedometer(speed: Float, modifier: Modifier = Modifier) {
    val degrees = speed * 2
    val angle by animateFloatAsState(
        SpeedometerDial.needle(degrees),
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "needle",
    )
    val size = dimensionResource(R.dimen.clock_image_dimensions)
    Box(modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        Image(painterResource(SpeedometerDial.dial(degrees)), contentDescription = null, modifier = Modifier.size(size))
        Image(
            painterResource(R.drawable.speedometer_needle),
            contentDescription = null,
            modifier = Modifier.size(size).graphicsLayer { rotationZ = angle },
        )
    }
}
