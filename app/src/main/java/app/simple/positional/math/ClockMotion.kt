package app.simple.positional.math

enum class ClockMotion(
    val key: String,
    val intervalMillis: Long,
    val fractional: Boolean,
    val animated: Boolean,
    val inertia: Float = RotationPhysics.INERTIA,
    val damping: Float = RotationPhysics.DAMPING,
    val magnetic: Float = RotationPhysics.MAGNETIC,
) {
    TICK("tick", 1000L, false, false),
    MECHANICAL("mechanical", 240L, true, false),
    SMOOTH("smooth", 0L, true, false),
    TICK_SMOOTH("tick_smooth", 1000L, true, true, damping = 5f),
    OSCILLATE("oscillate", 1000L, true, true, inertia = 1f, magnetic = 25000f);

    companion object {
        fun of(key: String?): ClockMotion = entries.firstOrNull { it.key == key } ?: TICK
    }
}
