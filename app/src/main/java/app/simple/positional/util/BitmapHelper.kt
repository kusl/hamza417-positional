package app.simple.positional.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat

object BitmapHelper {
    fun Bitmap.addLinearGradient(array: IntArray): Bitmap {
        val width = width
        val height = height
        val updatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(updatedBitmap)
        canvas.drawBitmap(this, 0f, 0f, null)
        val paint = Paint()
        val shader = LinearGradient(0f, 0f, 0f, height.toFloat(), array[0], array[1], Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return updatedBitmap
    }

    fun addLinearGradient(originalBitmap: Bitmap, array: IntArray, verticalOffset: Float): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val updatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(updatedBitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        val paint = Paint()
        val shader = LinearGradient(0f, verticalOffset, 0f, height.toFloat(), array[0], array[1], Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return updatedBitmap
    }

    fun addRadialGradient(originalBitmap: Bitmap, int: Int): Bitmap? {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val updatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(updatedBitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        val paint = Paint()
        val shader = RadialGradient(width.div(2F), height.div(2F), 200f, 0x000000, int, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return updatedBitmap
    }

    fun rotateBitmap(bitmap: Bitmap, rotationAngleDegree: Float): Bitmap {
        val w = bitmap.height
        val h = bitmap.height
        var newW = w
        var newH = h
        if (rotationAngleDegree == 90F || rotationAngleDegree == 270F) {
            newW = h
            newH = w
        }
        val rotatedBitmap = Bitmap.createBitmap(newW, newH, bitmap.config
                                                            ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rotatedBitmap)
        val rect = Rect(0, 0, newW, newH)
        val matrix = Matrix()
        val px: Float = rect.exactCenterX()
        val py: Float = rect.exactCenterY()
        matrix.postTranslate((-bitmap.width / 2).toFloat(), (-bitmap.height / 2).toFloat())
        matrix.postRotate(rotationAngleDegree)
        matrix.postTranslate(px, py)
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
        matrix.reset()
        return rotatedBitmap
    }

    fun rotateBitmap(bitmap: Bitmap, rotationAngleDegree: Float, tilt: Float): Bitmap? {
        val w = bitmap.height
        val h = bitmap.height
        var newW = w
        var newH = h

        if (rotationAngleDegree == 90F || rotationAngleDegree == 270F) {
            newW = h
            newH = w
        }

        val rotatedBitmap = Bitmap.createBitmap(newW, newH, bitmap.config
                                                            ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rotatedBitmap)
        val rect = Rect(0, 0, newW, newH)
        val matrix = Matrix()

        matrix.postRotate(rotationAngleDegree)

        val camera = Camera()
        camera.save()
        camera.rotateX(tilt)
        camera.getMatrix(matrix)
        camera.restore()

        val px: Float = rect.exactCenterX()
        val py: Float = rect.exactCenterY()
        matrix.postTranslate((-bitmap.width / 2).toFloat(), (-bitmap.height / 2).toFloat())
        matrix.postTranslate(px, py)

        canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
        matrix.reset()
        return rotatedBitmap
    }

    fun Int.toBitmap(context: Context, size: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        return bitmap
    }

    fun Int.toBitmap(context: Context, size: Int, @Suppress("UNUSED_PARAMETER") padding: Float): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        return bitmap
    }

    fun Int.toBitmap(context: Context, size: Int, alpha: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.alpha = alpha
        drawable?.draw(canvas)
        return bitmap
    }

    fun Int.toBitmapKeepingSize(context: Context, incrementFactor: Int, alpha: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, this)
        val intrinsicWidth = drawable!!.intrinsicWidth * incrementFactor
        val intrinsicHeight = drawable.intrinsicHeight * incrementFactor
        drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.alpha = alpha
        drawable.draw(canvas)
        return bitmap
    }

    fun Int.toBitmapKeepingSize(context: Context, @IntRange(from = 1, to = 10) incrementFactor: Int): Bitmap {
        val vectorDrawable = ContextCompat.getDrawable(context, this)
        val intrinsicWidth = vectorDrawable!!.intrinsicWidth * incrementFactor
        val intrinsicHeight = vectorDrawable.intrinsicHeight * incrementFactor
        vectorDrawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        vectorDrawable.draw(Canvas(bitmap))
        return bitmap
    }
}
