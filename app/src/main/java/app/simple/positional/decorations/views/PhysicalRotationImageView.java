package app.simple.positional.decorations.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class PhysicalRotationImageView extends androidx.appcompat.widget.AppCompatImageView {
    static final public float TIME_DELTA_THRESHOLD = 0.25f;
    static final public float ANGLE_DELTA_THRESHOLD = 0.1f;

    static final public float INERTIA_MOMENT_DEFAULT = 0.5f;
    static final public float ALPHA_DEFAULT = 10f;
    static final public float MB_DEFAULT = 5000f;

    long time1;
    long time2;

    double angle1;
    double angle2;
    double angle0;

    double angleLastDrawn;

    boolean animationOn = false;

    float inertiaMoment = INERTIA_MOMENT_DEFAULT;
    float alpha = ALPHA_DEFAULT;
    float mB = MB_DEFAULT;

    public PhysicalRotationImageView(Context context) {
        super(context);
    }

    public PhysicalRotationImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PhysicalRotationImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (animationOn) {
            if (angleRecalculate(System.currentTimeMillis())) {
                this.setRotation(angle1 > 0 ? (float) (angle1 % 360) : (float) (360 + angle1 % 360));
            }
        } else {
            this.setRotation(angle1 > 0 ? (float) (angle1 % 360) : (float) (360 + angle1 % 360));
        }

        super.onDraw(canvas);

        if (animationOn) {
            this.invalidate();
        }
    }

    public void setPhysical(float inertiaMoment, float alpha, float mB) {
        this.inertiaMoment = inertiaMoment >= 0 ? inertiaMoment : INERTIA_MOMENT_DEFAULT;
        this.alpha = alpha >= 0 ? alpha : ALPHA_DEFAULT;
        this.mB = mB >= 0 ? mB : MB_DEFAULT;
    }

    public void rotationUpdate(final float angleNew, final boolean animate) {
        if (animate) {
            if (Math.abs(angle0 - angleNew) > ANGLE_DELTA_THRESHOLD) {
                angle0 = angleNew;
                this.invalidate();
            }
            animationOn = true;
        } else {
            angle1 = angleNew;
            angle2 = angleNew;
            angle0 = angleNew;
            angleLastDrawn = angleNew;
            this.invalidate();
            animationOn = false;
        }
    }

    protected boolean angleRecalculate(final long timeNew) {
        float deltaT1 = (timeNew - time1) / 1000f;
        if (deltaT1 > TIME_DELTA_THRESHOLD) {
            deltaT1 = TIME_DELTA_THRESHOLD;
            time1 = timeNew + Math.round(TIME_DELTA_THRESHOLD * 1000);
        }

        float deltaT2 = (time1 - time2) / 1000f;
        if (deltaT2 > TIME_DELTA_THRESHOLD) {
            deltaT2 = TIME_DELTA_THRESHOLD;
        }

        double coefI = inertiaMoment / deltaT1 / deltaT2;

        double coefAlpha = alpha / deltaT1;

        double coefk = mB * (Math.sin(Math.toRadians(angle0)) * Math.cos(Math.toRadians(angle1)) -
                (Math.sin(Math.toRadians(angle1)) * Math.cos(Math.toRadians(angle0))));

        double angleNew = (coefI * (angle1 * 2f - angle2) + coefAlpha * angle1 + coefk) / (coefI + coefAlpha);

        angle2 = angle1;
        angle1 = angleNew;
        time2 = time1;
        time1 = timeNew;

        if (Math.abs(angleLastDrawn - angle1) < ANGLE_DELTA_THRESHOLD) {
            return false;
        } else {
            angleLastDrawn = angle1;
            return true;
        }
    }

    @Override
    public void clearAnimation() {
        time1 = System.currentTimeMillis();
        rotationUpdate(getRotation(), false);
        super.clearAnimation();
    }
}
