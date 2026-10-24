package app.simple.positional.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import app.simple.positional.R
import app.simple.positional.constants.LauncherBackground
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.util.ArtRaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class SunTimeWidgetArt : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SunTimeArtGlance()
}

class SunTimeArtGlance : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SharedPreferences.init(context)
        val pattern = WidgetSky.pattern()
        val times = withContext(Dispatchers.Default) { WidgetSky.sunTimes(pattern) }
        val pin = WidgetSky.pin()
        val title = context.getString(R.string.app_name)
        val updated = WidgetSky.updated(context, pattern)
        val art = ArtRaster.render(context, LauncherBackground.vectorBackground.random(), artWidth(context, id))
        provideContent {
            Box(
                GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.bg_widget)).cornerRadius(20.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Image(ImageProvider(art), contentDescription = null, modifier = GlanceModifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                Column(GlanceModifier.fillMaxSize().padding(10.dp)) {
                    WidgetHeader(pin, title, updated)
                    SunTimesRow(times, GlanceModifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
        }
    }

    private fun artWidth(context: Context, id: GlanceId): Int {
        val metrics = context.resources.displayMetrics
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val dp = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        return ((if (dp > 0) dp else FALLBACK_WIDTH) * metrics.density).roundToInt().coerceAtMost(metrics.widthPixels)
    }

    private companion object {
        const val FALLBACK_WIDTH = 320
    }
}
