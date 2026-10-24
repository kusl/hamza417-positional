package app.simple.positional.decorations.views

import android.content.Context
import android.content.res.Configuration
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class CustomWebView(context: Context) : WebView(context) {
    init {
        setBackgroundColor(0)
        settings.setSupportZoom(true)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, night)
        }
    }
}
