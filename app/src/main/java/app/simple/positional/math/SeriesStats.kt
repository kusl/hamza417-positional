package app.simple.positional.math

data class SeriesStats(val min: Double, val max: Double, val average: Double) {
    companion object {
        const val LIMIT = 45

        fun of(values: List<Float>): SeriesStats = if (values.isEmpty()) {
            SeriesStats(0.0, 0.0, 0.0)
        } else {
            SeriesStats(values.min().toDouble(), values.max().toDouble(), values.average())
        }

        fun append(values: List<Float>, value: Float, limit: Int = LIMIT): List<Float> =
            if (values.lastOrNull() == value) values else (values + value).takeLast(limit)
    }
}
