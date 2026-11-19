# EmberHome device compatibility

EmberHome is designed as a remote-first launcher for Android TV devices and compatible Amazon Fire TV devices.

## Baseline

- Minimum Android API: 19 (Android 4.4 KitKat).
- TV navigation: D-pad, OK/Enter, Back and Menu/Settings keys.
- Layout: responsive to logical display width; never infer layout from TV diagonal size.
- Performance: keep the default experience lightweight and make motion effects optional as customisation grows.
- Display: backgrounds may fill the full display; important text and controls should retain configurable safe-area spacing.

## Runtime detection

`DeviceProfile` centralises lightweight capability checks:
- Amazon Fire TV identification from build manufacturer, brand, model and device strings.
- Low-RAM device detection through ActivityManager.
- Grid column selection based on available logical width.
- Runtime check for Android immersive mode support.

Detection is advisory. A feature must still fall back gracefully if a device reports incomplete or unusual build information.

## Screensaver direction

The Aerial-style screensaver is planned as an optional module, not a dependency of the home screen. It must:
- work without a network connection after content is available locally;
- offer video scenes where decoding and storage permit;
- fall back to still-image scenes on weaker or incompatible devices;
- allow users to disable it entirely;
- respect the platform's supported idle/screensaver entry points where available.

Android TV and Fire OS do not expose identical system integration on every version. The launcher must not promise identical screensaver activation behaviour until tested on representative devices.

## Device test matrix

Before release, test at least:
- Android 4.4.4 TV box / television (API 19);
- Android 8–11 TV device;
- current Android TV / Google TV device;
- Fire TV Stick on Fire OS 7 or 8;
- low-memory device and a 720p display;
- 1080p and 4K logical display configurations.

## Release safety

The development branch must build successfully before its APK is offered for installation. Keep the known-good v1.0 build untouched until the new build has been installed and checked on a test device.
