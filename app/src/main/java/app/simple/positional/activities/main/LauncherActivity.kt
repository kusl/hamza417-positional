package app.simple.positional.activities.main

import android.content.Intent
import android.os.Bundle
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.FragmentPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.PanelCatalog
import app.simple.positional.ui.compose.SplashScreen

class LauncherActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShortcutScreen()
        if (MainPreferences.getSkipSplashScreen()) {
            openMain()
        } else {
            setPositionalContent { SplashScreen(::openMain) }
        }
    }

    private fun openMain() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setAction(intent.action))
        finish()
    }

    private fun setShortcutScreen() {
        when (intent.action) {
            "open_clock" -> FragmentPreferences.setCurrentTag(PanelCatalog.CLOCK)
            "open_compass" -> FragmentPreferences.setCurrentTag(PanelCatalog.COMPASS)
            "action_map_panel_full", "open_gps" -> FragmentPreferences.setCurrentTag(PanelCatalog.LOCATION)
            "open_level" -> FragmentPreferences.setCurrentTag(PanelCatalog.LEVEL)
        }
    }
}
