package app.simple.positional.widgets

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import app.simple.positional.R
import app.simple.positional.singleton.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SunTimeWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SunTimeGlance()
}

class SunTimeGlance : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SharedPreferences.init(context)
        val pattern = WidgetSky.pattern()
        val times = withContext(Dispatchers.Default) { WidgetSky.sunTimes(pattern) }
        val pin = WidgetSky.pin()
        val title = context.getString(R.string.app_name)
        val updated = WidgetSky.updated(context, pattern)
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.bg_widget)).padding(10.dp)) {
                WidgetHeader(pin, title, updated)
                SunTimesRow(times, GlanceModifier.fillMaxSize())
            }
        }
    }
}
