package app.simple.positional.math

data class Latency(val millis: Long) {
    val inSeconds: Boolean get() = millis > MAX_MILLIS
    val value: Number get() = if (inSeconds) MathExtensions.round(millis / 1000.0, 3) else millis
    val isLate: Boolean get() = millis > LATE_MILLIS

    private companion object {
        const val MAX_MILLIS = 999L
        const val LATE_MILLIS = 5000L
    }
}
