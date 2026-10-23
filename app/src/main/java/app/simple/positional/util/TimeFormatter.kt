package app.simple.positional.util

import android.text.SpannableString
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.util.StringUtils.buildSpannableString
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

object TimeFormatter {
    fun getTimeWithSeconds(zoneDateTime: ZonedDateTime): SpannableString {
        return if (ClockPreferences.getDefaultClockTimeFormat()) {
            buildSpannableString(zoneDateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a").withLocale(LocaleHelper.getAppLocale())))
        } else {
            buildSpannableString(zoneDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss").withLocale(LocaleHelper.getAppLocale())))
        }
    }

    fun Long.formatDate(): String {
        return DateFormat.getDateTimeInstance().format(Date(this))
    }

    fun getDuration(start: Instant, end: Instant): String {
        val duration = Duration.between(start, end).abs()
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart())
    }

    fun getDuration(start: ZonedDateTime?, end: ZonedDateTime?): String {
        val duration = Duration.between(start, end).abs()
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart())
    }
}
