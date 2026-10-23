package app.simple.positional.activities.subactivity

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import app.simple.positional.constants.LocationPins
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.ArtsScreen
import app.simple.positional.ui.compose.PositionalTheme

class ArtsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val radius = MainPreferences.getCornerRadius()
        val pin = LocationPins.getLocationPin()
        setContentView(ComposeView(this).apply {
            setContent {
                PositionalTheme {
                    ArtsScreen(
                        initialPage = MainPreferences.getCurrentArt(),
                        cornerRadius = with(LocalDensity.current) { radius.toDp() },
                        pin = pin,
                        onPageSettled = MainPreferences::setCurrentArt,
                    )
                }
            }
        })
    }
}
