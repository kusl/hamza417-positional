package app.simple.positional.util

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import app.simple.positional.R

object ImageLoader {
    fun loadImage(resourceValue: Int, imageView: ImageView, context: Context, delay: Int) {
        val animOut: Animation = AnimationUtils.loadAnimation(context, R.anim.image_out)
        val animIn: Animation = AnimationUtils.loadAnimation(context, R.anim.image_in)

        animIn.startOffset = delay.toLong()
        animOut.startOffset = delay.toLong()

        animOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                imageView.setImageResource(resourceValue)

                animIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })

                imageView.startAnimation(animIn)
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        imageView.startAnimation(animOut)
    }

    fun setImage(resourceValue: Int, imageView: ImageView, context: Context, delay: Int) {
        val animOut: Animation = AnimationUtils.loadAnimation(context, R.anim.image_out)
        val animIn: Animation = AnimationUtils.loadAnimation(context, R.anim.image_in)

        animIn.startOffset = delay.toLong()
        animOut.startOffset = delay.toLong()

        animOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                imageView.setImageResource(resourceValue)

                animIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
                imageView.startAnimation(animIn)
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        imageView.startAnimation(animOut)
    }
}
