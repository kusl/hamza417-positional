package app.simple.positional.preferences

import app.simple.positional.singleton.SharedPreferences.getSharedPreferences

object FragmentPreferences {
    private const val currentTag = "current_tag"

    fun setCurrentTag(value: String) {
        getSharedPreferences().edit().putString(currentTag, value).apply()
    }

    fun getCurrentTag(): String {
        return getSharedPreferences().getString(currentTag, "location")!!
    }
}
