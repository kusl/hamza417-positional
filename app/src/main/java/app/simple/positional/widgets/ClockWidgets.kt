package app.simple.positional.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Icon
import android.os.Build
import android.view.ContextThemeWrapper
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import app.simple.positional.R
import app.simple.positional.constants.ClockSkinsConstants
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.util.AccentThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.roundToInt

class ClockWidget2x2 : ClockWidget(ClockWidgetSpec(R.layout.widget_clock_2x2, 110, 10, 15, "2x2", 1522))
class ClockWidget3x3 : ClockWidget(ClockWidgetSpec(R.layout.widget_clock_3x3, 180, 25, 25, "3x3", 1533))
class ClockWidget4x4 : ClockWidget(ClockWidgetSpec(R.layout.widget_clock, 250, 25, 30, "4x4", 1544))

data class ClockWidgetSpec(val layout: Int, val sizeDp: Int, val handInsetDp: Int, val indicatorDp: Int, val legacyService: String, val legacyAlarm: Int)

abstract class ClockWidget(private val spec: ClockWidgetSpec) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        val provider = javaClass
        CoroutineScope(Dispatchers.Default).launch {
            try {
                cancelLegacyAlarm(context)
                appWidgetManager.updateAppWidget(appWidgetIds, ClockWidgetRenderer.render(context, spec))
                ClockWidgets.schedule(context, provider, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    private fun cancelLegacyAlarm(context: Context) {
        val intent = Intent().setClassName(context, "app.simple.positional.services.ClockWidgetService${spec.legacyService}")
        PendingIntent.getService(context, spec.legacyAlarm, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(ClockWidgets.updateIntent(context, javaClass, intArrayOf()))
    }
}

object ClockWidgets {
    private val providers = listOf(ClockWidget2x2::class.java, ClockWidget3x3::class.java, ClockWidget4x4::class.java)

    fun refresh(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = AppWidgetManager.getInstance(context) ?: return
        providers.forEach { provider ->
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isNotEmpty()) context.sendBroadcast(intent(context, provider, ids))
        }
    }

    internal fun schedule(context: Context, provider: Class<out ClockWidget>, ids: IntArray) {
        val next = DayNight.nextChange(ZonedDateTime.now()).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, next, updateIntent(context, provider, ids))
    }

    internal fun updateIntent(context: Context, provider: Class<out ClockWidget>, ids: IntArray): PendingIntent =
        PendingIntent.getBroadcast(context, provider.name.hashCode(), intent(context, provider, ids), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun intent(context: Context, provider: Class<out ClockWidget>, ids: IntArray) = Intent(context, provider)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
}

object DayNight {
    private const val DAY_START = 6
    private const val NIGHT_START = 19

    fun isDay(hour: Int) = hour in DAY_START until NIGHT_START

    fun nextChange(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate()
        return when {
            now.hour < DAY_START -> today.atTime(DAY_START, 0)
            now.hour < NIGHT_START -> today.atTime(NIGHT_START, 0)
            else -> today.plusDays(1).atTime(DAY_START, 0)
        }.atZone(now.zone)
    }
}

private object ClockWidgetRenderer {
    private const val FACE_RADIUS = 0.47f
    private const val TRAIL_INSET_DP = 5
    private const val INDICATOR_SCALE = 2

    fun render(context: Context, spec: ClockWidgetSpec): RemoteViews {
        SharedPreferences.init(context)
        val themed = ContextThemeWrapper(context, AccentThemes.of(context) ?: R.style.Positional)
        val density = context.resources.displayMetrics.density
        val size = (spec.sizeDp * density).roundToInt()
        val inset = (spec.handInsetDp * density).roundToInt()
        val indicator = (spec.indicatorDp * density).roundToInt() * INDICATOR_SCALE
        val needles = ClockSkinsConstants.clockNeedleSkins.getOrElse(ClockPreferences.getClockNeedleTheme()) { ClockSkinsConstants.clockNeedleSkins[0] }
        val dayNight = if (DayNight.isDay(ZonedDateTime.now().hour)) R.drawable.ic_day else R.drawable.ic_night
        return RemoteViews(context.packageName, spec.layout).apply {
            setImageViewBitmap(R.id.widget_clock_face, themed.bitmapOf(R.drawable.clock_face, size))
            setImageViewBitmap(R.id.widget_day_night_indicator, themed.bitmapOf(dayNight, indicator))
            setIcon(R.id.widget_clock, "setDial", Icon.createWithBitmap(Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)))
            setIcon(R.id.widget_clock, "setHourHand", Icon.createWithBitmap(themed.bitmapOf(needles[0], size, inset)))
            setIcon(R.id.widget_clock, "setMinuteHand", Icon.createWithBitmap(themed.bitmapOf(needles[1], size, inset)))
            setIcon(R.id.widget_clock, "setSecondHand", Icon.createWithBitmap(themed.secondHand(needles[2], size, inset, (TRAIL_INSET_DP * density).roundToInt())))
        }
    }

    private fun Context.bitmapOf(@DrawableRes id: Int, size: Int, inset: Int = 0): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawInto(Canvas(bitmap), id, size, inset)
        return bitmap
    }

    private fun Context.secondHand(@DrawableRes id: Int, size: Int, inset: Int, trailInset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        canvas.save()
        canvas.rotate(-90f, center, center)
        drawInto(canvas, R.drawable.clock_trail, size, trailInset)
        canvas.restore()
        canvas.drawCircle(center, center, size * FACE_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
        drawInto(canvas, id, size, inset)
        return bitmap
    }

    private fun Context.drawInto(canvas: Canvas, @DrawableRes id: Int, size: Int, inset: Int) {
        ContextCompat.getDrawable(this, id)?.run {
            setBounds(inset, inset, size - inset, size - inset)
            draw(canvas)
        }
    }
}
