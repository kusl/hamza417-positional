@file:Suppress("LeakingThis")

package app.simple.positional.decorations.views

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

open class CustomWebView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : WebView(context, attributeSet) {
    init {
        setBackgroundColor(0)
        settings.setSupportZoom(true)
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, night)
        } else if (night && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
        }
    }
}
