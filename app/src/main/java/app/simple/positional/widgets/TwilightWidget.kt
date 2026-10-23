package app.simple.positional.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.simple.positional.R
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.util.Twilight
import app.simple.positional.util.TwilightSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class TwilightWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TwilightGlance()
}

private data class TwilightLine(val color: Long, val label: String, val value: String)

class TwilightGlance : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SharedPreferences.init(context)
        val pattern = WidgetSky.pattern()
        val notAvailable = context.getString(R.string.not_available)
        fun time(value: ZonedDateTime?) = value?.format(pattern) ?: notAvailable
        fun lines(spans: List<TwilightSpan>) = spans.map {
            TwilightLine(it.phase.color, context.getString(it.phase.label), if (it.range) "${time(it.start)} ~ ${time(it.end)}" else time(it.start))
        }
        val day = withContext(Dispatchers.Default) {
            val (latitude, longitude) = WidgetSky.coordinates()
            Twilight.compute(latitude, longitude, ClockPreferences.getTimeZone())
        }
        val morning = lines(day.morning)
        val evening = lines(day.evening)
        val title = context.getString(R.string.twilight)
        val morningTitle = context.getString(R.string.twilight_morning)
        val eveningTitle = context.getString(R.string.twilight_evening)
        val updated = WidgetSky.updated(context, pattern)
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.bg_widget)).padding(10.dp)) {
                WidgetHeader(R.drawable.ic_twilight, title, updated)
                LazyColumn(GlanceModifier.fillMaxSize().padding(horizontal = 10.dp)) {
                    item { Heading(morningTitle) }
                    items(morning) { Line(it) }
                    item { Heading(eveningTitle) }
                    items(evening) { Line(it) }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = TextStyle(color = ColorProvider(R.color.textPrimary), fontWeight = FontWeight.Bold, fontSize = 15.sp),
        modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun Line(line: TwilightLine) {
    val style = TextStyle(color = ColorProvider(R.color.textSecondary), fontSize = 12.sp)
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("⬤ ", style = style.copy(color = ColorProvider(Color(line.color))))
        Text("${line.label} ", style = style.copy(fontWeight = FontWeight.Bold))
        Text(line.value, style = style, modifier = GlanceModifier.defaultWeight())
    }
}
