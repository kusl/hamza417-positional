package app.simple.positional.decorations.utils

import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DelayedTaskTest {
    private val scope = TestScope()
    private var runs = 0
    private val task = DelayedTask(DELAY) { runs++ }

    private fun advance(millis: Long) {
        scope.advanceTimeBy(millis)
        scope.runCurrent()
    }

    @Test
    fun runsOnceAfterDelay() {
        task.schedule(scope)
        advance(DELAY - 1)
        assertEquals(0, runs)
        advance(1)
        assertEquals(1, runs)
        advance(DELAY * 3)
        assertEquals(1, runs)
    }

    @Test
    fun rescheduleRestartsDelay() {
        task.schedule(scope)
        advance(DELAY - 1)
        task.schedule(scope)
        advance(DELAY - 1)
        assertEquals(0, runs)
        advance(1)
        assertEquals(1, runs)
    }

    @Test
    fun cancelStopsPendingRun() {
        task.schedule(scope)
        advance(DELAY / 2)
        task.cancel()
        advance(DELAY * 2)
        assertEquals(0, runs)
    }

    @Test
    fun withoutScopeNothingRuns() {
        task.schedule(null)
        advance(DELAY * 2)
        assertEquals(0, runs)
    }

    @Test
    fun cancelledChildStopsTaskButNotParent() {
        val child = scope.child()
        task.schedule(child)
        child.cancel()
        advance(DELAY * 2)
        assertEquals(0, runs)
        assertTrue(scope.isActive)
        task.schedule(scope)
        advance(DELAY)
        assertEquals(1, runs)
    }

    @Test
    fun cancelledParentCancelsChild() {
        val parent = TestScope()
        val child = parent.child()
        parent.cancel()
        assertFalse(child.isActive)
    }

    private companion object {
        const val DELAY = 500L
    }
}
