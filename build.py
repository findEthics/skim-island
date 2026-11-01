#!/usr/bin/env python3
"""Build with the Android SDK and JDK only. No Gradle, Maven or third-party libs."""
import argparse, os, pathlib, shutil, subprocess, zipfile
P = pathlib.Path(__file__).resolve().parent

def run(*args):
    subprocess.run([str(a) for a in args], check=True)

def build(test=False, release=False):
    sdk = pathlib.Path(os.environ.get("ANDROID_HOME", pathlib.Path.home() / "android-sdk"))
    platform = sdk / "platforms/android-36/android.jar"
    tools = sdk / "build-tools/36.0.0"
    name = "test" if test else "release" if release else "debug"
    out = P / "build" / name
    if out.exists(): shutil.rmtree(out)
    out.mkdir(parents=True)
    manifest = P / ("tests/AndroidManifest.xml" if test else "AndroidManifest.xml")
    source = P / ("tests/src" if test else "src")
    resources = P / "res"
    generated = out / "generated"; generated.mkdir()
    if not test:
        run(tools / "aapt2", "compile", "--dir", resources, "-o", out / "resources.zip")
    args = [tools / "aapt2", "link", "-I", platform, "--manifest", manifest, "--java", generated, "-o", out / "base.apk"]
    if not test: args += [out / "resources.zip"]
    run(*args)
    classes = out / "classes"; classes.mkdir()
    java = sorted(source.rglob("*.java")) + sorted(generated.rglob("*.java"))
    run("javac", "-encoding", "UTF-8", "-source", "8", "-target", "8", "-classpath", platform, "-d", classes, *java)
    dex = out / "dex"; dex.mkdir()
    run(tools / "d8", "--min-api", "32", "--lib", platform, "--output", dex, *sorted(classes.rglob("*.class")))
    with zipfile.ZipFile(out / "base.apk", "a", compression=zipfile.ZIP_DEFLATED) as archive:
        for f in dex.glob("*.dex"): archive.write(f, f.name)
    run(tools / "zipalign", "-f", "4", out / "base.apk", out / "aligned.apk")
    if release:
        key = pathlib.Path(os.environ["SKIM_KEYSTORE"])
        password = pathlib.Path(os.environ["SKIM_PASSWORD_FILE"])
    else:
        key = P / "build/debug.p12"; password = P / "build/debug.password"
        if not key.exists():
            import secrets
            password.write_text(secrets.token_urlsafe(32)); password.chmod(0o600)
            run("keytool", "-genkeypair", "-keystore", key, "-storepass:file", password, "-alias", "skim", "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000", "-dname", "CN=Skim Debug")
    apk = out / ("skim-island-0.1.0.apk" if release else "skim-" + name + ".apk")
    run(tools / "apksigner", "sign", "--ks", key, "--ks-key-alias", "skim", "--ks-pass", "file:" + str(password), "--out", apk, out / "aligned.apk")
    run(tools / "apksigner", "verify", "--print-certs", apk)
    print(apk)

if __name__ == "__main__":
    a = argparse.ArgumentParser(); a.add_argument("--test", action="store_true"); a.add_argument("--release", action="store_true"); opts = a.parse_args()
    if opts.test and opts.release: a.error("test and release are separate targets")
    build(opts.test, opts.release)
