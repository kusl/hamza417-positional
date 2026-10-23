package app.simple.positional.util

import app.simple.positional.database.instances.TrailDatabase

object TrailNames {
    fun isValid(name: String, existing: Collection<String>): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotEmpty() &&
            trimmed.none { it == '/' || it == '\\' } &&
            !trimmed.equals(TrailDatabase.NAME, ignoreCase = true) &&
            existing.none { it.equals(trimmed, ignoreCase = true) }
    }
}
