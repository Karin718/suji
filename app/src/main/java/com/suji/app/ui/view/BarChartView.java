package com.suji.app.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.suji.app.R;

/** 一个极简的柱状图，用来显示最近 7 天的复习量。 */
public class BarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int[] values = new int[]{0, 0, 0, 0, 0, 0, 0};
    private String[] labels = new String[]{"", "", "", "", "", "", ""};

    public BarChartView(Context context) {
        super(context);
        init(context);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        barPaint.setColor(ContextCompat.getColor(context, R.color.brand));
        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        float density = getResources().getDisplayMetrics().density;
        valuePaint.setTextSize(11 * density);
        labelPaint.setTextSize(11 * density);
    }

    public void setData(int[] values, String[] labels) {
        if (values != null && values.length == 7) {
            this.values = values;
        }
        if (labels != null && labels.length == 7) {
            this.labels = labels;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float padTop = 22 * density;
        float padBottom = 24 * density;

        int max = 1;
        for (int v : values) {
            max = Math.max(max, v);
        }

        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom() - padTop - padBottom;
        if (width <= 0 || height <= 0) {
            return;
        }
        float slot = width / 7f;
        float barWidth = Math.min(slot * 0.5f, 26 * density);
        float radius = barWidth / 2f;

        for (int i = 0; i < 7; i++) {
            float cx = getPaddingLeft() + slot * i + slot / 2f;
            float ratio = values[i] / (float) max;
            float barHeight = Math.max(values[i] > 0 ? 6 * density : 3 * density, height * ratio);
            float bottom = getPaddingTop() + padTop + height;
            float top = bottom - barHeight;

            barPaint.setAlpha(values[i] > 0 ? 255 : 60);
            rect.set(cx - barWidth / 2f, top, cx + barWidth / 2f, bottom);
            canvas.drawRoundRect(rect, radius, radius, barPaint);

            if (values[i] > 0) {
                canvas.drawText(String.valueOf(values[i]), cx, top - 6 * density, valuePaint);
            }
            canvas.drawText(labels[i], cx, bottom + 16 * density, labelPaint);
        }
    }
}
