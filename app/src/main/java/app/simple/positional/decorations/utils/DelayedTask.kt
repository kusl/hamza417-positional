package app.simple.positional.decorations.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

class DelayedTask(private val delayMillis: Long, private val action: () -> Unit) {
    private var job: Job? = null

    fun schedule(scope: CoroutineScope?) {
        cancel()
        job = scope?.launch {
            delay(delayMillis)
            action()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

fun CoroutineScope.child(): CoroutineScope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job))
