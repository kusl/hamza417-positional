package app.simple.positional.util

import android.content.Context
import androidx.annotation.StringRes
import app.simple.positional.R
import app.simple.positional.math.MathExtensions.round

object DistanceText {
    private const val KILOMETER = 1000.0
    private const val MILE = 1609.344
    private const val FEET_PER_METER = 3.28084

    fun parts(meters: Double, metric: Boolean): Pair<Double, Int> = when {
        metric && meters < KILOMETER -> round(meters, 2) to R.string.meter
        metric -> round(meters / KILOMETER, 2) to R.string.kilometer
        meters < MILE -> round(meters * FEET_PER_METER, 2) to R.string.feet
        else -> round(meters / MILE, 2) to R.string.miles
    }

    fun format(context: Context, meters: Double, metric: Boolean): String {
        val (value, @StringRes unit) = parts(meters, metric)
        return "$value ${context.getString(unit)}"
    }
}
