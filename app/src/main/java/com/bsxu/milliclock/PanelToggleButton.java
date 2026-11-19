package com.bsxu.milliclock;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The entire row toggles the overlay, and reflects the service's confirmed state. */
public final class PanelToggleButton extends LinearLayout {
    private TextView label;

    public PanelToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        label = findViewById(R.id.toggle_label);
    }

    public void setRunning(boolean running) {
        setSelected(running);
        label.setText(running ? R.string.stop_overlay : R.string.start_overlay);
        setContentDescription(label.getText());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setStateDescription(getContext().getText(
                    running ? R.string.overlay_running : R.string.overlay_stopped));
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
        info.setCheckable(true);
        info.setChecked(isSelected());
    }
}
