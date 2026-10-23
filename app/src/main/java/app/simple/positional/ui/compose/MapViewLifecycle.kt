package app.simple.positional.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.simple.positional.decorations.maps.PickerMap

@Composable
fun rememberPickerMap(): PickerMap {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val map = remember { PickerMap.create(context) }
    DisposableEffect(owner, map) {
        val delegate = map.lifecycleObserver
        var state = Lifecycle.State.INITIALIZED
        val observer = LifecycleEventObserver { source, event ->
            when (event) {
                Lifecycle.Event.ON_START -> delegate.onStart(source)
                Lifecycle.Event.ON_RESUME -> delegate.onResume(source)
                Lifecycle.Event.ON_PAUSE -> delegate.onPause(source)
                Lifecycle.Event.ON_STOP -> delegate.onStop(source)
                Lifecycle.Event.ON_DESTROY -> delegate.onDestroy(source)
                else -> Unit
            }
            state = event.targetState
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (state != Lifecycle.State.DESTROYED) {
                if (state.isAtLeast(Lifecycle.State.RESUMED)) delegate.onPause(owner)
                if (state.isAtLeast(Lifecycle.State.STARTED)) delegate.onStop(owner)
                delegate.onDestroy(owner)
            }
        }
    }
    return map
}
