package app.simple.positional.ui.panels

import android.content.Context
import androidx.annotation.StringRes
import app.simple.positional.R
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.UnitConverter.toMiles
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.ClockTime
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.SkyReport
import app.simple.positional.util.Direction
import app.simple.positional.util.LocaleHelper
import app.simple.positional.util.MoonAngle
import app.simple.positional.util.MoonTimeFormatter.formatMoonDate
import app.simple.positional.util.Twilight
import app.simple.positional.util.TwilightSpan
import app.simple.positional.util.Ordinal.toOrdinal
import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunPosition
import org.shredzone.commons.suncalc.SunTimes
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

class ClockReport(private val context: Context) {
    private fun text(@StringRes id: Int) = context.getString(id)

    private fun format(time: ZonedDateTime, pattern: String) =
        time.format(DateTimeFormatter.ofPattern(pattern).withLocale(LocaleHelper.getAppLocale()))

    fun time(zone: ZoneId): ClockTime {
        val local = ZonedDateTime.now(zone)
        val utc = local.withZoneSameInstant(ZoneOffset.UTC)
        return ClockTime(
            local = listOf(
                InfoRow(text(R.string.local_timezone), zone.id),
                InfoRow(text(R.string.local_time_24hr), format(local, "HH:mm:ss")),
                InfoRow(text(R.string.local_time_12hr), format(local, "hh:mm:ss a")),
                InfoRow(text(R.string.local_date), format(local, "dd MMMM, yyyy")),
                InfoRow(text(R.string.local_day), format(local, "EEEE")),
                InfoRow(text(R.string.local_day_of_year), local.dayOfYear.toOrdinal()),
                InfoRow(text(R.string.local_week_of_year), local.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toOrdinal()),
            ),
            utc = listOf(
                InfoRow(text(R.string.utc_local_time_offset), format(local, "XXX").replace("Z", "+00:00")),
                InfoRow(text(R.string.utc_current_time), format(utc, if (ClockPreferences.getDefaultClockTimeFormat()) "hh:mm:ss a" else "HH:mm:ss")),
                InfoRow(text(R.string.local_date), format(utc, "dd MMMM, yyyy")),
            ),
        )
    }

    fun sky(latitude: Double, longitude: Double, altitude: Double, zone: ZoneId): SkyReport {
        val now = Instant.now()
        val timezone = zone.id
        val pattern = DateTimeFormatter.ofPattern(timePattern()).withLocale(LocaleHelper.getAppLocale())
        val metric = MainPreferences.isMetric()
        val notAvailable = text(R.string.not_available)

        fun time(value: ZonedDateTime?) = value?.format(pattern) ?: notAvailable
        fun range(from: ZonedDateTime?, to: ZonedDateTime?) = "${time(from)} ~ ${time(to)}"
        fun degrees(value: Double) = "${round(value, 2)}°"
        fun direction(azimuth: Double) = "${degrees(azimuth)} ${Direction.getDirectionCodeFromAzimuth(context, azimuth)}"
        fun distance(kilometers: Double) =
            if (metric) "${"%.3E".format(kilometers)} ${text(R.string.kilometer)}" else "${"%.3E".format(kilometers.toMiles())} ${text(R.string.miles)}"

        val sun = SunTimes.compute().timezone(timezone).on(now).latitude(latitude).longitude(longitude).elevation(altitude).execute()
        val day = Twilight.compute(latitude, longitude, timezone, now, altitude)
        fun rows(spans: List<TwilightSpan>) = spans.map { InfoRow(text(it.phase.label), if (it.range) range(it.start, it.end) else time(it.start)) }

        val sunPosition = SunPosition.compute().timezone(timezone).on(now).at(latitude, longitude).elevation(altitude).execute()
        val moonPosition = MoonPosition.compute().timezone(timezone).on(now).latitude(latitude).longitude(longitude).elevation(altitude).execute()
        val moonTimes = MoonTimes.compute().on(now).timezone(timezone).latitude(latitude).longitude(longitude).elevation(altitude).execute()
        val illumination = MoonIllumination.compute().timezone(timezone).on(now).execute()
        fun moonDate(phase: MoonPhase.Phase) = formatMoonDate(MoonPhase.compute().timezone(timezone).on(now).phase(phase).execute().time)

        return SkyReport(
            sunPosition = listOf(
                InfoRow(text(R.string.sun_azimuth), direction(sunPosition.azimuth)),
                InfoRow(text(R.string.sun_altitude), degrees(sunPosition.trueAltitude)),
                InfoRow(text(R.string.sun_distance), distance(sunPosition.distance)),
            ),
            sunTime = listOf(
                InfoRow(text(R.string.sun_sunrise), time(sun.rise)),
                InfoRow(text(R.string.sun_sunset), time(sun.set)),
                InfoRow(text(R.string.sun_noon), time(sun.noon)),
                InfoRow(text(R.string.sun_nadir), time(sun.nadir)),
            ),
            twilight = listOf(InfoRow(text(R.string.twilight_morning))) + rows(day.morning) +
                InfoRow(text(R.string.twilight_evening)) + rows(day.evening),
            moonPosition = listOf(
                InfoRow(text(R.string.moon_azimuth), direction(moonPosition.azimuth)),
                InfoRow(text(R.string.moon_altitude), degrees(moonPosition.altitude)),
                InfoRow(text(R.string.true_altitude), degrees(moonPosition.trueAltitude)),
                InfoRow(text(R.string.moon_distance), distance(moonPosition.distance)),
                InfoRow(text(R.string.moon_parallactic_angle), degrees(moonPosition.parallacticAngle)),
            ),
            moonTime = listOf(
                InfoRow(text(R.string.moon_moonrise), time(moonTimes.rise)),
                InfoRow(text(R.string.moon_moonset), time(moonTimes.set)),
            ),
            moonIllumination = listOf(
                InfoRow(text(R.string.moon_fraction), round(illumination.fraction, 2).toString()),
                InfoRow(text(R.string.moon_angle), degrees(illumination.angle)),
                InfoRow(text(R.string.moon_angle_state), text(if (illumination.angle < 0) R.string.waxing else R.string.waning)),
                InfoRow(text(R.string.moon_phase), MoonAngle.getMoonPhase(context, illumination.phase)),
                InfoRow(text(R.string.moon_phase_angle), degrees(illumination.phase)),
                InfoRow(text(R.string.elongation), distance(illumination.elongation)),
                InfoRow(text(R.string.radius), distance(illumination.radius)),
                InfoRow(text(R.string.crescent_width), distance(illumination.crescentWidth)),
            ),
            moonDates = listOf(
                InfoRow(text(R.string.moon_full_moon), moonDate(MoonPhase.Phase.FULL_MOON)),
                InfoRow(text(R.string.moon_new_moon), moonDate(MoonPhase.Phase.NEW_MOON)),
                InfoRow(text(R.string.moon_first_quarter), moonDate(MoonPhase.Phase.FIRST_QUARTER)),
                InfoRow(text(R.string.moon_last_quarter), moonDate(MoonPhase.Phase.LAST_QUARTER)),
            ),
            sunAzimuth = sunPosition.azimuth.toFloat(),
            moonAzimuth = moonPosition.azimuth.toFloat(),
            moonGraphic = MoonAngle.getMoonPhaseGraphics(round(illumination.phase, 2)),
        )
    }

    private fun timePattern(): String {
        val seconds = ClockPreferences.isUsingSecondsPrecision()
        return when {
            ClockPreferences.getDefaultClockTimeFormat() -> if (seconds) "hh:mm:ss a" else "hh:mm a"
            seconds -> "HH:mm:ss"
            else -> "HH:mm"
        }
    }
}
