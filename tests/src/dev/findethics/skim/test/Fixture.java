package dev.findethics.skim.test;
import android.app.*;
import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.Bundle;
import android.widget.TextView;

public class Fixture extends Activity {
    static Fixture live;
    TextView text;
    MediaSession session;
    int state = PlaybackState.STATE_PLAYING, skipped;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); live = this; TextView t = text = new TextView(this);
        t.setText("Fixture ready"); t.setTextSize(24); setContentView(t);
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("tests", "Tests", NotificationManager.IMPORTANCE_LOW));
    }
    Notification.Builder note(String title) {
        return new Notification.Builder(this,"tests").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText("Local-only test");
    }
    void post() {
        PendingIntent p = PendingIntent.getActivity(this, 1, new Intent(this,Fixture.class).putExtra("opened", true),PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        getSystemService(NotificationManager.class).notify(1, note("Private fixture alert").setContentIntent(p).build());
    }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); if(i.getBooleanExtra("opened", false)) text.setText("Notification opened"); }
    void play() {
        session = new MediaSession(this,"Skim fixture");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPause() { state = PlaybackState.STATE_PAUSED; update(); }
            @Override public void onPlay() { state = PlaybackState.STATE_PLAYING; update(); }
            @Override public void onSkipToNext() { skipped++; update(); }
        });
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Fixture track").build());
        update(); session.setActive(true);
        getSystemService(NotificationManager.class).notify(2, note("Fixture track").setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken())).setOngoing(true).build());
    }
    void update() { session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT).setState(state, 0, 1).build()); }
    void clear() { getSystemService(NotificationManager.class).cancelAll(); if(session!=null) {session.release();session=null;} }
    @Override protected void onDestroy() { clear(); live=null; super.onDestroy(); }
}
