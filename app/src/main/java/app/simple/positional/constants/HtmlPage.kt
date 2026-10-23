package app.simple.positional.constants

import androidx.annotation.StringRes
import app.simple.positional.R

enum class HtmlPage(val asset: String, @param:StringRes val title: Int? = null) {
    DISCLAIMER("disclaimer", R.string.disclaimer),
    PRIVACY_POLICY("privacy_policy", R.string.privacy_policy),
    TERMS("terms_and_conditions", R.string.terms_of_use),
    PERMISSIONS("required_permissions", R.string.permissions),
    CREDITS("credits", R.string.credits),
    INTERNET_USES("internet_uses", R.string.internet_uses),
    PHYSICAL_PROPERTIES("physical_properties"),
    CUSTOM_COORDINATES_HELP("custom_coordinates_help"),
    MEDIA_KEYS("media_keys"),
    TRAIL_HELP("trail_help");

    val url: String get() = "file:///android_asset/html/$asset.html"

    companion object {
        fun forTitle(@StringRes title: Int): HtmlPage? = entries.firstOrNull { it.title == title }

        fun forName(name: String?): HtmlPage? = entries.firstOrNull { it.name == name }
    }
}
