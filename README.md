# EmberHome

**A TV launcher project focused on a clean, remote-friendly home-screen experience.**

EmberHome is built for Android-based TVs and streaming devices. This repository contains the current v2.0.0 source archive, the build workflow, and the preserved legacy project.

## Compatibility and development direction

- **Existing editions:** EmberHome has supported Android 4.4.4 (KitKat) and newer.
- **Legacy support is being deprecated:** the Android 4.4.4-compatible line is being phased out. Existing builds may continue to be used, but new features and ongoing development are not guaranteed for that line.
- **Future releases:** development is moving to **Android 6.0 (API 23) and newer**. New features will target this newer baseline.

The legacy project is preserved under [`legacy/android-4.4.4/`](legacy/android-4.4.4/) for reference. It is not the active development line.

## Download

The latest successful GitHub Actions build publishes an APK artifact:

1. Open [Actions](https://github.com/iy367-ux/EmberHome/actions).
2. Select the latest successful **Build APK** run.
3. Download the `EmberHome-v2.0.0-apk` artifact and extract `EmberHome.apk`.

Tagged releases will also attach the APK to the corresponding [GitHub Release](https://github.com/iy367-ux/EmberHome/releases).

> Builds are development/debug APKs unless a release is explicitly marked otherwise. Check the release notes and device compatibility before installing.

## Install on an Android TV

You can install over ADB from a computer on the same network:

```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r EmberHome.apk
```

Then press **Home** on the TV remote and choose EmberHome from the launcher selection screen. The exact steps for setting a default launcher vary by TV manufacturer and Android version.

## Build from source

The active v2.0.0 Android project is kept as a source archive at [`source/EmberHome-v2.0.0.zip`](source/EmberHome-v2.0.0.zip). GitHub Actions extracts that archive into a temporary build directory and runs the Gradle build there; it does not overwrite the checked-in repository files.

To build locally:

1. Extract the source archive.
2. Open the extracted `EmberHome` project directory in Android Studio, or use a compatible Java/Gradle/Android SDK setup.
3. Run:

```bash
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` inside the extracted project.

## Repository layout

| Path | Purpose |
| --- | --- |
| `source/` | Active v2.0.0 source archive used by CI |
| `legacy/android-4.4.4/` | Preserved earlier Android 4.4.4-compatible project |
| `.github/workflows/` | Automated APK build and release workflow |

## Project status

EmberHome is actively evolving. Features, device compatibility, and release availability may change between builds. Please use GitHub Issues to report reproducible bugs or request improvements.

---

Made by [iy367-ux](https://github.com/iy367-ux).
