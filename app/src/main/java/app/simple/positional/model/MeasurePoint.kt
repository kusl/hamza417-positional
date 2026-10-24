package app.simple.positional.model

data class MeasurePoint(val latitude: Double, val longitude: Double, val order: Int) {
    fun toDatabase() = "$latitude,$longitude,$order"

    companion object {
        fun fromDatabase(data: String): MeasurePoint {
            val (latitude, longitude, order) = data.split(',')
            return MeasurePoint(latitude.toDouble(), longitude.toDouble(), order.toInt())
        }
    }
}
