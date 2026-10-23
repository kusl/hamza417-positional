package app.simple.positional.ui.compose

import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.simple.positional.R
import app.simple.positional.preferences.MainPreferences
import kotlinx.coroutines.delay

@Composable
fun MapPanelScaffold(
    map: View,
    topInset: Dp,
    landscape: Boolean,
    fullScreen: Boolean,
    toolsLeft: Boolean,
    toolbar: @Composable () -> Unit,
    tools: @Composable (horizontal: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    side: (@Composable () -> Unit)? = null,
    bottom: @Composable () -> Unit = {},
) {
    val margin = dimensionResource(R.dimen.trail_tools_margin)
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (landscape && side != null) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
                    overlay()
                    MapToolsSurface(true, Modifier.align(Alignment.BottomCenter).padding(margin)) { tools(true) }
                }
                VerticalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outline)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 24.dp),
                ) {
                    Box(Modifier.padding(top = topInset)) { toolbar() }
                    side()
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
                overlay()
                AnimatedVisibility(
                    !fullScreen,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Box(Modifier.padding(top = topInset).padding(dimensionResource(R.dimen.toolbar_padding) / 2)) { toolbar() }
                }
                MapToolsSurface(
                    false,
                    Modifier
                        .align(if (toolsLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = margin),
                ) { tools(false) }
                AnimatedVisibility(!fullScreen, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
                    bottom()
                }
            }
        }
    }
}

@Composable
fun MapToolbar(title: String, actions: @Composable RowScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(cornerRadius()), color = MaterialTheme.colorScheme.background, shadowElevation = 5.dp) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
private fun MapToolsSurface(horizontal: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(cornerRadius()), color = MaterialTheme.colorScheme.background, shadowElevation = 10.dp) {
        val padding = Modifier.padding(dimensionResource(R.dimen.trail_tools_padding))
        if (horizontal) {
            Row(padding, horizontalArrangement = Arrangement.SpaceEvenly) { content() }
        } else {
            Column(padding, horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MapToolButton(@DrawableRes icon: Int, alpha: Float = 1f, tint: Boolean = true, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .size(dimensionResource(R.dimen.app_button_size) + 8.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        val modifier = Modifier.size(24.dp)
        if (tint) {
            Icon(painterResource(icon), contentDescription = null, tint = colorResource(R.color.iconColor), modifier = modifier)
        } else {
            Image(painterResource(icon), contentDescription = null, modifier = modifier)
        }
    }
}

@Composable
fun AlignToolButton(toolsLeft: Boolean, onClick: () -> Unit) =
    MapToolButton(if (toolsLeft) R.drawable.ic_arrow_right else R.drawable.ic_arrow_left, onClick = onClick)

@Composable
fun LocationToolButton(fixed: Boolean, enabled: Boolean, onLocate: (close: Boolean) -> Unit) {
    var blink by remember { mutableStateOf(false) }
    val searching = enabled && !fixed
    LaunchedEffect(searching) {
        while (searching) {
            delay(BLINK)
            blink = !blink
        }
    }
    val icon = when {
        !enabled -> R.drawable.ic_gps_off
        fixed || blink -> R.drawable.ic_gps_fixed
        else -> R.drawable.ic_gps_not_fixed
    }
    MapToolButton(icon, onLongClick = { onLocate(true) }) { onLocate(false) }
}

@Composable
fun ConfirmToolButton(@DrawableRes icon: Int, onConfirm: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Box {
        MapToolButton(icon) { confirm = true }
        DropdownMenu(expanded = confirm, onDismissRequest = { confirm = false }) {
            MenuEntry(R.string.sure) {
                confirm = false
                onConfirm()
            }
            MenuEntry(R.string.button_cancel) { confirm = false }
        }
    }
}

@Composable
fun toggleAlpha(on: Boolean): Float = animateFloatAsState(if (on) 1f else 0.4f, label = "toggle").value

@Composable
fun BoxScope.MapCrosshair() {
    Icon(
        painterResource(R.drawable.ic_crosshair),
        contentDescription = null,
        tint = colorResource(R.color.iconColor),
        modifier = Modifier.size(dimensionResource(R.dimen.app_button_size)).align(Alignment.Center),
    )
}

@Composable
fun SheetHandle(onClick: () -> Unit) {
    Box(Modifier.navigationBarsPadding().padding(bottom = dimensionResource(R.dimen.bottom_nav_height) + 16.dp)) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.background, shadowElevation = 5.dp, onClick = onClick) {
            Icon(
                painterResource(R.drawable.ic_expand_up),
                contentDescription = stringResource(R.string.gps_info),
                tint = colorResource(R.color.iconColor),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).size(dimensionResource(R.dimen.app_button_size)),
            )
        }
    }
}

@Composable
fun cornerRadius(): Dp = with(LocalDensity.current) { MainPreferences.getCornerRadius().toDp() }

private const val BLINK = 500L
