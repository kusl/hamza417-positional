package app.simple.positional.singleton

import android.content.Context
import android.content.SharedPreferences
import app.simple.positional.util.ConditionUtils.isNull

object SharedPreferences {
    const val preferences = "Preferences"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences.isNull()) {
            sharedPreferences = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
        }
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences ?: throw NullPointerException()
    }

    fun getSharedPreferences(context: Context): SharedPreferences {
        return if (sharedPreferences.isNull()) {
            context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
        } else {
            sharedPreferences!!
        }
    }
}
