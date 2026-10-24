package app.simple.positional.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

data class ArtFrame(val width: Int, val height: Int, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object {
        fun of(intrinsicWidth: Int, intrinsicHeight: Int, width: Int, height: Int = 0): ArtFrame {
            val scale = if (height > 0) max(width / intrinsicWidth.toFloat(), height / intrinsicHeight.toFloat()) else width / intrinsicWidth.toFloat()
            val drawWidth = (intrinsicWidth * scale).roundToInt()
            val drawHeight = (intrinsicHeight * scale).roundToInt()
            val frameHeight = if (height > 0) height else drawHeight
            val left = (width - drawWidth) / 2
            val top = (frameHeight - drawHeight) / 2
            return ArtFrame(width, frameHeight, left, top, left + drawWidth, top + drawHeight)
        }
    }
}

object ArtRaster {
    suspend fun render(context: Context, @DrawableRes art: Int, width: Int, height: Int = 0): Bitmap = withContext(Dispatchers.Default) {
        val drawable = checkNotNull(ContextCompat.getDrawable(context, art)).mutate()
        val frame = ArtFrame.of(drawable.intrinsicWidth, drawable.intrinsicHeight, width, height)
        Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(frame.left, frame.top, frame.right, frame.bottom)
            drawable.draw(Canvas(it))
        }
    }
}
