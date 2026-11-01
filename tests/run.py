#!/usr/bin/env python3
"""Run on a disposable configured emulator; fail on instrumentation failures."""
import subprocess

def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)

if __name__ == "__main__":
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("Refusing battery/sleep tests on a physical device")
    output = adb("shell", "am", "instrument", "-w", "dev.findethics.skim.test/.Check")
    print(output, end="")
    if "SUCCESS: 31 device checks passed" not in output or "FAILURE" in output:
        raise SystemExit("Device checks failed")
