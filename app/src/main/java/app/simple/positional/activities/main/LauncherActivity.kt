package app.simple.positional.activities.main

import android.content.Intent
import android.os.Bundle
import app.simple.positional.R
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.FragmentPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.PanelCatalog
import app.simple.positional.ui.launcher.SplashScreen

class LauncherActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShortcutScreen()
        setContentView(R.layout.activity_launcher)

        if (MainPreferences.getSkipSplashScreen()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.action = this.intent.action
            startActivity(intent)
            finish()
        } else if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.dialog_in, R.anim.dialog_out)
                .replace(R.id.launcher_act, SplashScreen.newInstance(), "launcher")
                .commit()
        }
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
