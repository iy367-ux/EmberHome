# EmberHome

**A remote-friendly home-screen launcher for Android TVs and streaming devices.**

EmberHome is an independent launcher project focused on a clean TV experience and practical D-pad navigation. This repository is the public home for the project, its active source archive, automated builds, and preserved historical editions.

> **Development direction:** Existing EmberHome builds have supported Android 4.4.4 and newer. The Android 4.4.4-compatible line is now being deprecated. Future active development is moving to **Android 6.0 (API 23) and newer**.

## Getting EmberHome

### Download a build

The latest successful build is available from [GitHub Actions](https://github.com/iy367-ux/EmberHome/actions):

1. Open the latest successful **Build APK** workflow run.
2. Find the artifact named `EmberHome-v2.0.0-apk`.
3. Download and extract it to obtain `EmberHome.apk`.

You can also check [GitHub Releases](https://github.com/iy367-ux/EmberHome/releases) for versioned releases.

**Build status:** The current `main` branch has passed its GitHub Actions build. The workflow currently produces a **debug/development APK**, not a signed production release. Treat it as a test build until a release is explicitly marked stable and signed.

### Install using ADB

Enable network ADB on your TV/device, connect your computer to the same network, then run:

```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r EmberHome.apk
```

Press **Home** on the remote and select EmberHome when the device offers a launcher choice. The exact default-launcher steps vary by manufacturer and Android version.

**Safety note:** Do not disable or remove your device's stock launcher as a routine installation step. Keep a way to return to the original home screen.

## Compatibility

- **Existing editions:** Android 4.4.4 (KitKat / API 19) and newer, depending on the edition and device.
- **Legacy line:** Android 4.4.4 support is being phased out. Legacy builds are preserved for existing compatible devices, but should not be expected to receive new features or ongoing maintenance.
- **Future active development:** Android 6.0 (API 23) and newer.

Android TV and Amazon Fire TV devices can differ in their launcher, screensaver, permissions, and system-settings behaviour. Compatibility with a particular model should be confirmed by testing; a successful APK build does not prove every device feature works.

## Source and building

The active v2.0.0 project is currently stored as [`source/EmberHome-v2.0.0.zip`](source/EmberHome-v2.0.0.zip). GitHub Actions extracts it into a temporary directory and builds it without overwriting repository files.

To build locally:

1. Download and extract the source archive.
2. Open the extracted Android project in Android Studio, or use a compatible Java, Gradle, and Android SDK installation.
3. Run:

```bash
gradle assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/` in the extracted project.

## Repository map

| Location | Purpose |
| --- | --- |
| `source/` | Active v2.0.0 source archive used by CI |
| `legacy/android-4.4.4/` | Earlier Android 4.4.4-compatible project, preserved for reference |
| `.github/workflows/` | Automated APK build and release workflow |
| `docs/BRANCHES.md` | What each development and legacy branch is for |

## Branches and project history

The `main` branch is the official public branch. Historical device ports and experiments are kept separate so they cannot accidentally become the current release. See [Branch guide](docs/BRANCHES.md) before choosing a branch.

## Downloader version picker

A TV-friendly version selector is available at [EmberHome Downloads](https://iy367-ux.github.io/EmberHome/) once GitHub Pages is enabled with branch `main` and folder `/docs`. It lists versions 2.0.0, 1.5.0, and 1.0.0, and enables downloads only when a matching public GitHub Release contains an APK asset. See [Downloader setup](docs/DOWNLOADER.md) for publishing and creating the AFTVnews numeric code.

## Reporting a problem

Please use [GitHub Issues](https://github.com/iy367-ux/EmberHome/issues). Include:

- Your TV/streaming-device make and model.
- Android version and API level, if known.
- EmberHome version and how it was installed.
- What you expected, what happened, and steps to reproduce it.
- Relevant screenshots or logs, with personal information removed.

## Project status

EmberHome is evolving. Features, supported devices, and release availability may change. Please read the compatibility notes before installing and do not treat an experimental branch or debug artifact as a stable release.

---

Made by [iy367-ux](https://github.com/iy367-ux).
