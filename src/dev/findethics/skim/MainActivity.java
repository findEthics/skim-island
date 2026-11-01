package dev.findethics.skim;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.provider.Settings;
import android.widget.*;
import android.view.*;

public class MainActivity extends Activity {
    LinearLayout rows;
    SharedPreferences prefs;
    TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("island", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(32, 48, 32, 32); scroll.addView(rows); setContentView(scroll);
        TextView title = new TextView(this); title.setText("Skim Island"); title.setTextSize(32); rows.addView(title);
        TextView info = new TextView(this);
        info.setText("A small, local-only island. Tap to expand; long press for Home; swipe down for notifications.\n\nAccessibility draws the island, never reads your screen. Notification access reads titles, text and media controls only on this device. No account or Internet permission.");
        info.setPadding(0, 24, 0, 24); rows.addView(info);
        status = new TextView(this); rows.addView(status);
        button("Enable accessibility", () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button("Enable notification access", () -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        button("Allow flashlight (optional)", () -> requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1));
        toggle("Island enabled", "enabled", true);
        toggle("Show notification previews", "notifications", true);
        slider("Width", "width", 160, 100, 280);
        slider("Top offset", "top", 0, 0, 100);
        button("Preview island", () -> { if (IslandService.current != null) IslandService.current.preview(); else Toast.makeText(this, "Enable Skim Island in accessibility first", Toast.LENGTH_LONG).show(); });
    }
    @Override public void onResume() { super.onResume();
        status.setText("Accessibility: " + (IslandService.current != null ? "connected" : "off") + " · Notifications: " + (IslandListener.current != null ? "connected" : "off"));
    }
    void button(String label, Runnable action) { Button b = new Button(this); b.setText(label); b.setOnClickListener(v -> action.run()); rows.addView(b); }
    void toggle(String label, String key, boolean value) { Switch s = new Switch(this); s.setText(label); s.setChecked(prefs.getBoolean(key, value)); s.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key, checked).apply()); rows.addView(s); }
    void slider(String label, String key, int value, int min, int max) {
        TextView text = new TextView(this); rows.addView(text);
        SeekBar bar = new SeekBar(this); bar.setMin(min); bar.setMax(max); bar.setProgress(prefs.getInt(key, value)); text.setText(label + ": " + bar.getProgress() + " dp");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int n, boolean user) { text.setText(label + ": " + n + " dp"); if (user) prefs.edit().putInt(key, n).apply(); }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        }); rows.addView(bar);
    }
}
