package app.simple.positional.util

import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Date

object TimeFormatter {
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
