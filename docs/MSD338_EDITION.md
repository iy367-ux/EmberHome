# EmberOS — MSD338 Edition

Dedicated development target: Wisdom Share TV using the CVTE / MStar PB801 board.

## Verified target facts

- Manufacturer: CVTE
- Product/device: SMART_TV / SMART_TV_PB801
- Board: BD_TP_MS338_PB801
- Android: 4.4.4 (API 19)
- CPU ABI: armeabi-v7a (32-bit)
- Reported memory: approximately 480,184 kB
- TV UI has reported 1280 × 720 logical output; panel properties separately report 1920 × 1080.

Do not assume all PB801 televisions have identical panels, remotes, tuner hardware, or firmware.

## Design and engineering rules

1. Keep minSdk at 19 and use Java APIs available on Android 4.4.4.
2. Design for D-pad navigation first. Every interactive element must have a visible focus state.
3. Keep memory use low: no continuous background video, no large image galleries, no heavy blur, and no mandatory network services.
4. Use small, cached artwork and decode images to their display size.
5. Preserve the existing EmberHome stable and development package IDs. This work lives on the separate `msd338-edition` branch.
6. Do not remove or disable CVTE/MStar services, TV input, tuner, audio, panel, remote-control, HDMI, or upgrade components.
7. Do not claim control of protected system settings, HDMI routing, tuner functions, standby, or boot animation unless a documented API or tested privilege is available.
8. Never flash firmware or write block devices as part of launcher installation.

## Product direction

- Cinematic home screen with a restrained hero area and compact app favourites.
- Left-side navigation / quick panel inspired by modern TV operating systems.
- Dedicated Apps, Inputs, Media, and Settings destinations.
- A settings shell that launches existing system activities where possible; unavailable controls are clearly identified rather than faked.
- Optional ambient/screensaver experience only if it behaves reliably on this device.

## First implementation milestone

Add a hardware profile that recognizes the known PB801 identifiers and applies conservative rendering defaults. Next: build the D-pad-first home shell and verify it on the actual TV before adding integrations.


## Owner-selected interface specification

The owner selected the following MSD338 Edition direction:

- Home: Smart Hub.
- Apps: Classic App Drawer by default, with Apple-style Grid selectable in Settings.
- Inputs: Compact Control Panel. The UI may label available sources, but must not claim to switch HDMI inputs unless a verified CVTE/MStar API is found.
- Media: Cinematic Library, focused on launching installed media applications and clearly distinguishing local library access from playback functionality.
- Settings: Apple-style Sidebar by default, with Category Cards selectable in Settings.
- Theme: Midnight Black.

The current implementation adds a dedicated MSD338 navigation shell in MainActivity and stores the two layout choices in the existing EmberHome preferences. The media page is an app-launch/library landing page, not a media playback engine. The input page is a source-information panel; actual source switching remains firmware-controlled. The settings page links to existing Android/CVTE settings where available.
