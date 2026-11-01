package dev.findethics.skim;

import android.app.Notification;
import android.media.session.MediaSession;
import android.service.notification.*;

public class IslandListener extends NotificationListenerService {
    static IslandListener current;
    @Override public void onListenerConnected() { current = this; refresh(); }
    @Override public void onListenerDisconnected() { current = null; if (IslandService.current != null) IslandService.current.clearNotification(); }
    @Override public void onDestroy() { current = null; if (IslandService.current != null) IslandService.current.clearNotification(); super.onDestroy(); }
    @Override public void onNotificationPosted(StatusBarNotification item) {
        if (IslandService.current != null && !item.getPackageName().equals(getPackageName())) IslandService.current.notification(item);
    }
    @Override public void onNotificationRemoved(StatusBarNotification item) {
        if (IslandService.current != null) IslandService.current.removed(item.getKey());
    }
    void refresh() {
        if (IslandService.current == null) return;
        // ponytail: resume media only; don't replay stale notification previews.
        try { for (StatusBarNotification n : getActiveNotifications()) {
            if (n.getNotification().extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token.class) != null) IslandService.current.notification(n);
        } } catch (SecurityException ignored) { current = null; }
    }
}
