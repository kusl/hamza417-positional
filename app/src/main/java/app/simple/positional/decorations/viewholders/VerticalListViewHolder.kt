package app.simple.positional.decorations.viewholders

import android.view.View
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView

open class VerticalListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var currentVelocity = 0f

    val rotation: SpringAnimation = SpringAnimation(itemView, SpringAnimation.ROTATION)
            .setSpring(
                    SpringForce()
                            .setFinalPosition(0f)
                            .setDampingRatio(bouncyValue)
                            .setStiffness(stiffnessValue)
            )
            .addUpdateListener { _, _, velocity ->
                currentVelocity = velocity
            }

    val translationY: SpringAnimation = SpringAnimation(itemView, SpringAnimation.TRANSLATION_Y)
            .setSpring(
                    SpringForce()
                            .setFinalPosition(0f)
                            .setDampingRatio(bouncyValue)
                            .setStiffness(stiffnessValue)
            )

    companion object {
        const val bouncyValue = SpringForce.DAMPING_RATIO_NO_BOUNCY
        const val stiffnessValue = SpringForce.STIFFNESS_LOW
        const val TYPE_HEADER = -1
        const val TYPE_ITEM = 1
        const val TYPE_FOOTER = -2

        private const val value = 1.0f
        const val flingTranslationMagnitude = value
        const val overScrollRotationMagnitude = value
        const val overScrollTranslationMagnitude = value
    }
}
