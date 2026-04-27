package com.bsxu.milliclock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView statusText;
    private Button permissionButton;
    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        permissionButton = findViewById(R.id.permission_button);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);

        permissionButton.setOnClickListener(v -> openOverlaySettings());
        startButton.setOnClickListener(v -> startOverlay());
        stopButton.setOnClickListener(v -> stopService(new Intent(this, ClockOverlayService.class)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean granted = Settings.canDrawOverlays(this);
        statusText.setText(granted ? R.string.overlay_permission_granted : R.string.overlay_permission_missing);
        permissionButton.setVisibility(granted ? View.GONE : View.VISIBLE);
        startButton.setEnabled(granted);
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            return;
        }
        Intent intent = new Intent(this, ClockOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
