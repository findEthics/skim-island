# Skim Island

An independent, local-only Android island inspired by a focused reverse-engineering study of Shima Dynamic Island (`com.piyush.island`, 0.4.0). Not affiliated with its developer; no original code, artwork, APK or licensing machinery is distributed.

Android 12L+ (API 32). Native Java, Android SDK/JDK only; no Gradle or third-party runtime libraries.

## Install

Download the signed APK from [Releases](https://github.com/findEthics/skim-island/releases). Open Skim Island and enable its accessibility service and notification access in Android Settings. On sideloaded apps, Android may require **App info → menu → Allow restricted settings** first. Camera permission is optional for the flashlight.

Tap the island to expand; long-press for Home; swipe down for notifications. Displays the latest notification preview, media playback controls, charging/battery and flashlight state. Width/offset controls and a Quick Settings tile are included.

## Privacy

No Internet permission, account, ads, analytics or billing. Notification titles/text and media tokens stay in memory. The island hides on locked/sleeping displays and clears alert previews. Accessibility does not retrieve screen contents. Android grants notification-listener access broadly; this app uses it only for these local features. Settings are local and backup is disabled.

## Build

Install JDK 17+, Python 3, Android platform 36 and build-tools 36.0.0; put Java on PATH and set ANDROID_HOME to your SDK.

```sh
python3 build.py                         # build/debug/skim-debug.apk
python3 build.py --test                  # build/test/skim-test.apk
SKIM_KEYSTORE=/path/release.p12 SKIM_PASSWORD_FILE=/path/password python3 build.py --release
```

Release keystore alias: `skim`. Signing material never belongs in Git.

## Test

Use a disposable emulator (the tests simulate battery state and sleep). Install the app and test APK, enable Skim's accessibility/notification services, then:

```sh
adb install -r build/debug/skim-debug.apk
adb install -r build/test/skim-test.apk
adb shell pm grant dev.findethics.skim.test android.permission.POST_NOTIFICATIONS
adb shell cmd notification allow_listener dev.findethics.skim/dev.findethics.skim.IslandListener
# Enable Skim Island in Accessibility settings before running.
python3 tests/run.py
```

The runner checks instrumentation output, not just adb's exit code. Verified signed release on an API 35 x86_64 emulator: **31 checks passed**. Coverage: notification open/removal, real media-session pause/play/next, charging, enable/disable, preview expiry/preference and display sleep/unlock. Flashlight hardware, OEM restrictions, Quick Settings and gesture shortcuts are not fully automated-tested.

## Scope

This is a working minimal reconstruction, not a feature-complete clone. No original premium widgets, Tasker integration, screenshot tooling, backups, billing or watchdog subsystem. Add those only for a concrete need. See [reverse-engineering notes](REVERSE_ENGINEERING.md).
