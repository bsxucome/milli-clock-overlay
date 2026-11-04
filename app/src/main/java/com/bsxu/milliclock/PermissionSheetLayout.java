package com.bsxu.milliclock;

import android.content.Context;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Fits the permission copy and actions while keeping each step at its natural content height. */
public final class PermissionSheetLayout extends LinearLayout {
    private final TextPaint measuringPaint = new TextPaint();

    public PermissionSheetLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        TextView details = findViewById(R.id.permission_step_details);
        LinearLayout actions = findViewById(R.id.permission_actions);
        Button later = findViewById(R.id.permission_later_button);
        Button settings = findViewById(R.id.permission_settings_button);
        View primary = findViewById(R.id.permission_primary_container);
        float density = getResources().getDisplayMetrics().density;
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int gap = Math.round(12 * density);
        // Measure the actual labels instead of switching orientation at a fixed screen width.
        int buttonWidth = Math.max(requiredWidth(later), requiredWidth(settings));
        boolean stacked = availableWidth < buttonWidth * 2 + gap;
        actions.setOrientation(stacked ? VERTICAL : HORIZONTAL);
        configureAction(later, stacked, 0);
        configureAction(primary, stacked, gap);

        int buttonMinimum = Math.round(50 * density);
        later.setMinimumHeight(buttonMinimum);
        settings.setMinimumHeight(buttonMinimum);
        details.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Keep the complete explanation on one line with a small secondary-type adjustment.
        // Large accessibility text can still wrap instead of being clipped or shrunk away.
        float desiredWidth = Layout.getDesiredWidth(details.getText(), details.getPaint());
        int textWidth = details.getMeasuredWidth()
                - details.getCompoundPaddingLeft() - details.getCompoundPaddingRight();
        float minimumSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                getResources().getDisplayMetrics());
        if (desiredWidth > textWidth && textWidth > 0) {
            measuringPaint.set(details.getPaint());
            measuringPaint.setTextSize(minimumSize);
            int targetWidth = textWidth - 1;
            if (Layout.getDesiredWidth(details.getText(), measuringPaint) <= targetWidth) {
                // Font hinting rounds glyph widths: proportional scaling can still overflow.
                // Search using the phone's actual font measurements at each candidate size.
                float low = minimumSize;
                float high = details.getTextSize();
                for (int attempt = 0; attempt < 16; attempt++) {
                    float candidate = (low + high) / 2;
                    measuringPaint.setTextSize(candidate);
                    if (Layout.getDesiredWidth(details.getText(), measuringPaint) <= targetWidth) {
                        low = candidate;
                    } else {
                        high = candidate;
                    }
                }
                details.setTextSize(TypedValue.COMPLEX_UNIT_PX, low);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        int buttonHeight = Math.max(later.getMeasuredHeight(), settings.getMeasuredHeight());
        later.setMinimumHeight(buttonHeight);
        settings.setMinimumHeight(buttonHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int requiredWidth(Button button) {
        return Math.max(button.getMinimumWidth(), (int) Math.ceil(
                Layout.getDesiredWidth(button.getText(), button.getPaint()))
                + button.getCompoundPaddingLeft() + button.getCompoundPaddingRight());
    }

    private void configureAction(View action, boolean stacked, int gap) {
        LayoutParams params = (LayoutParams) action.getLayoutParams();
        int width = stacked ? LayoutParams.MATCH_PARENT : 0;
        float weight = stacked ? 0 : 1;
        int start = stacked ? 0 : gap;
        int top = stacked ? gap : 0;
        if (params.width != width || params.weight != weight
                || params.getMarginStart() != start || params.topMargin != top) {
            params.width = width;
            params.weight = weight;
            params.setMarginStart(start);
            params.topMargin = top;
            action.setLayoutParams(params);
        }
    }
}
