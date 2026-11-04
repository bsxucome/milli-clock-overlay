package com.bsxu.milliclock;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** A native modal sheet shown only after a start request needs overlay permission. */
public final class OverlayPermissionSheet extends DialogFragment {
    public interface Listener {
        void onOverlaySettingsRequested();
        void onOverlayPermissionDeferred();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle(R.string.permission_prompt_title);
        dialog.setContentView(R.layout.overlay_permission_sheet);
        dialog.findViewById(R.id.permission_sheet).setClipToOutline(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.findViewById(R.id.permission_settings_button).setOnClickListener(view -> {
            Listener listener = (Listener) getActivity();
            dismiss();
            listener.onOverlaySettingsRequested();
        });
        dialog.findViewById(R.id.permission_later_button).setOnClickListener(view -> {
            ((Listener) getActivity()).onOverlayPermissionDeferred();
            dismiss();
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.BOTTOM);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.22f);
        int systemUi = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(getActivity().getColor(R.color.window_background));
            systemUi |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(systemUi);
        int width = getResources().getDisplayMetrics().widthPixels;
        int availableHeight = getActivity().findViewById(android.R.id.content).getHeight();
        if (availableHeight == 0) {
            availableHeight = getResources().getDisplayMetrics().heightPixels;
        }
        int maxHeight = Math.round(availableHeight * 0.9f);
        View content = dialog.findViewById(R.id.permission_sheet);
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                Math.min(content.getMeasuredHeight(), maxHeight));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ((Listener) getActivity()).onOverlayPermissionDeferred();
        super.onCancel(dialog);
    }
}
