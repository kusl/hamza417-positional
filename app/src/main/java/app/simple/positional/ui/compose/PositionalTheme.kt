package app.simple.positional.ui.compose

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import app.simple.positional.R
import app.simple.positional.util.StatusBarHeight

object PositionalFonts {
    val regular = FontFamily(Font(R.font.regular))
    val medium = FontFamily(Font(R.font.medium, FontWeight.Medium))
    val heading = FontFamily(Font(R.font.heading, FontWeight.Bold))
    val light = FontFamily(Font(R.font.light, FontWeight.Light))
    val degrees = FontFamily(Font(R.font.degrees))
}

private val typography = Typography(
    headlineMedium = TextStyle(fontFamily = PositionalFonts.heading, fontWeight = FontWeight.Bold, fontSize = 31.sp),
    titleLarge = TextStyle(fontFamily = PositionalFonts.medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = PositionalFonts.medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = PositionalFonts.regular, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = PositionalFonts.medium, fontSize = 14.sp),
)

fun Context.appAccentColor(): Int =
    obtainStyledAttributes(intArrayOf(R.attr.colorAppAccent)).use { it.getColor(0, getColor(R.color.positional)) }

@Composable
fun PositionalTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val accent = remember(context) { Color(context.appAccentColor()) }
    val background = colorResource(R.color.mainBackground)
    val primaryText = colorResource(R.color.textPrimary)
    val tertiaryText = colorResource(R.color.textTertiary)
    val divider = colorResource(R.color.dividerColor)
    val switchOff = colorResource(R.color.switch_off)
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        background = background,
        surface = background,
        surfaceContainer = background,
        onBackground = primaryText,
        onSurface = primaryText,
        onSurfaceVariant = tertiaryText,
        outline = divider,
        outlineVariant = divider,
        surfaceContainerHighest = switchOff,
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

fun ComponentActivity.setPositionalContent(content: @Composable (topInset: Dp) -> Unit) {
    val topInset = StatusBarHeight.getStatusBarHeight(resources)
    setContentView(ComposeView(this).apply {
        setContent {
            PositionalTheme { content(with(LocalDensity.current) { topInset.toDp() }) }
        }
    })
}

fun Fragment.positionalComposeView(menu: MenuSheetState? = null, dialog: DialogHost? = null, content: @Composable (topInset: Dp) -> Unit): ComposeView {
    val topInset = StatusBarHeight.getStatusBarHeight(resources)
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            PositionalTheme {
                content(with(LocalDensity.current) { topInset.toDp() })
                menu?.let { MenuSheet(it) }
                dialog?.content?.invoke()
            }
        }
    }
}
