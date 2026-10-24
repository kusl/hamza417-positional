package app.simple.positional.singleton

import android.content.Context
import android.content.SharedPreferences

object SharedPreferences {
    const val preferences = "Preferences"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
        }
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences ?: throw NullPointerException()
    }

    fun getSharedPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
    }
}
