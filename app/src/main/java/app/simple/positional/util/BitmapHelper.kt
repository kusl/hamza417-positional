package app.simple.positional.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat

object BitmapHelper {
    fun Bitmap.addLinearGradient(array: IntArray): Bitmap {
        val updatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(updatedBitmap)
        canvas.drawBitmap(this, 0f, 0f, null)
        val paint = Paint()
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), array[0], array[1], Shader.TileMode.CLAMP)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return updatedBitmap
    }

    fun Int.toBitmap(context: Context, size: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable?.setBounds(0, 0, size, size)
        drawable?.draw(Canvas(bitmap))
        return bitmap
    }

    fun Int.toBitmapKeepingSize(context: Context, incrementFactor: Int, alpha: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val intrinsicWidth = drawable!!.intrinsicWidth * incrementFactor
        val intrinsicHeight = drawable.intrinsicHeight * incrementFactor
        drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        drawable.alpha = alpha
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    fun Int.toBitmapKeepingSize(context: Context, @IntRange(from = 1, to = 10) incrementFactor: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val intrinsicWidth = drawable!!.intrinsicWidth * incrementFactor
        val intrinsicHeight = drawable.intrinsicHeight * incrementFactor
        drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }
}
