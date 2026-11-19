package com.bsxu.milliclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OverlayPermissionSheet.Listener {
    public static final String EXTRA_AUTO_START_OVERLAY = "autoStartOverlay";
    private static final String STATE_AUTO_START = "auto_start";
    private static final String STATE_AUTO_HANDLED = "auto_start_handled";
    private static final String STATE_WAITING_FOR_SETTINGS = "waiting_for_overlay_settings";
    private static final String PERMISSION_SHEET_TAG = "overlay_permission_sheet";

    private TextView statusText;
    private PanelToggleButton toggleButton;
    private boolean togglePending;
    private boolean autoStartOverlay;
    private boolean autoStartHandled;
    private boolean waitingForOverlaySettings;
    private final BroadcastReceiver overlayStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            togglePending = false;
            updateOverlayControls();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        autoStartOverlay = getIntent().getBooleanExtra(EXTRA_AUTO_START_OVERLAY, false);
        if (savedInstanceState != null) {
            autoStartOverlay = savedInstanceState.getBoolean(STATE_AUTO_START);
            autoStartHandled = savedInstanceState.getBoolean(STATE_AUTO_HANDLED);
            waitingForOverlaySettings = savedInstanceState.getBoolean(STATE_WAITING_FOR_SETTINGS);
        }

        statusText = findViewById(R.id.status_text);
        toggleButton = findViewById(R.id.toggle_button);
        styleUsageTip(findViewById(R.id.usage_tip_drag_text), R.string.usage_tip_drag);
        styleUsageTip(findViewById(R.id.usage_tip_close_text), R.string.usage_tip_close);

        toggleButton.setOnClickListener(v -> {
            autoStartOverlay = false;
            if (ClockOverlayService.isOverlayRunning()) {
                togglePending = stopService(new Intent(this, ClockOverlayService.class));
                updateOverlayControls();
            } else {
                requestOverlayStart();
            }
        });
    }

    private void styleUsageTip(TextView view, int textResource) {
        SpannableString text = new SpannableString(getText(textResource));
        for (Annotation annotation : text.getSpans(0, text.length(), Annotation.class)) {
            if (!"emphasis".equals(annotation.getKey()) || !"action".equals(annotation.getValue())) {
                continue;
            }
            int start = text.getSpanStart(annotation);
            int end = text.getSpanEnd(annotation);
            text.setSpan(new TypefaceSpan("sans-serif-medium"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(getColor(R.color.home_action_ink)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        view.setText(text);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ClockOverlayService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerLegacyStateReceiver(filter);
        }
    }

    // Android 8-12 have no export flags. No broadcast data is trusted: we read local service state.
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerLegacyStateReceiver(IntentFilter filter) {
        registerReceiver(overlayStateReceiver, filter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(overlayStateReceiver);
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        autoStartOverlay = intent != null && intent.getBooleanExtra(EXTRA_AUTO_START_OVERLAY, false);
        autoStartHandled = false;
        waitingForOverlaySettings = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean(STATE_AUTO_START, autoStartOverlay);
        state.putBoolean(STATE_AUTO_HANDLED, autoStartHandled);
        state.putBoolean(STATE_WAITING_FOR_SETTINGS, waitingForOverlaySettings);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        togglePending = false;
        updateOverlayControls();
        if (waitingForOverlaySettings) {
            // Consume this request once, including when settings returned without permission.
            waitingForOverlaySettings = false;
            if (Settings.canDrawOverlays(this)) {
                completeOverlayStart();
            } else {
                autoStartOverlay = false;
            }
            return;
        }
        if (autoStartOverlay && !autoStartHandled) {
            autoStartHandled = true;
            requestOverlayStart();
        }
    }

    private void updateOverlayControls() {
        boolean granted = Settings.canDrawOverlays(this);
        boolean running = ClockOverlayService.isOverlayRunning();
        statusText.setVisibility(granted || running ? View.VISIBLE : View.GONE);
        statusText.setText(granted ? R.string.overlay_permission_granted : R.string.overlay_permission_missing);
        toggleButton.setRunning(running);
        toggleButton.setEnabled(!togglePending);
        toggleButton.setAlpha(toggleButton.isEnabled() ? 1f : 0.55f);
    }

    private void requestOverlayStart() {
        if (Settings.canDrawOverlays(this)) {
            completeOverlayStart();
        } else {
            showOverlayPermissionSheet();
        }
    }

    private void completeOverlayStart() {
        if (startOverlay() && autoStartOverlay) {
            finish();
        }
    }

    private void showOverlayPermissionSheet() {
        if (getFragmentManager().findFragmentByTag(PERMISSION_SHEET_TAG) == null) {
            new OverlayPermissionSheet().show(getFragmentManager(), PERMISSION_SHEET_TAG);
        }
    }

    @Override
    public void onOverlayPermissionDeferred() {
        autoStartOverlay = false;
        waitingForOverlaySettings = false;
    }

    @Override
    public void onOverlaySettingsRequested() {
        waitingForOverlaySettings = true;
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException exception) {
            waitingForOverlaySettings = false;
            autoStartOverlay = false;
            Toast.makeText(this, R.string.overlay_settings_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private boolean startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionSheet();
            return false;
        }
        togglePending = true;
        updateOverlayControls();
        Intent intent = new Intent(this, ClockOverlayService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            requestNotificationPermissionIfNeeded();
            return true;
        } catch (IllegalStateException | SecurityException exception) {
            togglePending = false;
            updateOverlayControls();
            Toast.makeText(this, R.string.overlay_start_failed, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (!autoStartOverlay
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("notification_permission_requested", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("notification_permission_requested", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }
}
