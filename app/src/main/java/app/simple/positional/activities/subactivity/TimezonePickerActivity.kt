package app.simple.positional.activities.subactivity

import android.os.Bundle
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.ui.compose.TimeZoneScreen
import app.simple.positional.ui.compose.setPositionalContent
import app.simple.positional.util.LocaleHelper

class TimezonePickerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selected = ClockPreferences.getTimeZone()
        val twelveHour = ClockPreferences.getDefaultClockTimeFormat()
        setPositionalContent { topInset ->
            TimeZoneScreen(selected, twelveHour, LocaleHelper.getAppLocale(), topInset) {
                ClockPreferences.setTimeZone(it)
                finish()
            }
        }
    }
}
