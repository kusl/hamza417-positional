package app.simple.positional.preferences

import app.simple.positional.singleton.SharedPreferences

object DirectionPreferences {
    const val directionLatitude = "direction_target_latitude"
    const val directionLongitude = "direction_target_longitude"
    const val directionGimbalLock = "direction_gimbal_lock"
    const val useMapsTarget = "direction_use_maps_target"
    const val directionLabel = "direction_target_label"

    fun setTarget(latitude: Float, longitude: Float, label: String, useMaps: Boolean) {
        SharedPreferences.getSharedPreferences().edit()
                .putFloat(directionLatitude, latitude)
                .putFloat(directionLongitude, longitude)
                .putString(directionLabel, label)
                .putBoolean(useMapsTarget, useMaps)
                .apply()
    }

    fun getTargetCoordinates(): FloatArray {
        return floatArrayOf(
                SharedPreferences.getSharedPreferences().getFloat(directionLatitude, 0f),
                SharedPreferences.getSharedPreferences().getFloat(directionLongitude, 0f)
        )
    }

    fun getTargetLabel(): String? {
        return SharedPreferences.getSharedPreferences().getString(directionLabel, null)
    }

    fun setGimbalLock(value: Boolean) {
        SharedPreferences.getSharedPreferences().edit().putBoolean(directionGimbalLock, value).apply()
    }

    fun isGimbalLock(): Boolean {
        return SharedPreferences.getSharedPreferences().getBoolean(directionGimbalLock, false)
    }

    fun setUseMapsTarget(value: Boolean) {
        SharedPreferences.getSharedPreferences().edit().putBoolean(useMapsTarget, value).apply()
    }

    fun isUsingMapsTarget(): Boolean {
        return SharedPreferences.getSharedPreferences().getBoolean(useMapsTarget, false)
    }
}
