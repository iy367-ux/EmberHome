<!-- CI build verification: Android 4.4/API 19 -->

# EmberHome — Thiago-style KitKat port

This branch ports the clean, ad-free, liquid-glass launcher experience documented by [Thiago OS](https://github.com/thiagofernandezguillen-rgb/thiago-os) to **Android 4.4.4 / API 19** for the CVTE / MStar PB801 (MSD338) television.

It is a native Java implementation using Android's API-19 View, Canvas, PackageManager and SharedPreferences APIs. It does **not** use Jetpack Compose, AndroidX, Kotlin, DataStore, Baseline Profiles, or runtime APIs newer than KitKat.

## Included

- Featured application with a large app icon, category, description, Open and Favourite actions.
- Favourites-first app row and an eight-column full app drawer.
- Long-press OK / Menu context menu: favourite, move left/right, app info.
- D-pad-first navigation with visible focus outlines.
- Soft-gradient wallpaper and translucent glass-like panels, drawn locally.
- Control centre with Wi-Fi, Bluetooth, Cast, Source and Settings shortcuts plus volume and brightness controls.
- Settings destinations that hand off to Android / TV settings activities where present.
- Automatic installed-app discovery; app list and favourites are stored locally.
- No ads, analytics, telemetry, or launcher-initiated network requests.
- Existing EmberHome Aerial Dream screensaver retained.

## Remote control

- D-pad: navigate.
- OK: open the focused app or action.
- Hold OK, or press Menu: app context menu.
- Back: close overlay / return home.
- Volume keys: adjust media volume.
- From the top navigation, move right past Settings to open Control Centre.
- Move to Settings in the top navigation and press Down to open settings shortcuts.

## Compatibility notes

- minSdk 19; built for Android 4.4.4 (API 19).
- The UI is deliberately drawn without blur filters or continuous animation to stay responsive on the PB801's roughly 480 MB RAM.
- Some settings pages, Cast, Bluetooth and HDMI source switching depend on the TV firmware. EmberHome opens the corresponding system screen when available; it does not claim privileged control over HDMI routing, tuner hardware, or protected system services.
- App ordering and favourites are local launcher preferences. App launches use the installed app's declared launcher activity.
- The package remains com.idrees.emberhome.msd338 for the MSD338 debug build. The stock CVTE launcher and system services are not disabled or removed.

## Build and test

GitHub Actions builds EmberHome-Thiago-KitKat.apk, boots an Android 4.4 emulator, opens the launcher and captures home, app drawer, context menu, settings and control-centre screenshots.

The APK is available from the Actions run artifact named EmberHome-Thiago-KitKat-APK.

## Upstream attribution

The UI direction and feature behaviour are based on Thiago OS (MIT), copyright (c) 2026 Thiago. See THIAGO_OS_LICENSE.txt. This port is independently implemented in Java for API 19; it does not bundle the upstream Compose implementation or Inter font.


The API 19 test also checks Back-key overlay dismissal and the complete Home → Settings → Control Centre navigation path.
