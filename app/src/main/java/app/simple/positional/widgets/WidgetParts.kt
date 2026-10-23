package app.simple.positional.widgets

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.LocaleHelper
import org.shredzone.commons.suncalc.SunTimes
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

internal object WidgetSky {
    fun coordinates(): Pair<Double, Double> {
        val c = if (MainPreferences.isCustomCoordinate()) MainPreferences.getCoordinates() else MainPreferences.getLastCoordinates()
        return c[0].toDouble() to c[1].toDouble()
    }

    @DrawableRes
    fun pin(): Int = if (MainPreferences.isCustomCoordinate()) R.drawable.ic_place_custom else LocationPins.getLocationPin()

    fun pattern(): DateTimeFormatter =
        if (ClockPreferences.getDefaultClockTimeFormat()) DateTimeFormatter.ofPattern("hh:mm a").withLocale(LocaleHelper.getAppLocale())
        else DateTimeFormatter.ofPattern("HH:mm")

    fun updated(context: Context, pattern: DateTimeFormatter) = context.getString(R.string.last_updated) + " • " + pattern.format(ZonedDateTime.now())

    fun sunTimes(pattern: DateTimeFormatter): List<Pair<Int, String>> {
        val (latitude, longitude) = coordinates()
        val sun = SunTimes.compute().timezone(TimeZone.getDefault().id).on(Instant.now()).latitude(latitude).longitude(longitude).execute()
        return listOf(
            R.drawable.ic_widget_sunrise to sun.rise,
            R.drawable.ic_widget_sun_noon to sun.noon,
            R.drawable.ic_widget_sun_set to sun.set,
            R.drawable.ic_widget_sun_nadir to sun.nadir,
        ).map { (icon, time) -> icon to (time?.let(pattern::format) ?: "--") }
    }
}

@Composable
internal fun WidgetHeader(@DrawableRes icon: Int, title: String, caption: String) {
    Row(GlanceModifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(20.dp))
        Text(
            title,
            style = TextStyle(color = ColorProvider(R.color.textTertiary), fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 10.dp),
        )
        Text(caption, style = TextStyle(color = ColorProvider(R.color.dividerColor), fontSize = 11.sp))
    }
}

@Composable
internal fun SunTimesRow(times: List<Pair<Int, String>>, modifier: GlanceModifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        times.forEach { (icon, time) ->
            Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(36.dp))
                Text(time, style = TextStyle(color = ColorProvider(R.color.textTertiary), fontSize = 12.sp), modifier = GlanceModifier.padding(top = 6.dp))
            }
        }
    }
}
