package id.emes.exambrowser;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ScannerOverlayView extends View {

    private final Paint overlayPaint  = new Paint();
    private final Paint borderPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint erasePaint    = new Paint();
    private final RectF frameRect     = new RectF();

    private float scanLineY   = 0f;
    private boolean success   = false;
    private ValueAnimator animator;

    private static final int CORNER_LEN   = 60;
    private static final int CORNER_WIDTH = 6;
    private static final float FRAME_RATIO = 0.7f;

    public ScannerOverlayView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        overlayPaint.setColor(Color.parseColor("#BB000000"));
        overlayPaint.setStyle(Paint.Style.FILL);

        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        borderPaint.setColor(Color.parseColor("#44FFFFFF"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f);

        cornerPaint.setColor(Color.parseColor("#E8A020"));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(CORNER_WIDTH);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        linePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float size = Math.min(w, h) * FRAME_RATIO;
        float cx = w / 2f, cy = h / 2f;
        frameRect.set(cx - size/2, cy - size/2, cx + size/2, cy + size/2);
        scanLineY = frameRect.top;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();

        // Semi-transparent overlay
        c.drawRect(0, 0, w, h, overlayPaint);
        // Clear centre hole
        c.drawRoundRect(frameRect, 16, 16, erasePaint);
        // Subtle border on hole
        c.drawRoundRect(frameRect, 16, 16, borderPaint);

        // Corner brackets
        drawCorners(c);

        // Scan line (gradient-like via alpha)
        if (!success) {
            drawScanLine(c);
        } else {
            // Success fill — green tint
            Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
            sp.setColor(Color.parseColor("#8800C853"));
            c.drawRoundRect(frameRect, 16, 16, sp);
            Paint sp2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            sp2.setColor(Color.parseColor("#00C853"));
            sp2.setStyle(Paint.Style.STROKE);
            sp2.setStrokeWidth(4f);
            c.drawRoundRect(frameRect, 16, 16, sp2);
        }
    }

    private void drawCorners(Canvas c) {
        float l = frameRect.left, t = frameRect.top,
              r = frameRect.right, b = frameRect.bottom;
        int cl = CORNER_LEN;
        // Top-left
        c.drawLine(l, t + cl, l, t, cornerPaint);
        c.drawLine(l, t, l + cl, t, cornerPaint);
        // Top-right
        c.drawLine(r - cl, t, r, t, cornerPaint);
        c.drawLine(r, t, r, t + cl, cornerPaint);
        // Bottom-left
        c.drawLine(l, b - cl, l, b, cornerPaint);
        c.drawLine(l, b, l + cl, b, cornerPaint);
        // Bottom-right
        c.drawLine(r - cl, b, r, b, cornerPaint);
        c.drawLine(r, b - cl, r, b, cornerPaint);
    }

    private void drawScanLine(Canvas c) {
        // Line with soft glow effect (3 layers)
        float[] alphas = {40, 120, 220};
        float[] heights = {12f, 4f, 1.5f};
        for (int i = 0; i < 3; i++) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.argb((int) alphas[i], 232, 160, 32));
            RectF lr = new RectF(frameRect.left + 2, scanLineY - heights[i]/2,
                                 frameRect.right - 2, scanLineY + heights[i]/2);
            c.drawRoundRect(lr, 4, 4, p);
        }
    }

    public void startAnimation() {
        animator = ValueAnimator.ofFloat(frameRect.top, frameRect.bottom);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            scanLineY = (float) a.getAnimatedValue();
            // Keep line inside frame
            if (scanLineY < frameRect.top) scanLineY = frameRect.top;
            if (scanLineY > frameRect.bottom) scanLineY = frameRect.bottom;
            invalidate();
        });
        animator.start();
    }

    public void stopAnimation() {
        if (animator != null) { animator.cancel(); animator = null; }
    }

    public void showSuccess() {
        stopAnimation();
        success = true;
        invalidate();
    }
}
