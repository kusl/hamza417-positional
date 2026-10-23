package app.simple.positional.util

import app.simple.positional.BuildConfig
import app.simple.positional.preferences.MainPreferences

object MapSource {
    const val AUTO = 0
    const val GOOGLE = 1
    const val OSM = 2

    val choices = listOf(AUTO, GOOGLE, OSM)

    fun useOsm(choice: Int, hasGoogleKey: Boolean) = choice == OSM || (choice != GOOGLE && !hasGoogleKey)

    fun isOsm() = useOsm(MainPreferences.getMapSource(), BuildConfig.HAS_MAPS_KEY)
}
