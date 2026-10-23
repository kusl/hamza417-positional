package app.simple.positional.util

object ArrayHelper {
    fun ArrayList<Float>.isLastValueSame(value: Float): Boolean {
        return try {
            if (this.size == 0) {
                false
            } else {
                this.last() == value
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            false
        }
    }

    fun <T> List<T>.secondLastOrNull(): T? {
        return if (size < 2) null else this[size - 2]
    }
}
