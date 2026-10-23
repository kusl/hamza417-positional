package app.simple.positional.math

object UnitConverter {
    fun Double.toMiles(): Double {
        return this * 0.621371
    }

    fun Double.toKiloMetersPerHour(): Double {
        return this * 3.6
    }

    fun Float.toKiloMetersPerHour(): Float {
        return this * 3.6F
    }

    fun Float.toKilometers(): Float {
        return this / 1000f
    }

    fun Double.toKilometers(): Double {
        return this / 1000
    }

    fun Float.toMiles(): Float {
        return this / 1609
    }

    fun Double.toMilesPerHour(): Double {
        return this * 0.621371
    }

    fun Float.toMilesPerHour(): Float {
        return this.toKiloMetersPerHour() * 0.621371F
    }

    fun Double.toFeet(): Double {
        return this * 3.28084
    }

    fun Float.toFeet(): Float {
        return this * 3.28084F
    }
}
