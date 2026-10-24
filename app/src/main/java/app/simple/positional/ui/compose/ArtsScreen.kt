package app.simple.positional.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.constants.LauncherBackground
import app.simple.positional.util.ArtRaster
import app.simple.positional.util.BitmapHelper.addLinearGradient
import app.simple.positional.util.BitmapHelper.toBitmapKeepingSize

data class Art(@param:DrawableRes val background: Int, val startColor: Int, val endColor: Int)

object ArtsCatalog {
    val arts: List<Art> = (LauncherBackground.vectorBackground + LauncherBackground.vectorBackgroundNight)
        .zip(LauncherBackground.vectorColors + LauncherBackground.vectorNightColors) { background, colors ->
            Art(background, colors[0], colors[1])
        }
}

private val logoFont = FontFamily(Font(R.font.logo))

@Composable
fun ArtsScreen(initialPage: Int, cornerRadius: Dp, @DrawableRes pin: Int, onPageSettled: (Int) -> Unit) {
    val arts = ArtsCatalog.arts
    val state = rememberPagerState(initialPage = initialPage.coerceIn(0, arts.lastIndex)) { arts.size }
    LaunchedEffect(state) { snapshotFlow { state.settledPage }.collect { onPageSettled(it) } }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        VerticalPager(state = state, modifier = Modifier.fillMaxSize().systemBarsPadding(), beyondViewportPageCount = 1) { page ->
            ArtCard(arts[page], page, arts.size, cornerRadius, pin)
        }
    }
}

@Composable
private fun ArtCard(art: Art, index: Int, count: Int, cornerRadius: Dp, pin: Int) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    val glow = Color(art.startColor)
    val icon = remember(art, pin) {
        pin.toBitmapKeepingSize(context, 6, 255).addLinearGradient(intArrayOf(art.startColor, art.endColor)).asImageBitmap()
    }
    Box(
        Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.options_container_horizontal_padding))
            .shadow(25.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(colorResource(R.color.toolbarBackground)),
    ) {
        Text(
            "${index + 1}/$count",
            modifier = Modifier.align(Alignment.TopEnd).padding(25.dp),
            color = colorResource(R.color.textTertiary),
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(Modifier.align(Alignment.TopCenter).padding(top = 75.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.app_name),
                fontFamily = logoFont,
                fontSize = with(LocalDensity.current) { dimensionResource(R.dimen.launcher_app_title).toSp() },
                color = colorResource(R.color.textSecondary),
            )
            Image(icon, contentDescription = null, modifier = Modifier.padding(20.dp).size(dimensionResource(R.dimen.launcher_app_icon_dimen)))
        }
        LauncherArt(art.background, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
    }
}

@Composable
fun LauncherArt(@DrawableRes art: Int, modifier: Modifier, crop: Boolean = false) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val width = size.width
    val height = if (crop) size.height else 0
    val bitmap by produceState<ImageBitmap?>(null, art, width, height) {
        if (width > 0 && (!crop || height > 0)) value = ArtRaster.render(context, art, width, height).asImageBitmap()
    }
    val fade by animateFloatAsState(if (bitmap == null) 0f else 1f, label = "art")
    Box(modifier.onSizeChanged { size = it }) {
        bitmap?.let {
            Image(it, contentDescription = null, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = fade }, contentScale = ContentScale.FillWidth)
        }
    }
}
