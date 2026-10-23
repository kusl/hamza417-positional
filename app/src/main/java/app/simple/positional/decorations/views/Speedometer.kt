package app.simple.positional.decorations.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import app.simple.positional.R
import app.simple.positional.constants.SpeedometerConstants
import app.simple.positional.util.BitmapHelper
import app.simple.positional.util.BitmapHelper.toBitmap
import app.simple.positional.util.ImageLoader.loadImage
import com.google.android.material.animation.ArgbEvaluatorCompat

class Speedometer constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private var lastColor = Color.parseColor("#363636")
    private var isGradientNeedle = false
    private var firstGradientColor = 0
    private var colorfulNeedle = false
    private var whichImage = -1

    private var needleAngleCompensator = 0F

    private val needle: ImageView
    private val dial: ImageView

    private var objectAnimator: ObjectAnimator? = null
    private var colorAnimation: ValueAnimator? = null
    private var springAnimation: SpringAnimation? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.speedometer, this, true)
        needle = view.findViewById(R.id.speedometer_needle)
        dial = view.findViewById(R.id.speedometer_dial)

        context.theme.obtainStyledAttributes(attrs, R.styleable.Speedometer, 0, 0).apply {
            try {
                isGradientNeedle = getBoolean(R.styleable.Speedometer_gradient_needle, false)
                colorfulNeedle = getBoolean(R.styleable.Speedometer_colorful_needle, false)
                firstGradientColor = getColor(R.styleable.Speedometer_first_gradient_color, Color.parseColor("#009bce"))
                whichImage = R.drawable.speedometer_dial_normal_range
            } finally {
                recycle()
            }
        }
    }

    fun setSpeedValue(value: Float) {
        println(value)
        setDial(value * 2)
        rotateNeedle(value * 2)
    }

    private fun setDial(value: Float) {
        when {
            value <= 300F -> {
                if (whichImage != R.drawable.speedometer_dial_normal_range) {
                    loadImage(R.drawable.speedometer_dial_normal_range, dial, context, 0)
                    whichImage = R.drawable.speedometer_dial_normal_range
                    needleAngleCompensator = 0F
                }
            }
            value in 300.01F..600F -> {
                if (whichImage != R.drawable.speedometer_dial_medium_range) {
                    loadImage(R.drawable.speedometer_dial_medium_range, dial, context, 0)
                    whichImage = R.drawable.speedometer_dial_medium_range
                    needleAngleCompensator = 60F
                }
            }
            value in 600.01F..900F -> {
                if (whichImage != R.drawable.speedometer_dial_high_range) {
                    loadImage(R.drawable.speedometer_dial_high_range, dial, context, 0)
                    whichImage = R.drawable.speedometer_dial_high_range
                    needleAngleCompensator = 120F
                }
            }
            value >= 900.01F -> {
                if (whichImage != R.drawable.speedometer_dial_max_range) {
                    loadImage(R.drawable.speedometer_dial_max_range, dial, context, 0)
                    whichImage = R.drawable.speedometer_dial_max_range
                    needleAngleCompensator = 180F
                }
            }
        }
    }

    private fun rotateNeedle(value: Float) {
        if (springAnimation != null) {
            springAnimation!!.cancel()
        }

        springAnimation = SpringAnimation(needle, SpringAnimation.ROTATION)
        springAnimation!!.spring = SpringForce()
                .setFinalPosition(value - needleAngleCompensator)
                .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_VERY_LOW)
        springAnimation!!.setStartValue(needle.rotation)
        springAnimation!!.start()

        if (colorfulNeedle) {
            animateColorChange(value = value)
        }
    }

    private fun animateColorChange(value: Float) {
        colorAnimation = ValueAnimator.ofObject(ArgbEvaluatorCompat(), lastColor, SpeedometerConstants.getSpeedometerColor(value / 2.0))
        colorAnimation!!.duration = 1000L
        colorAnimation!!.interpolator = LinearOutSlowInInterpolator()
        colorAnimation!!.addUpdateListener { animation ->
            lastColor = animation.animatedValue as Int
            if (isGradientNeedle) {
                needle.setImageBitmap(BitmapHelper.addLinearGradient(
                        R.drawable.speedometer_needle.toBitmap(context, 500),
                        intArrayOf(animation.animatedValue as Int, firstGradientColor),
                        needle.height / 4F
                ))
            } else {
                ImageViewCompat.setImageTintList(
                        needle,
                        ColorStateList.valueOf(animation.animatedValue as Int))
            }
        }
        colorAnimation!!.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clear()
    }

    fun clear() {
        objectAnimator?.cancel()
        colorAnimation?.cancel()
        springAnimation?.cancel()
        dial.clearAnimation()
        needle.clearAnimation()
        invalidate()
    }
}
