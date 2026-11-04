package com.bsxu.milliclock;

import android.annotation.SuppressLint;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.Button;

/** A raised ring whose checked appearance follows the overlay service. */
public final class RingToggleButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF powerBounds = new RectF();
    private StaticLayout labelLayout;
    private LinearGradient ringGradient;
    private LinearGradient innerBevelGradient;
    private String label;
    private boolean running;
    private ValueAnimator pressAnimator;
    private float pressScale = 1f;
    private float pressTarget = 1f;

    public RingToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(true);
        // The ring draws its own circular keyboard focus indicator.
        setDefaultFocusHighlightEnabled(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        labelPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16f, getResources().getDisplayMetrics()));
        labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        updateLabel();
    }

    public void setRunning(boolean running) {
        if (this.running == running) {
            return;
        }
        this.running = running;
        updateLabel();
        buildGradients();
        invalidate();
    }

    private void updateLabel() {
        label = getContext().getString(running ? R.string.stop_overlay : R.string.start_overlay);
        setContentDescription(label);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setStateDescription(getContext().getString(
                    running ? R.string.overlay_running : R.string.overlay_stopped));
        }
        buildLabelLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 244dp ring plus room for the cast shadow and keyboard focus indicator.
        int desiredSize = Math.round(dp(268));
        int width = resolveSize(desiredSize, widthMeasureSpec);
        int height = resolveSize(Math.min(width, desiredSize), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        buildLabelLayout();
        buildGradients();
    }

    private float outerRadius() {
        return Math.max(0f, (Math.min(getWidth(), getHeight()) - dp(24)) / 2f);
    }

    private void buildLabelLayout() {
        int width = Math.round((outerRadius() * 2f - dp(28)) * 0.8f);
        if (width <= 0 || label == null) {
            return;
        }
        labelLayout = StaticLayout.Builder.obtain(label, 0, label.length(), labelPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .build();
    }

    private void buildGradients() {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = outerRadius();
        if (radius <= 0f) {
            return;
        }
        float innerRadius = Math.max(0f, radius - dp(16));
        ringGradient = new LinearGradient(
                centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                running
                        ? new int[]{getContext().getColor(R.color.ring_active_highlight),
                                getContext().getColor(R.color.ring_active_mid),
                                getContext().getColor(R.color.ring_active_shadow)}
                        : new int[]{getContext().getColor(R.color.ring_inactive_highlight),
                                getContext().getColor(R.color.ring_inactive_mid),
                                getContext().getColor(R.color.ring_inactive_shadow)},
                running ? new float[]{0f, 0.48f, 1f} : new float[]{0.08f, 0.52f, 1f},
                Shader.TileMode.CLAMP);
        innerBevelGradient = new LinearGradient(
                centerX, centerY - innerRadius, centerX, centerY + innerRadius,
                getContext().getColor(running ? R.color.ring_active_inner_shadow
                        : R.color.ring_inactive_inner_shadow),
                0xBFFFFFFF, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = outerRadius();
        float innerRadius = Math.max(0f, radius - dp(16));
        if (radius <= 0f) {
            return;
        }

        canvas.save();
        canvas.scale(pressScale, pressScale, centerX, centerY);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(ringGradient);
        // Short layered shadows give the ring depth without multiple hard outlines.
        paint.setShadowLayer(dp(2), 0, dp(2), getContext().getColor(R.color.ring_shadow_ambient));
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setShadowLayer(dp(1), 0, dp(1), getContext().getColor(R.color.ring_shadow_edge));
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setShader(null);
        paint.setColor(getContext().getColor(R.color.ring_shadow_edge));
        canvas.drawCircle(centerX, centerY, radius - dp(0.5f), paint);

        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getContext().getColor(R.color.window_background));
        canvas.drawCircle(centerX, centerY, innerRadius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(isPressed() && isEnabled() ? 5 : 3));
        paint.setShader(innerBevelGradient);
        canvas.drawCircle(centerX, centerY, Math.max(0f, innerRadius - dp(1.5f)), paint);
        paint.setShader(null);
        paint.setStrokeWidth(dp(1));
        paint.setColor(getContext().getColor(running ? R.color.ring_active_inner_border
                : R.color.ring_inactive_inner_border));
        canvas.drawCircle(centerX, centerY, Math.max(0f, innerRadius - dp(0.5f)), paint);

        int foreground = getContext().getColor(running ? R.color.ring_active_foreground
                : R.color.ring_inactive_foreground);
        labelPaint.setColor(foreground);
        if (labelLayout != null) {
            float iconSize = dp(32);
            float labelHeight = Math.max(dp(26), labelLayout.getHeight());
            float contentTop = centerY - (iconSize + dp(20) + labelHeight) / 2f;
            float iconCenterY = contentTop + iconSize / 2f;
            float iconRadius = dp(12);
            powerBounds.set(centerX - iconRadius, iconCenterY - iconRadius,
                    centerX + iconRadius, iconCenterY + iconRadius);
            paint.setColor(foreground);
            paint.setStrokeWidth(dp(2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawArc(powerBounds, -50f, 280f, false, paint);
            canvas.drawLine(centerX, iconCenterY - dp(16), centerX, iconCenterY, paint);
            canvas.save();
            canvas.translate(centerX - labelLayout.getWidth() / 2f,
                    contentTop + iconSize + dp(20) + (labelHeight - labelLayout.getHeight()) / 2f);
            labelLayout.draw(canvas);
            canvas.restore();
        }

        if (isFocused()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(foreground);
            paint.setStrokeWidth(dp(1.5f));
            canvas.drawCircle(centerX, centerY, radius + dp(4), paint);
        }
        canvas.restore();
    }

    @Override
    // View.onTouchEvent dispatches performClick; this override only excludes the ring's corners.
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = event.getX() - getWidth() / 2f;
            float y = event.getY() - getHeight() / 2f;
            float radius = outerRadius();
            if (x * x + y * y > radius * radius) {
                return false;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        // View can dispatch state changes before this class's fields are initialized.
        if (labelPaint != null) {
            updatePressFeedback();
        }
        invalidate();
    }

    private void updatePressFeedback() {
        // A static inner-bevel cue remains when system animations are disabled.
        boolean animate = isAttachedToWindow() && ValueAnimator.areAnimatorsEnabled();
        float target = animate && isPressed() && isEnabled() ? 0.96f : 1f;
        if (target == pressTarget && animate) {
            return;
        }
        pressTarget = target;
        if (pressAnimator != null) {
            pressAnimator.cancel();
        }
        if (!animate) {
            pressScale = target;
            return;
        }
        pressAnimator = ValueAnimator.ofFloat(pressScale, target);
        pressAnimator.setDuration(150);
        pressAnimator.setInterpolator(new PathInterpolator(0f, 0f, 0.58f, 1f));
        pressAnimator.addUpdateListener(animation -> {
            pressScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pressAnimator != null) {
            pressAnimator.cancel();
            pressAnimator = null;
        }
        pressScale = pressTarget = 1f;
        super.onDetachedFromWindow();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
        info.setCheckable(true);
        info.setChecked(running);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
