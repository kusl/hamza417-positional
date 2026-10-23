package app.simple.positional.widgets

import android.content.Context
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.simple.positional.R
import app.simple.positional.math.MathExtensions
import app.simple.positional.util.MoonAngle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.MoonIllumination
import java.time.Instant
import java.util.TimeZone

class MoonPhaseWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseGlance()
}

class MoonPhaseGlance : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val phase = withContext(Dispatchers.Default) {
            MoonIllumination.compute().timezone(TimeZone.getDefault().id).on(Instant.now()).execute().phase
        }
        val art = MoonAngle.getMoonPhaseGraphics(MathExtensions.round(phase, 2))
        val label = MoonAngle.getMoonPhase(context, phase)
        provideContent {
            Column(
                GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.bg_widget)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(art), contentDescription = label, modifier = GlanceModifier.size(120.dp))
                Text(label, style = TextStyle(color = ColorProvider(R.color.textSecondary)))
            }
        }
    }
}
