package app.simple.positional.util

import android.content.Context
import app.simple.positional.R

object Direction {
    private val codes = intArrayOf(
        R.string.north_N, R.string.north_east_NE, R.string.east_E, R.string.south_east_SE,
        R.string.south_S, R.string.south_west_SW, R.string.west_W, R.string.north_west_NW
    )

    private val names = intArrayOf(
        R.string.north, R.string.north_east, R.string.east, R.string.south_east,
        R.string.south, R.string.south_west, R.string.west, R.string.north_west
    )

    fun sector(azimuth: Double): Int = when {
        azimuth.isNaN() -> -1
        azimuth <= 10.0 || azimuth > 349.0 -> 0
        azimuth <= 80.0 -> 1
        azimuth <= 100.0 -> 2
        azimuth <= 170.0 -> 3
        azimuth <= 190.0 -> 4
        azimuth <= 260.0 -> 5
        azimuth <= 280.0 -> 6
        else -> 7
    }

    fun getDirectionCodeFromAzimuth(context: Context, azimuth: Double): String {
        return context.getString(codes.getOrElse(sector(azimuth)) { R.string.not_available })
    }

    fun getDirectionNameFromAzimuth(context: Context, azimuth: Double): String {
        return context.getString(names.getOrElse(sector(azimuth)) { R.string.not_available })
    }
}
