package dev.findethics.skim.test;
import android.app.*;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.*;
import android.os.*;
import android.view.accessibility.*;
import java.util.*;

public class Check extends Instrumentation {
    UiAutomation ui;
    int passed;
    @Override public void onCreate(Bundle b) { super.onCreate(b); start(); }
    interface Condition { boolean ok(); }
    void check(String name, Condition condition) {
        long end = SystemClock.uptimeMillis()+12000;
        do { if(condition.ok()) {passed++; status("PASS "+name); return;} SystemClock.sleep(100); } while(SystemClock.uptimeMillis()<end);
        throw new AssertionError(name+"; visible="+visible());
    }
    void status(String s) { Bundle b=new Bundle(); b.putString("stream", s+"\n"); sendStatus(0,b); }
    void shell(String cmd) {
        try (ParcelFileDescriptor fd=ui.executeShellCommand(cmd); java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] b=new byte[1024]; while(in.read(b)!=-1){} }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    List<AccessibilityNodeInfo> nodes() {
        List<AccessibilityNodeInfo> out=new ArrayList<>();
        for(AccessibilityWindowInfo w:ui.getWindows()) { AccessibilityNodeInfo n=w.getRoot(); if(n!=null) collect(n,out); } return out;
    }
    void collect(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out) {out.add(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null)collect(c,out);}}
    boolean has(String text) {for(AccessibilityNodeInfo n:nodes()) if((String.valueOf(n.getText())+" "+("dev.findethics.skim".equals(String.valueOf(n.getPackageName()))?String.valueOf(n.getContentDescription()):"")).toLowerCase(java.util.Locale.ROOT).contains(text.toLowerCase(java.util.Locale.ROOT)))return true;return false;}
    String visible() {StringBuilder s=new StringBuilder();for(AccessibilityNodeInfo n:nodes())if(n.getText()!=null)s.append(n.getText()).append("|");return s.toString();}
    void click(String name) {
        check("control visible: "+name,()->has(name));
        for(AccessibilityNodeInfo n:nodes()) if(name.equalsIgnoreCase(String.valueOf(n.getText())) || name.equalsIgnoreCase(String.valueOf(n.getContentDescription()))) {if(!n.performAction(AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("click "+name);return;}
        throw new AssertionError("no control "+name);
    }
    boolean islandVisible() { for(AccessibilityWindowInfo w:ui.getWindows()) if(w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && w.getRoot()!=null && w.getRoot().isVisibleToUser()) return true; return false; }
    void expand() {
        if(has("Collapse")) return;
        for(AccessibilityWindowInfo w:ui.getWindows()) if(w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) {
            android.graphics.Rect b=new android.graphics.Rect(); w.getBoundsInScreen(b);
            long now=SystemClock.uptimeMillis();
            android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,b.centerX(),b.top+20,0);
            android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+60,android.view.MotionEvent.ACTION_UP,b.centerX(),b.top+20,0);
            ui.injectInputEvent(down,true);ui.injectInputEvent(up,true);down.recycle();up.recycle();return;
        }
        throw new AssertionError("No island: "+visible());
    }
    @Override public void onStart() {
        Bundle result=new Bundle(); int code=Activity.RESULT_OK;
        try {
            ui=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo info=ui.getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;ui.setServiceInfo(info);
            shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard");
            shell("dumpsys battery unplug");
            startActivitySync(new Intent(getTargetContext(),Fixture.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            check("fixture launched",()->Fixture.live!=null);
            check("idle island",()->islandVisible());
            shell("dumpsys battery set level 42");shell("dumpsys battery set ac 1");
            check("charging battery",()->has("Charging 42%"));
            shell("dumpsys battery unplug");
            check("charging clears",()->!has("Charging 42%"));
            runOnMainSync(()->Fixture.live.post());
            check("notification preview",()->has("Private fixture alert"));
            expand();click("Open notification");check("notification opens its app",()->has("Notification opened"));
            runOnMainSync(()->Fixture.live.getSystemService(NotificationManager.class).cancel(1));
            check("notification removal clears preview",()->!has("Private fixture alert"));
            runOnMainSync(()->Fixture.live.play());
            check("media metadata",()->has("Fixture track"));
            expand();click("Pause");check("pause delivered to real media session",()->Fixture.live.state==android.media.session.PlaybackState.STATE_PAUSED);
            click("Play");check("play delivered to real media session",()->Fixture.live.state==android.media.session.PlaybackState.STATE_PLAYING);
            click("Next");check("next delivered to real media session",()->Fixture.live.skipped==1);
            runOnMainSync(()->Fixture.live.clear());
            check("media removal clears metadata",()->!has("Fixture track"));
            expand();click("Settings");check("settings opened",()->has("Enable accessibility"));
            click("Island enabled");check("disable removes overlay",()->!islandVisible());
            click("Island enabled");check("enable restores overlay",()->islandVisible());
            click("Preview island");check("preview action",()->has("Your island is ready"));
            check("preview expires",()->!has("Your island is ready"));
            click("Show notification previews");
            runOnMainSync(()->Fixture.live.post()); SystemClock.sleep(1200);
            check("previews disabled",()->!has("Private fixture alert"));
            click("Show notification previews"); runOnMainSync(()->Fixture.live.clear());
            shell("input keyevent KEYCODE_SLEEP");
            check("screen off",()->!getTargetContext().getSystemService(PowerManager.class).isInteractive());
            check("no overlay exposed on sleeping display",()->!islandVisible());
            shell("input keyevent KEYCODE_WAKEUP"); shell("wm dismiss-keyguard");
            check("overlay restored after unlock",()->islandVisible());
            result.putString("stream","SUCCESS: "+passed+" device checks passed\n");
        } catch(Throwable t) { code=Activity.RESULT_CANCELED; result.putString("stream","FAILURE: "+t+"\n"); }
        finally { if(ui!=null)shell("dumpsys battery reset"); runOnMainSync(()->{if(Fixture.live!=null)Fixture.live.clear();}); }
        finish(code,result);
    }
}
