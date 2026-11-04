package com.bsxu.milliclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class AutomationReceiver extends BroadcastReceiver {
    public static final String ACTION_START_OVERLAY = "com.bsxu.milliclock.action.START_OVERLAY";
    public static final String ACTION_STOP_OVERLAY = "com.bsxu.milliclock.action.STOP_OVERLAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP_OVERLAY.equals(action)) {
            context.stopService(new Intent(context, ClockOverlayService.class));
            return;
        }
        if (!ACTION_START_OVERLAY.equals(action)) {
            return;
        }

        if (!Settings.canDrawOverlays(context)) {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            launchIntent.putExtra(MainActivity.EXTRA_AUTO_START_OVERLAY, true);
            context.startActivity(launchIntent);
            return;
        }

        Intent serviceIntent = new Intent(context, ClockOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
