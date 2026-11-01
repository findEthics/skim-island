# Focused Shima study

Target listing: https://www.appbrain.com/app/shima-dynamic-island/com.piyush.island

## Artifact identity and method

Inspected APK package `com.piyush.island`, version 0.4.0 (code 7), min API 32, target API 36.
SHA-256: `771e10e7be8fc5d1d386e937e4249912c641ac2abf0811020b57b23bd20c664a`.
Analysis: installed [REA](https://github.com/morluto/rea) CLI 6.7.0, JADX headless MCP 0.7.1. Decompiled output is a representation, not original source. Original APK and raw decompilation are intentionally not published.

Reproduction commands (provide a lawfully obtained matching APK):

```sh
export REA_JADX_MCP_JAR=/path/jadx-headless-mcp-0.7.1-all.jar
rea inspect-android-package /path/com.piyush.island.apk --format json
rea inspect-android-method /path/com.piyush.island.apk com.piyush.island.overlay.IslandAccessibilityService onServiceConnected --format json
rea inspect-android-method /path/com.piyush.island.apk com.piyush.island.content.IslandNotificationListener onNotificationPosted --format json
rea inspect-android-method /path/com.piyush.island.apk defpackage.C0154 m3512a --format json
```

## Observed static facts

- Manifest declares IslandAccessibilityService, IslandNotificationListener, IslandTileService, KeepAliveService, WatchdogJob and SystemEventsReceiver.
- onServiceConnected records initial accessibility connection, instantiates overlay-related objects C1220/C0154, starts coroutine work and disables the accessibility cache on API 33+.
- C0154.m3512a constructs C0168 with the accessibility service and state objects. This supports an overlay/state architecture inference; it alone does not establish the complete rendering implementation.
- onNotificationPosted forwards posted notifications to multiple obfuscated handlers and separately handles a filtered ticker path.
- Manifest includes billing/license checks and Pairip application, plus Internet, location, Bluetooth, call and settings-related permissions. Declared permission is not proof of runtime use.

## Independent reconstruction

Skim uses native AccessibilityService overlay, NotificationListenerService, MediaController transport controls and CameraManager torch API. It deliberately replaces the broad original subsystem graph with a single overlay service, one latest preview, a settings activity and a tile. These are our implementation choices, not recovered original source.

## Coverage and unknowns

This was a focused study of package declarations, service lifecycle and notification dispatch, not exhaustive deobfuscation or verified full feature parity. Original protected flows and premium functionality were not bypassed. The new app's runtime was separately exercised with 31 passing checks on an API 35 emulator; that does not establish original-app runtime parity or physical-device compatibility.
