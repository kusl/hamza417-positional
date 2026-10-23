package app.simple.positional.util

import android.content.Context
import android.content.res.Configuration
import android.os.PowerManager
import androidx.core.content.getSystemService
import app.simple.positional.preferences.MainPreferences
import java.util.Calendar

object NightMode {
    const val FOLLOW_SYSTEM = -1
    const val NO = 1
    const val YES = 2
    const val AUTO_BATTERY = 3
    const val AUTO_TIME = 4
    private const val DAY_START = 6
    private const val DAY_END = 18

    fun resolve(mode: Int, byTime: Boolean, systemNight: Boolean, powerSave: Boolean, hour: Int): Boolean = when {
        byTime || mode == AUTO_TIME -> hour !in DAY_START until DAY_END
        mode == YES -> true
        mode == NO -> false
        mode == AUTO_BATTERY -> powerSave || systemNight
        else -> systemNight
    }

    fun isNight(context: Context): Boolean = resolve(
        MainPreferences.getTheme(),
        MainPreferences.isDayNightOn(),
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
        context.getSystemService<PowerManager>()?.isPowerSaveMode == true,
        Calendar.getInstance()[Calendar.HOUR_OF_DAY],
    )
}
