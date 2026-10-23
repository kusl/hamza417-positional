package app.simple.positional.behaviors

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

class OverScrollBehavior(context: Context, attributeSet: AttributeSet) :
        CoordinatorLayout.Behavior<View>(context, attributeSet) {
    private val animationObjects = arrayListOf<SpringAnimation>()

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        val group = target as ViewGroup
        val count = group.childCount

        for (animation in animationObjects) {
            animation.cancel()
        }

        for (i in 0 until count) {
            with(group.getChildAt(i)) {
                clearAnimation()
                overScrollY = translationY
            }
        }

        animationObjects.clear()
        return true
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        overScrollY -= dyUnconsumed / OVER_SCROLL_AREA
        val group = target as ViewGroup
        val count = group.childCount
        for (i in 0 until count) {
            val view = group.getChildAt(i)
            view.translationY = overScrollY
        }
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, type: Int) {
        moveToDefPosition(target)
    }

    private fun moveToDefPosition(target: View) {
        val group = target as ViewGroup
        val count = group.childCount
        for (i in 0 until count) {
            val view = group.getChildAt(i)

            val springAnimation = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y)
            springAnimation.spring = SpringForce()
                .setFinalPosition(0f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_LOW)

            springAnimation.start()

            animationObjects.add(springAnimation)
        }
    }

    companion object {
        private const val OVER_SCROLL_AREA = 1.5F
        private var overScrollY = 0F
    }
}
