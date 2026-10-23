package app.simple.positional.util

@Suppress("unused")
object ConditionUtils {
    fun Any?.isNull(): Boolean {
        return this == null
    }

    fun Any?.isNotNull(): Boolean {
        return this != null
    }

    fun Number.isZero(): Boolean {
        return this == 0
    }

    fun Boolean.invert(): Boolean {
        return !this
    }
}
