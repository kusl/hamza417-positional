package app.simple.positional.activities.subactivity

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.AccentColorScreen
import app.simple.positional.ui.compose.AccentOption
import app.simple.positional.ui.compose.AccentPalette
import app.simple.positional.ui.compose.PositionalTheme
import app.simple.positional.util.StatusBarHeight

class AccentColorsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = AccentPalette.options(this)
        val topInset = StatusBarHeight.getStatusBarHeight(resources)
        val selected = MainPreferences.getAccentColor()
        val materialYou = MainPreferences.isMaterialYouAccentColor()
        setContentView(ComposeView(this).apply {
            setContent {
                PositionalTheme {
                    AccentColorScreen(
                        options = options,
                        selected = selected,
                        materialYou = materialYou,
                        topInset = with(LocalDensity.current) { topInset.toDp() },
                        onSelect = this@AccentColorsActivity::select,
                    )
                }
            }
        })
    }

    private fun select(option: AccentOption) {
        if (AccentPalette.isSelected(option, MainPreferences.getAccentColor(), MainPreferences.isMaterialYouAccentColor())) return
        MainPreferences.setMaterialYouAccentColor(option.dynamic)
        MainPreferences.setAccentColor(option.color)
        recreate()
    }
}
