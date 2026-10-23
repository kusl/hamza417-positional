package app.simple.positional.math

data class SeriesStats(val min: Double, val max: Double, val average: Double) {
    companion object {
        fun of(values: List<Float>): SeriesStats = if (values.isEmpty()) {
            SeriesStats(0.0, 0.0, 0.0)
        } else {
            SeriesStats(values.min().toDouble(), values.max().toDouble(), values.average())
        }
    }
}
