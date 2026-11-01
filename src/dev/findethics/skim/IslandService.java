package dev.findethics.skim;

import android.accessibilityservice.AccessibilityService;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.media.session.MediaController;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;
import java.util.Objects;

public class IslandService extends AccessibilityService {
    static IslandService current;
    final Handler handler = new Handler(Looper.getMainLooper());
    WindowManager windows;
    WindowManager.LayoutParams layout;
    LinearLayout island;
    SharedPreferences prefs;
    CameraManager cameras;
    String cameraId, alertKey, alertTitle = "", alertText = "", mediaKey, signature = "";
    PendingIntent alertIntent;
    MediaController media;
    boolean expanded, charging, torch;
    int battery;
    long alertUntil;
    final Runnable tick = new Runnable() { public void run() { render(); handler.postDelayed(this, 1000); } };
    final SharedPreferences.OnSharedPreferenceChangeListener changed = (p, k) -> { signature = ""; render(); };
    final BroadcastReceiver batteryChanged = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0), scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            battery = scale > 0 ? Math.max(0, Math.min(100, level * 100 / scale)) : 0;
            charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            render();
        }
    };
    final CameraManager.TorchCallback torchChanged = new CameraManager.TorchCallback() {
        @Override public void onTorchModeChanged(String id, boolean enabled) { if (Objects.equals(cameraId, id)) { torch = enabled; render(); } }
        @Override public void onTorchModeUnavailable(String id) { if (Objects.equals(cameraId, id)) { torch = false; render(); } }
    };
    @Override public void onServiceConnected() {
        current = this;
        prefs = getSharedPreferences("island", MODE_PRIVATE);
        windows = getSystemService(WindowManager.class);
        island = new LinearLayout(this); island.setOrientation(LinearLayout.VERTICAL);
        island.setPadding(dp(16), dp(6), dp(16), dp(6));
        GradientDrawable background = new GradientDrawable(); background.setColor(Color.BLACK); background.setCornerRadius(dp(28)); island.setBackground(background);
        layout = new WindowManager.LayoutParams(dp(160), dp(36), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        layout.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        layout.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        windows.addView(island, layout);
        GestureDetector gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(android.view.MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(android.view.MotionEvent e) { expanded = !expanded; signature = ""; render(); return true; }
            @Override public void onLongPress(android.view.MotionEvent e) { performGlobalAction(GLOBAL_ACTION_HOME); }
            @Override public boolean onFling(android.view.MotionEvent a, android.view.MotionEvent b, float x, float y) {
                if (a != null && b.getY() - a.getY() > dp(24)) { performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); return true; } return false;
            }
        });
        island.setFocusable(true); island.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        island.setOnTouchListener((v, e) -> gestures.onTouchEvent(e));
        island.setOnClickListener(v -> { expanded = !expanded; signature = ""; render(); });
        prefs.registerOnSharedPreferenceChangeListener(changed);
        registerReceiver(batteryChanged, new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_EXPORTED);
        cameras = getSystemService(CameraManager.class);
        try {
            for (String id : cameras.getCameraIdList()) {
                if (Boolean.TRUE.equals(cameras.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) { cameraId = id; break; }
            }
            cameras.registerTorchCallback(torchChanged, handler);
        } catch (CameraAccessException | SecurityException ignored) { cameraId = null; }
        if (IslandListener.current != null) IslandListener.current.refresh();
        handler.post(tick);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { render(); }
    @Override public void onInterrupt() {}
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config); signature = ""; render(); }
    @Override public void onDestroy() {
        current = null; handler.removeCallbacksAndMessages(null);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(changed);
        try { unregisterReceiver(batteryChanged); } catch (IllegalArgumentException ignored) {}
        if (cameras != null) cameras.unregisterTorchCallback(torchChanged);
        if (island != null) windows.removeView(island);
        super.onDestroy();
    }
    void notification(StatusBarNotification item) {
        Notification n = item.getNotification();
        MediaSession.Token token = n.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token.class);
        if (token != null) { media = new MediaController(this, token); mediaKey = item.getKey(); }
        else if (prefs.getBoolean("notifications", true) && n.visibility != Notification.VISIBILITY_SECRET && !getSystemService(KeyguardManager.class).isKeyguardLocked()) {
            alertKey = item.getKey(); alertTitle = clean(n.extras.getCharSequence(Notification.EXTRA_TITLE));
            alertText = clean(n.extras.getCharSequence(Notification.EXTRA_TEXT)); alertIntent = n.contentIntent;
            if (alertTitle.isEmpty()) alertTitle = item.getPackageName();
            alertUntil = SystemClock.elapsedRealtime() + 8000;
        }
        signature = ""; render();
    }
    void removed(String key) { if (Objects.equals(key, alertKey)) clearNotification(); if (Objects.equals(key, mediaKey)) { media = null; mediaKey = null; } signature = ""; render(); }
    void clearNotification() { alertKey = null; alertIntent = null; alertTitle = ""; alertText = ""; alertUntil = 0; signature = ""; render(); }
    void preview() { alertTitle = "Skim Island"; alertText = "Your island is ready"; alertKey = null; alertIntent = null; alertUntil = SystemClock.elapsedRealtime() + 8000; expanded = true; signature = ""; render(); }
    // ponytail: one latest alert in memory; per-app queues only if simultaneous alerts matter.
    void render() {
        if (island == null || prefs == null) return;
        boolean locked = getSystemService(KeyguardManager.class).isKeyguardLocked() || !getSystemService(PowerManager.class).isInteractive();
        if (locked) { alertUntil = 0; alertIntent = null; alertTitle = ""; alertText = ""; expanded = false; }
        if (locked || !prefs.getBoolean("enabled", true)) { island.setVisibility(View.GONE); return; }
        island.setVisibility(View.VISIBLE);
        PlaybackState playback = media == null ? null : media.getPlaybackState();
        MediaMetadata meta = media == null ? null : media.getMetadata();
        boolean hasMedia = playback != null && (playback.getState() == PlaybackState.STATE_PLAYING || playback.getState() == PlaybackState.STATE_PAUSED);
        boolean alert = alertUntil > SystemClock.elapsedRealtime() && prefs.getBoolean("notifications", true);
        String title = alert ? alertTitle : hasMedia ? clean(meta == null ? "Music" : meta.getText(MediaMetadata.METADATA_KEY_TITLE)) : torch ? "Flashlight on" : charging ? "Charging " + battery + "%" : "Skim";
        if (title.isEmpty()) title = "Music";
        String detail = alert ? alertText : hasMedia ? clean(meta == null ? "" : meta.getText(MediaMetadata.METADATA_KEY_ARTIST)) : "Battery " + battery + "%";
        int width = Math.max(100, Math.min(280, prefs.getInt("width", 160)));
        int top = Math.max(0, Math.min(100, prefs.getInt("top", 0)));
        String next = title + "\n" + detail + expanded + hasMedia + torch + width + top + (playback == null ? 0 : playback.getState());
        if (next.equals(signature)) return;
        signature = next;
        island.removeAllViews();
        TextView label = text(title, 14); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END); island.addView(label);
        island.setContentDescription("Island: " + title + (expanded ? ", expanded" : ", tap to expand"));
        if (expanded) {
            TextView body = text(detail, 13); body.setMaxLines(3); island.addView(body);
            if (alert && alertIntent != null) button("Open notification", () -> {
                try { alertIntent.send(); expanded = false; signature = ""; render(); }
                catch (PendingIntent.CanceledException e) { clearNotification(); Toast.makeText(this, "Notification no longer available", Toast.LENGTH_SHORT).show(); }
            });
            if (hasMedia) {
                LinearLayout controls = new LinearLayout(this); island.addView(controls);
                mediaButton(controls, "Previous", android.R.drawable.ic_media_previous, () -> media.getTransportControls().skipToPrevious());
                boolean playing = playback.getState() == PlaybackState.STATE_PLAYING;
                mediaButton(controls, playing ? "Pause" : "Play", playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        () -> { if (playing) media.getTransportControls().pause(); else media.getTransportControls().play(); });
                mediaButton(controls, "Next", android.R.drawable.ic_media_next, () -> media.getTransportControls().skipToNext());
            }
            if (cameraId != null) button(torch ? "Turn flashlight off" : "Turn flashlight on", this::toggleTorch);
            button("Settings", () -> { expanded = false; signature = ""; render(); startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); });
            button("Collapse", () -> { expanded = false; signature = ""; render(); });
        }
        layout.width = dp(expanded ? Math.max(width, 280) : width); layout.height = expanded ? WindowManager.LayoutParams.WRAP_CONTENT : dp(36); layout.y = dp(top);
        windows.updateViewLayout(island, layout);
    }
    void toggleTorch() {
        try { cameras.setTorchMode(cameraId, !torch); }
        catch (CameraAccessException | SecurityException | IllegalArgumentException e) { Toast.makeText(this, "Allow camera permission in Skim Island, or free the camera", Toast.LENGTH_LONG).show(); }
    }
    void button(String title, Runnable action) { Button b = new Button(this); b.setText(title); b.setMinHeight(dp(48)); b.setOnClickListener(v -> action.run()); island.addView(b); }
    void mediaButton(LinearLayout row, String title, int icon, Runnable action) { ImageButton b = new ImageButton(this); b.setImageResource(icon); b.setContentDescription(title); row.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1)); b.setOnClickListener(v -> { try { if (media != null) action.run(); } catch (SecurityException e) { media = null; } signature = ""; render(); }); }
    TextView text(String s, int size) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE); t.setGravity(Gravity.CENTER); t.setPadding(0, dp(3), 0, dp(3)); return t; }
    static String clean(CharSequence value) { String s = value == null ? "" : value.toString(); return s.substring(0, Math.min(s.length(), 240)); }
    int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
