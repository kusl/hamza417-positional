package app.simple.positional.math

import kotlin.math.abs
import kotlin.math.sin

class RotationPhysics(inertia: Float = INERTIA, damping: Float = DAMPING, magnetic: Float = MAGNETIC) {
    var inertia = INERTIA
        private set
    var damping = DAMPING
        private set
    var magnetic = MAGNETIC
        private set
    var angle = 0.0
        private set
    var target = 0.0
        private set
    private var previous = 0.0
    private var lastFrame = -1L

    init {
        configure(inertia, damping, magnetic)
    }

    val isSettled: Boolean
        get() = abs(shortestDelta(angle, target)) < SETTLED_ANGLE && abs(angle - previous) < SETTLED_ANGLE

    fun configure(inertia: Float, damping: Float, magnetic: Float) {
        this.inertia = if (inertia >= 0f) inertia else INERTIA
        this.damping = if (damping >= 0f) damping else DAMPING
        this.magnetic = if (magnetic >= 0f) magnetic else MAGNETIC
    }

    fun snap(value: Float) {
        angle = value.toDouble()
        previous = angle
        target = angle
        lastFrame = -1L
    }

    fun aim(value: Float) {
        target = value.toDouble()
    }

    fun step(frameMillis: Long): Boolean {
        val dt = if (lastFrame < 0L) FRAME else ((frameMillis - lastFrame) / 1000.0).coerceIn(MIN_DT, MAX_DT)
        lastFrame = frameMillis
        val coefInertia = inertia / (dt * dt)
        val coefDamping = damping / dt
        val torque = magnetic * sin(Math.toRadians(target - angle))
        val denominator = coefInertia + coefDamping
        val next = if (denominator > 0.0) {
            (coefInertia * (2 * angle - previous) + coefDamping * angle + torque) / denominator
        } else {
            angle + shortestDelta(angle, target)
        }
        previous = angle
        angle = next
        if (isSettled) {
            previous = angle
            lastFrame = -1L
            return false
        }
        return true
    }

    companion object {
        const val INERTIA = 0.5f
        const val DAMPING = 10f
        const val MAGNETIC = 5000f
        private const val SETTLED_ANGLE = 0.1
        private const val FRAME = 1.0 / 60.0
        private const val MIN_DT = 0.001
        private const val MAX_DT = 0.05

        fun shortestDelta(from: Double, to: Double): Double = ((to - from) % 360.0 + 540.0) % 360.0 - 180.0
    }
}
