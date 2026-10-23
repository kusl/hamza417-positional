package app.simple.positional.util

import androidx.annotation.StringRes
import app.simple.positional.R
import org.shredzone.commons.suncalc.SunTimes
import java.time.Instant
import java.time.ZonedDateTime

enum class TwilightPhase(@StringRes val label: Int, val color: Long) {
    ASTRONOMICAL(R.string.twilight_astronomical_dusk, 0xFF1A237E),
    NAUTICAL(R.string.twilight_nautical_dusk, 0xFF0D47A1),
    BLUE_HOUR(R.string.twilight_blue_hour, 0xFF1565C0),
    CIVIL(R.string.twilight_civil_dusk, 0xFF1E88E5),
    VISUAL(R.string.twilight_visual, 0xFF42A5F5),
    HORIZON(R.string.twilight_horizon, 0xFF64B5F6),
    VISUAL_LOWER(R.string.twilight_visual_lower, 0xFF90CAF9),
    GOLDEN_HOUR(R.string.twilight_golden_hour, 0xFFFFCC80),
}

data class TwilightSpan(val phase: TwilightPhase, val start: ZonedDateTime?, val end: ZonedDateTime?, val range: Boolean)

data class TwilightDay(val morning: List<TwilightSpan>, val evening: List<TwilightSpan>)

object Twilight {
    private const val GOLDEN_HOUR_EDGE = -4.0
    private val angles = listOf(
        SunTimes.Twilight.ASTRONOMICAL, SunTimes.Twilight.NAUTICAL, SunTimes.Twilight.CIVIL,
        SunTimes.Twilight.NIGHT_HOUR, SunTimes.Twilight.BLUE_HOUR, SunTimes.Twilight.HORIZON,
        SunTimes.Twilight.VISUAL, SunTimes.Twilight.VISUAL_LOWER, SunTimes.Twilight.GOLDEN_HOUR,
    )

    fun compute(latitude: Double, longitude: Double, timezone: String, at: Instant = Instant.now(), altitude: Double = 0.0): TwilightDay {
        fun base() = SunTimes.compute().timezone(timezone).on(at).latitude(latitude).longitude(longitude).elevation(altitude)
        val times = angles.associateWith { base().twilight(it).execute() }
        val golden = base().twilight(GOLDEN_HOUR_EDGE).execute()
        fun rise(t: SunTimes.Twilight) = times.getValue(t).rise
        fun set(t: SunTimes.Twilight) = times.getValue(t).set
        fun span(phase: TwilightPhase, start: ZonedDateTime?, end: ZonedDateTime?) = TwilightSpan(phase, start, end, true)
        fun moment(phase: TwilightPhase, time: ZonedDateTime?) = TwilightSpan(phase, time, time, false)
        return TwilightDay(
            morning = listOf(
                span(TwilightPhase.ASTRONOMICAL, rise(SunTimes.Twilight.ASTRONOMICAL), rise(SunTimes.Twilight.NAUTICAL)),
                span(TwilightPhase.NAUTICAL, rise(SunTimes.Twilight.NAUTICAL), rise(SunTimes.Twilight.CIVIL)),
                span(TwilightPhase.BLUE_HOUR, rise(SunTimes.Twilight.NIGHT_HOUR), rise(SunTimes.Twilight.BLUE_HOUR)),
                span(TwilightPhase.CIVIL, rise(SunTimes.Twilight.CIVIL), rise(SunTimes.Twilight.HORIZON)),
                moment(TwilightPhase.VISUAL, rise(SunTimes.Twilight.VISUAL)),
                moment(TwilightPhase.HORIZON, rise(SunTimes.Twilight.HORIZON)),
                moment(TwilightPhase.VISUAL_LOWER, rise(SunTimes.Twilight.VISUAL_LOWER)),
                span(TwilightPhase.GOLDEN_HOUR, golden.rise, rise(SunTimes.Twilight.GOLDEN_HOUR)),
            ),
            evening = listOf(
                span(TwilightPhase.GOLDEN_HOUR, set(SunTimes.Twilight.GOLDEN_HOUR), golden.set),
                moment(TwilightPhase.VISUAL_LOWER, set(SunTimes.Twilight.VISUAL_LOWER)),
                moment(TwilightPhase.HORIZON, set(SunTimes.Twilight.HORIZON)),
                moment(TwilightPhase.VISUAL, set(SunTimes.Twilight.VISUAL)),
                span(TwilightPhase.CIVIL, set(SunTimes.Twilight.HORIZON), set(SunTimes.Twilight.CIVIL)),
                span(TwilightPhase.BLUE_HOUR, set(SunTimes.Twilight.BLUE_HOUR), set(SunTimes.Twilight.NIGHT_HOUR)),
                span(TwilightPhase.NAUTICAL, set(SunTimes.Twilight.CIVIL), set(SunTimes.Twilight.NAUTICAL)),
                span(TwilightPhase.ASTRONOMICAL, set(SunTimes.Twilight.NAUTICAL), set(SunTimes.Twilight.ASTRONOMICAL)),
            ),
        )
    }
}
