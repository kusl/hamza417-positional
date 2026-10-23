package app.simple.positional.extensions.panel

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.simple.positional.constants.HtmlPage
import app.simple.positional.ui.navigation.AppNavigator
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.ui.navigation.SearchResult
import app.simple.positional.singleton.SharedPreferences.getSharedPreferences
import app.simple.positional.ui.compose.DialogHost
import app.simple.positional.ui.compose.MenuSheet
import app.simple.positional.ui.compose.MenuSheetState

abstract class PanelController(val activity: ComponentActivity) : LifecycleOwner, SharedPreferences.OnSharedPreferenceChangeListener {
    private val registry = LifecycleRegistry(this)
    private val launchers = mutableListOf<ActivityResultLauncher<*>>()
    private var destroyed = false
    private val parentObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) return@LifecycleEventObserver
        registry.handleLifecycleEvent(event)
        when (event) {
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            else -> Unit
        }
    }

    override val lifecycle: Lifecycle get() = registry
    protected val owner: LifecycleOwner get() = this
    protected val dialogs = DialogHost()
    protected val menuSheet = MenuSheetState()
    protected val isLandscapeOrientation: Boolean = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    init {
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }

    @Composable
    abstract fun Content(topInset: Dp)

    protected open fun onResume() {}

    protected open fun onPause() {}

    protected open fun onCleared() {}

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {}

    protected fun getString(@StringRes id: Int): String = activity.getString(id)

    protected fun getString(@StringRes id: Int, vararg args: Any): String = activity.getString(id, *args)

    protected fun startActivity(intent: Intent) = activity.startActivity(intent)

    protected fun navigate(route: String) {
        (activity as? AppNavigator)?.navigate(route)
    }

    protected fun openPage(page: HtmlPage) = navigate(Routes.web(page))

    protected fun search(onResult: (SearchResult) -> Unit) {
        (activity as? AppNavigator)?.search(onResult)
    }

    protected fun <I, O> register(contract: ActivityResultContract<I, O>, callback: (O) -> Unit): ActivityResultLauncher<I> =
        activity.activityResultRegistry.register("${javaClass.name}#${launchers.size}", contract, callback).also { launchers += it }

    private fun follow(parent: LifecycleOwner) {
        if (!destroyed) parent.lifecycle.addObserver(parentObserver)
    }

    private fun unfollow(parent: LifecycleOwner) {
        parent.lifecycle.removeObserver(parentObserver)
        if (destroyed) return
        if (registry.currentState.isAtLeast(Lifecycle.State.RESUMED)) onPause()
        if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) registry.currentState = Lifecycle.State.CREATED
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
        launchers.forEach { it.unregister() }
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.DESTROYED
        onCleared()
    }

    @Composable
    fun Host(topInset: Dp) {
        val parent = LocalLifecycleOwner.current
        DisposableEffect(this, parent) {
            follow(parent)
            onDispose { unfollow(parent) }
        }
        Content(topInset)
        MenuSheet(menuSheet)
        dialogs.content?.invoke()
    }
}
