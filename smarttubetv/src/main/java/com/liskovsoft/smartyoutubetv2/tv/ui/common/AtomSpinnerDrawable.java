package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Loading spinner after loading.dev's "Atom": a thin circle with three rings tumbling inside it.
 * The same drawing as the phone app's AtomSpinnerView, as a drawable so it can stand in for the
 * indeterminate drawable of the ProgressBar views the TV screens already use.
 * <p>
 * The web version tilts each ring flat (rotateX 90deg), spins it around the view axis and leans
 * its orbit by 0, 60 and 120 degrees, all a third of a turn apart. Seen straight on, a ring turned
 * by angle a is an ellipse as tall as the circle and |cos a| as wide, leaned by its orbit angle,
 * which is what this draws, without any 3D.
 */
public class AtomSpinnerDrawable extends Drawable implements Animatable {
    /** One full turn of each ring (linear); a little calmer than the web default of 1000ms */
    private static final long TURN_MS = 1500;
    private static final int RINGS = 3;
    /** Stroke widths relative to the spinner size, as in the web CSS */
    private static final float SHELL_STROKE = 0.055f;
    private static final float RING_STROKE = 0.045f;

    private final Paint mShellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mOval = new RectF();
    private boolean mIsRunning;

    public AtomSpinnerDrawable(int color) {
        mShellPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mShellPaint.setColor(color);
        mRingPaint.setColor(color);
    }

    /**
     * @return null when the current theme (color scheme) doesn't ask for the atom spinner
     */
    @Nullable
    public static AtomSpinnerDrawable fromTheme(Context context) {
        TypedValue value = new TypedValue();

        if (!context.getTheme().resolveAttribute(R.attr.loadingSpinnerColor, value, true) || value.resourceId == 0) {
            return null;
        }

        return new AtomSpinnerDrawable(ContextCompat.getColor(context, value.resourceId));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float size = Math.min(bounds.width(), bounds.height());

        if (size <= 0) {
            return;
        }

        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();

        // Borders sit inside the box on the web, so the stroke centre is half a stroke in
        float shellStroke = size * SHELL_STROKE;
        float shellRadius = size / 2 - shellStroke / 2;
        mShellPaint.setStrokeWidth(shellStroke);
        canvas.drawCircle(cx, cy, shellRadius, mShellPaint);

        float ringStroke = size * RING_STROKE;
        float ringRadius = size / 2 - ringStroke / 2;
        mRingPaint.setStrokeWidth(ringStroke);

        double turn = (SystemClock.uptimeMillis() % TURN_MS) / (double) TURN_MS * 2 * Math.PI;

        for (int i = 0; i < RINGS; i++) {
            // Ring i starts i thirds of a turn back (the web staggers them with negative delays)
            double angle = turn - i * 2 * Math.PI / RINGS;
            float halfWidth = (float) Math.abs(Math.cos(angle)) * ringRadius;

            canvas.save();
            canvas.rotate(i * 180f / RINGS, cx, cy);
            mOval.set(cx - halfWidth, cy - ringRadius, cx + halfWidth, cy + ringRadius);
            canvas.drawOval(mOval, mRingPaint);
            canvas.restore();
        }

        // Next frame. A hidden or detached view stops drawing, and with it the animation.
        if (mIsRunning) {
            invalidateSelf();
        }
    }

    @Override
    public void start() {
        if (!mIsRunning) {
            mIsRunning = true;
            invalidateSelf();
        }
    }

    @Override
    public void stop() {
        mIsRunning = false;
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void setAlpha(int alpha) {
        mShellPaint.setAlpha(alpha);
        mRingPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mShellPaint.setColorFilter(colorFilter);
        mRingPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
