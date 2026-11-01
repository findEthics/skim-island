# Project briefing

Independent implementation of Shima's core island behavior; user requested a new GitHub repository and granted discretion. Keep changes minimal, native and local-only. Do not redistribute original APK/decompiled code or bypass its licensing/protection.

- Package: dev.findethics.skim; min API 32, target 36.
- Build: python3 build.py; SDK platform 36/build-tools 36.0.0, JDK 17+.
- Release: SKIM_KEYSTORE and SKIM_PASSWORD_FILE; alias skim. Credentials are outside repository and backed by Bitwarden. Never commit keys/passwords.
- Tests: tests/run.py runs the real emulator fixture. Instrumentation can report FAILURE despite adb exit 0; inspect stream.
- Final signed release verification: API 35 emulator, 31 checks passed. Sleep tests check visible root, not mere stale accessibility-window existence. Status-bar notification descriptions are not evidence of island previews.
- Accessibility overlay + notification listener + native MediaController; latest alert only. No INTERNET permission or screen-content retrieval.
- REA CLI 6.7.0 and JADX headless MCP 0.7.1 used for original APK inspection. Evidence and scope: REVERSE_ENGINEERING.md.
- Do not claim full Shima parity. OEM/physical flashlight/Quick Settings shortcuts are not fully tested.
