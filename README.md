[README.md](https://github.com/user-attachments/files/35078002/README.md)
# Ember Home - Modern TV launcher for Android 4.4+

Dark, ad-free home screen with four styles (Poster, Rows, Spotlight, Grid), a clock, weather,
and a settings menu to switch styles. minSdk 19. No libraries, no tracking.

## Remote controls
- D-pad: move. OK: open app. Hold OK: add to favorites (Rows style) or app info / uninstall.
- Menu key (or the Settings button): change style, weather city, units.

## Weather
Uses wttr.in (free, no account). By default it finds your city from your internet connection;
you can type a city in Settings. Only the city name (or nothing) is sent.


## Install on a TV (via ADB)
    adb connect <tv-ip>:5555
    adb install -r EmberHome.apk
Press Home, choose Ember Home, tap Always.
If the old launcher keeps coming back:
    adb shell pm list packages | findstr launcher
    adb shell pm disable-user <stock-launcher-package>     (undo: pm enable <package>)
