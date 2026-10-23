package app.simple.positional.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import app.simple.positional.R
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.util.HtmlHelper
import app.simple.positional.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.SunTimes
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TwilightWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        context?.updateWidget()
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context?.updateWidget()
    }

    private fun Context.updateWidget() {
        CoroutineScope(Dispatchers.Default).launch {
            SharedPreferences.init(this@updateWidget)
            val preferences = SharedPreferences.getSharedPreferences()
            val custom = MainPreferences.isCustomCoordinate()
            val latitude = preferences.getFloat(if (custom) MainPreferences.latitude else MainPreferences.lastLatitude, 0f).toDouble()
            val longitude = preferences.getFloat(if (custom) MainPreferences.longitude else MainPreferences.lastLongitude, 0f).toDouble()

            val pattern: DateTimeFormatter = if (ClockPreferences.getDefaultClockTimeFormat()) {
                DateTimeFormatter.ofPattern("hh:mm a").withLocale(LocaleHelper.getAppLocale())
            } else {
                DateTimeFormatter.ofPattern("HH:mm")
            }
            val notAvailable = getString(R.string.not_available)
            val now = Instant.now()
            val timezone = ClockPreferences.getTimeZone()

            fun base() = SunTimes.compute().timezone(timezone).on(now).latitude(latitude).longitude(longitude)
            val times = TWILIGHTS.associateWith { base().twilight(it).execute() }
            val golden = base().twilight(GOLDEN_HOUR_EDGE).execute()
            fun time(value: ZonedDateTime?) = value?.format(pattern) ?: notAvailable
            fun rise(twilight: SunTimes.Twilight) = times.getValue(twilight).rise
            fun set(twilight: SunTimes.Twilight) = times.getValue(twilight).set
            fun row(color: String, label: Int, value: ZonedDateTime?) =
                "<b><font color='$color'>$DOT</font> ${getString(label)}</b> ${time(value)}<br>"
            fun row(color: String, label: Int, from: ZonedDateTime?, to: ZonedDateTime?) =
                "<b><font color='$color'>$DOT</font> ${getString(label)}</b> ${time(from)} $ARROW ${time(to)}<br>"

            val twilightData = HtmlHelper.fromHtml(
                "<h3><b>${getString(R.string.twilight_morning)}</b></h3>" +
                row("#1a237e", R.string.twilight_astronomical_dusk, rise(SunTimes.Twilight.ASTRONOMICAL), rise(SunTimes.Twilight.NAUTICAL)) +
                row("#0d47a1", R.string.twilight_nautical_dusk, rise(SunTimes.Twilight.NAUTICAL), rise(SunTimes.Twilight.CIVIL)) +
                row("#1565c0", R.string.twilight_blue_hour, rise(SunTimes.Twilight.NIGHT_HOUR), rise(SunTimes.Twilight.BLUE_HOUR)) +
                row("#1e88e5", R.string.twilight_civil_dusk, rise(SunTimes.Twilight.CIVIL), rise(SunTimes.Twilight.HORIZON)) +
                row("#42a5f5", R.string.twilight_visual, rise(SunTimes.Twilight.VISUAL)) +
                row("#64b5f6", R.string.twilight_horizon, rise(SunTimes.Twilight.HORIZON)) +
                row("#90caf9", R.string.twilight_visual_lower, rise(SunTimes.Twilight.VISUAL_LOWER)) +
                row("#ffcc80", R.string.twilight_golden_hour, golden.rise, rise(SunTimes.Twilight.GOLDEN_HOUR)) +
                "<br><h3>${getString(R.string.twilight_evening)}</h3>" +
                row("#ffcc80", R.string.twilight_golden_hour, set(SunTimes.Twilight.GOLDEN_HOUR), golden.set) +
                row("#90caf9", R.string.twilight_visual_lower, set(SunTimes.Twilight.VISUAL_LOWER)) +
                row("#64b5f6", R.string.twilight_horizon, set(SunTimes.Twilight.HORIZON)) +
                row("#42a5f5", R.string.twilight_visual, set(SunTimes.Twilight.VISUAL)) +
                row("#1e88e5", R.string.twilight_civil_dusk, set(SunTimes.Twilight.HORIZON), set(SunTimes.Twilight.CIVIL)) +
                row("#1565c0", R.string.twilight_blue_hour, set(SunTimes.Twilight.BLUE_HOUR), set(SunTimes.Twilight.NIGHT_HOUR)) +
                row("#0d47a1", R.string.twilight_nautical_dusk, set(SunTimes.Twilight.CIVIL), set(SunTimes.Twilight.NAUTICAL)) +
                row("#1a237e", R.string.twilight_astronomical_dusk, set(SunTimes.Twilight.NAUTICAL), set(SunTimes.Twilight.ASTRONOMICAL))
            )
            val updateTime = getString(R.string.last_updated) + " • " + pattern.format(ZonedDateTime.now())

            withContext(Dispatchers.Main) {
                val views = RemoteViews(packageName, R.layout.widget_twilight)
                views.setTextViewText(R.id.twilight_info, twilightData)
                views.setTextViewText(R.id.widget_update_time, updateTime)
                AppWidgetManager.getInstance(this@updateWidget)
                    .updateAppWidget(ComponentName(this@updateWidget, TwilightWidget::class.java), views)
            }
        }
    }

    private companion object {
        const val DOT = "⬤"
        const val ARROW = "<b>~</b>"
        const val GOLDEN_HOUR_EDGE = -4.0
        val TWILIGHTS = listOf(
            SunTimes.Twilight.ASTRONOMICAL, SunTimes.Twilight.NAUTICAL, SunTimes.Twilight.CIVIL,
            SunTimes.Twilight.NIGHT_HOUR, SunTimes.Twilight.BLUE_HOUR, SunTimes.Twilight.HORIZON,
            SunTimes.Twilight.VISUAL, SunTimes.Twilight.VISUAL_LOWER, SunTimes.Twilight.GOLDEN_HOUR,
        )
    }
}
