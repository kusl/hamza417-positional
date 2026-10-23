package app.simple.positional.extensions.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import app.simple.positional.R
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.util.ContextUtils
import app.simple.positional.util.LocaleHelper

open class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        SharedPreferences.init(newBase)
        super.attachBaseContext(ContextUtils.wrap(newBase, ContextUtils.language()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(accentTheme())
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (MainPreferences.getLaunchCount() == 0 && DateFormat.is24HourFormat(this)) {
            ClockPreferences.setDefaultClockTime(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }
        window.setFormat(PixelFormat.RGBA_8888)
        if (MainPreferences.isScreenOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        keepContentAboveNavigationBar()
        ConfigurationCompat.getLocales(resources.configuration)[0]?.let(LocaleHelper::setAppLocale)
    }

    private fun accentTheme(): Int {
        if (MainPreferences.isMaterialYouAccentColor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return R.style.MaterialYou
        val accent = MainPreferences.getAccentColor()
        return accentThemes.firstOrNull { getColor(it.first) == accent }?.second ?: R.style.Positional.also {
            MainPreferences.setAccentColor(getColor(R.color.positional))
        }
    }

    private fun keepContentAboveNavigationBar() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private companion object {
        val accentThemes = listOf(
            R.color.positional to R.style.Positional,
            R.color.blue to R.style.Blue,
            R.color.blueGrey to R.style.BlueGrey,
            R.color.darkBlue to R.style.DarkBlue,
            R.color.red to R.style.Red,
            R.color.green to R.style.Green,
            R.color.orange to R.style.Orange,
            R.color.purple to R.style.Purple,
            R.color.yellow to R.style.Yellow,
            R.color.caribbeanGreen to R.style.CaribbeanGreen,
            R.color.persianGreen to R.style.PersianGreen,
            R.color.amaranth to R.style.Amaranth,
            R.color.indian_red to R.style.IndianRed,
            R.color.light_coral to R.style.LightCoral,
            R.color.pink_flare to R.style.PinkFlare,
            R.color.makeup_tan to R.style.MakeupTan,
            R.color.egg_yellow to R.style.EggYellow,
            R.color.medium_green to R.style.MediumGreen,
            R.color.olive to R.style.Olive,
            R.color.copperfield to R.style.Copperfield,
            R.color.mineral_green to R.style.MineralGreen,
            R.color.lochinvar to R.style.Lochinvar,
            R.color.beach_grey to R.style.BeachGrey,
        )
    }
}
