package com.codex.milliclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ClockOverlayService extends Service {
    private static final String CHANNEL_ID = "clock_overlay_channel";
    private static final int NOTIFICATION_ID = 7;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter TIME_MAIN_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.", Locale.US);
    private static final DateTimeFormatter MILLIS_FORMATTER =
            DateTimeFormatter.ofPattern("SSS", Locale.US);

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams layoutParams;
    private TextView dateText;
    private TextView timeText;
    private TextView millisText;
    private TextView closeButton;
    private boolean closeButtonVisible = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideCloseButtonRunnable = () -> {
        closeButtonVisible = false;
        if (closeButton != null) {
            closeButton.setVisibility(View.GONE);
        }
    };

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            updateClock();
            if (overlayView != null) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        createNotificationChannel();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        createOverlay();
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
        mainHandler.removeCallbacks(hideCloseButtonRunnable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int paddingHorizontal = dp(12);
        int paddingVertical = dp(3);
        int gapSmall = dp(0);
        container.setPadding(
                paddingHorizontal,
                paddingVertical,
                paddingHorizontal,
                paddingVertical
        );

        GradientDrawable background = new GradientDrawable();
        background.setColor(0x4D000000);
        background.setCornerRadius(dp(10));
        background.setStroke(dp(1), 0x26FFFFFF);
        container.setBackground(background);
        container.setElevation(dp(4));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        dateText = new TextView(this);
        dateText.setTextColor(0xD9FFFFFF);
        dateText.setTextSize(12f);
        dateText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        dateText.setLetterSpacing(0.04f);
        dateText.setIncludeFontPadding(false);
        dateText.setShadowLayer(6f, 0f, 0f, 0xAA000000);

        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        headerRow.addView(dateText, dateParams);

        closeButton = new TextView(this);
        closeButton.setText("\u00D7");
        closeButton.setTextColor(0xF2FFFFFF);
        closeButton.setTextSize(15f);
        closeButton.setTypeface(Typeface.DEFAULT_BOLD);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setPadding(dp(8), dp(2), dp(8), dp(2));
        closeButton.setIncludeFontPadding(false);
        closeButton.setShadowLayer(6f, 0f, 0f, 0xAA000000);
        closeButton.setVisibility(View.GONE);

        GradientDrawable closeBackground = new GradientDrawable();
        closeBackground.setColor(0x18000000);
        closeBackground.setCornerRadius(dp(9));
        closeBackground.setStroke(dp(1), 0x22FFFFFF);
        closeButton.setBackground(closeBackground);
        closeButton.setOnClickListener(v -> stopSelf());

        headerRow.addView(closeButton);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);

        timeText = new TextView(this);
        timeText.setTextColor(0xFFFFFFFF);
        timeText.setTextSize(23f);
        timeText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        timeText.setLetterSpacing(0.01f);
        timeText.setIncludeFontPadding(false);
        timeText.setShadowLayer(8f, 0f, 0f, 0xCC000000);

        millisText = new TextView(this);
        millisText.setTextColor(0xFFFF4D4D);
        millisText.setTextSize(23f);
        millisText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        millisText.setIncludeFontPadding(false);
        millisText.setShadowLayer(8f, 0f, 0f, 0xCC000000);

        LinearLayout.LayoutParams millisParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        millisParams.leftMargin = dp(2);
        timeRow.addView(timeText);
        timeRow.addView(millisText, millisParams);

        container.addView(headerRow);
        LinearLayout.LayoutParams timeRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeRowParams.topMargin = gapSmall;
        container.addView(timeRow, timeRowParams);

        container.setOnClickListener(v -> {
            closeButtonVisible = !closeButtonVisible;
            closeButton.setVisibility(closeButtonVisible ? View.VISIBLE : View.GONE);
            mainHandler.removeCallbacks(hideCloseButtonRunnable);
            if (closeButtonVisible) {
                mainHandler.postDelayed(hideCloseButtonRunnable, 2200L);
            }
        });
        container.setOnTouchListener(new DragTouchListener());

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = dp(16);
        layoutParams.y = dp(96);

        overlayView = container;
        windowManager.addView(overlayView, layoutParams);
        updateClock();
    }

    private void updateClock() {
        long epochMillis = System.currentTimeMillis();
        ZonedDateTime dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
        dateText.setText(dateTime.format(DATE_FORMATTER));
        timeText.setText(dateTime.format(TIME_MAIN_FORMATTER));
        millisText.setText(dateTime.format(MILLIS_FORMATTER));
    }

    private Notification buildNotification() {
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.notification_channel_description));

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private final class DragTouchListener implements View.OnTouchListener {
        private int startX;
        private int startY;
        private float touchX;
        private float touchY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = layoutParams.x;
                    startY = layoutParams.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    layoutParams.x = startX + Math.round(event.getRawX() - touchX);
                    layoutParams.y = startY + Math.round(event.getRawY() - touchY);
                    if (windowManager != null && overlayView != null) {
                        windowManager.updateViewLayout(overlayView, layoutParams);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}
