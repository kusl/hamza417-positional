package app.simple.positional.widgets

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.SunTimes
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class SunTimeWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SunTimeGlance()
}

class SunTimeGlance : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SharedPreferences.init(context)
        val pin = if (MainPreferences.isCustomCoordinate()) R.drawable.ic_place_custom else LocationPins.getLocationPin()
        val pattern = if (ClockPreferences.getDefaultClockTimeFormat()) {
            DateTimeFormatter.ofPattern("hh:mm a").withLocale(LocaleHelper.getAppLocale())
        } else {
            DateTimeFormatter.ofPattern("HH:mm")
        }
        val times = withContext(Dispatchers.Default) {
            val coordinates = if (MainPreferences.isCustomCoordinate()) MainPreferences.getCoordinates() else MainPreferences.getLastCoordinates()
            val sun = SunTimes.compute()
                .timezone(TimeZone.getDefault().id)
                .on(Instant.now())
                .latitude(coordinates[0].toDouble())
                .longitude(coordinates[1].toDouble())
                .execute()
            listOf(
                R.drawable.ic_widget_sunrise to sun.rise,
                R.drawable.ic_widget_sun_noon to sun.noon,
                R.drawable.ic_widget_sun_set to sun.set,
                R.drawable.ic_widget_sun_nadir to sun.nadir,
            ).map { (icon, time) -> icon to (time?.let(pattern::format) ?: "--") }
        }
        val updated = context.getString(R.string.last_updated) + " • " + pattern.format(ZonedDateTime.now())
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.bg_widget)).padding(10.dp)) {
                Row(GlanceModifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(pin), contentDescription = null, modifier = GlanceModifier.size(20.dp))
                    Text(
                        updated,
                        style = TextStyle(color = ColorProvider(R.color.textTertiary)),
                        modifier = GlanceModifier.defaultWeight().padding(horizontal = 8.dp),
                    )
                    Text(context.getString(R.string.app_name), style = TextStyle(color = ColorProvider(R.color.dividerColor), fontSize = 12.sp))
                }
                Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    times.forEach { (icon, time) ->
                        Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(36.dp))
                            Text(time, style = TextStyle(color = ColorProvider(R.color.textTertiary), fontSize = 12.sp))
                        }
                    }
                }
            }
        }
    }
}
