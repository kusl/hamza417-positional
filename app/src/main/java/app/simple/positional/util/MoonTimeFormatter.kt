package app.simple.positional.util

import app.simple.positional.preferences.ClockPreferences
import java.text.ParseException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object MoonTimeFormatter {
    fun formatMoonDate(zonedDateTime: ZonedDateTime): String {
        return try {
            zonedDateTime.format(DateTimeFormatter.ofPattern(getMoonTimePattern()).withLocale(LocaleHelper.getAppLocale()))
        } catch (e: ParseException) {
            "N/A"
        }
    }

    private fun getMoonTimePattern(): String {
        return if (ClockPreferences.getDefaultClockTimeFormat()) {
            if (ClockPreferences.isUsingSecondsPrecision()) {
                "dd MMM, yyyy, hh:mm:ss a"
            } else {
                "dd MMM, yyyy, hh:mm a"
            }
        } else {
            if (ClockPreferences.isUsingSecondsPrecision()) {
                "dd MMM, yyyy, HH:mm:ss"
            } else {
                "dd MMM, yyyy, HH:mm"
            }
        }
    }
}
