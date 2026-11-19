[README.md](https://github.com/user-attachments/files/35078002/README.md)
# EmberHome v1.1 Premium — MSD338 Edition

A lightweight Android TV launcher targeting Android 4.4.4 / API 19, tuned for the CVTE / MStar PB801 (MSD338) board.

The MSD338 edition follows the owner's five-page redesign blueprint:
- **Apple TV Style** — warm gradient, large colourful app tiles, selected-app heading, top-right clock/date and floating shortcut dock.
- **VIDAA OS Style** — dark sidebar, wide information panel and two rows of app tiles.
- **Google TV Style** — teal gradient, centred clock/date and oval app tiles.
- **Aerial screensaver** — offline coastline-at-dusk scene with clock/date and wake prompt.
- **Settings** — Home style, Wallpaper, Screensaver, Apps and About.

The three home themes are selectable in Settings and the installed-app list is discovered automatically. The debug package suffix `.msd338` also enables this edition in the API 19 emulator used by CI.

## Remote controls
- D-pad: move. OK: open app. Hold OK: add to favorites (Rows style) or app info / uninstall.
- Menu key (or the Settings button): change style, weather city, units.

## Weather
Uses wttr.in (free, no account). By default it finds your city from your internet connection;
you can type a city in Settings. Only the city name (or nothing) is sent.

## Build option 1: GitHub (easiest, no Android Studio)
1. Create a GitHub repo and upload this whole folder.
2. Open the Actions tab -> "Build APK" -> Run workflow. When it finishes, download "EmberHome-apk".
3. To publish for others: create a release tagged v1.0 - the APK is attached automatically.

## Build option 2: Android Studio
Open this folder, wait for sync, Build > Build APK(s).
APK: app/build/outputs/apk/debug/app-debug.apk

## Install on a TV
    adb connect <tv-ip>:5555
    adb install -r EmberHome.apk
Press Home, choose Ember Home, tap Always.
If the old launcher keeps coming back:
    adb shell pm list packages | findstr launcher
    adb shell pm disable-user <stock-launcher-package>     (undo: pm enable <package>)
