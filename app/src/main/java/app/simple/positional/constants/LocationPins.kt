package app.simple.positional.constants

import app.simple.positional.R
import app.simple.positional.preferences.GPSPreferences

object LocationPins {
    private const val FILL_ALPHA = 0x4D000000
    private const val OPAQUE = 0xFF000000.toInt()

    val locationsPins = intArrayOf(
        R.drawable.ic_pin_01,
        R.drawable.ic_pin_02,
        R.drawable.ic_pin_03,
        R.drawable.ic_pin_04,
        R.drawable.ic_pin_05,
        R.drawable.ic_pin_06,
        R.drawable.ic_pin_07,
        R.drawable.ic_pin_08,
        R.drawable.ic_pin_09,
        R.drawable.ic_pin_10,
    )

    private val circleColors = intArrayOf(
        0x1B9CFF,
        0x5F0B95,
        0xFF8859,
        0x4C9ACC,
        0xED765E,
        0xEB2A2E,
        0x943B5E,
        0x8B3B94,
        0xF38630,
        0xFF695B,
    )

    fun pin(skin: Int): Int = locationsPins.getOrElse(skin) { locationsPins[0] }

    fun fillColor(skin: Int): Int = rgb(skin) or FILL_ALPHA

    fun strokeColor(skin: Int): Int = rgb(skin) or OPAQUE

    private fun rgb(skin: Int): Int = circleColors.getOrElse(skin) { circleColors[0] }

    fun getLocationPin(): Int = pin(GPSPreferences.getPinSkin())

    fun getCircleFillColor(): Int = fillColor(GPSPreferences.getPinSkin())

    fun getCircleStrokeColor(): Int = strokeColor(GPSPreferences.getPinSkin())
}
