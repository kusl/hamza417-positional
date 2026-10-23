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
import app.simple.positional.util.AccentThemes
import app.simple.positional.util.ContextUtils
import app.simple.positional.util.LocaleHelper
import app.simple.positional.widgets.ClockWidgets

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

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) ClockWidgets.refresh(this)
    }

    private fun accentTheme(): Int = AccentThemes.of(this) ?: R.style.Positional.also {
        MainPreferences.setAccentColor(getColor(R.color.positional))
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
}
