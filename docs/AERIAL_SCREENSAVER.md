# EmberHome Aerial screensaver

## What it does

EmberHome registers an Android DreamService named **EmberHome Aerial** on Android versions that expose the standard Android screensaver (Daydream) settings.

Playback priority:
1. Local MP4/M4V/3GP videos from the Aerial media folder on standard-memory devices.
2. Local JPG/JPEG/PNG/WebP images, rotated every 25 seconds.
3. A lightweight, offline illustrated landscape when no compatible local media is available.

On low-RAM devices, video playback is skipped and still images are preferred. Images are decoded at a reduced resolution to limit memory use. No streaming service or internet connection is required.

## Add your own scenes

Copy compatible media into:

`Movies/EmberHome/Aerial/`

Use landscape images and MP4 videos encoded with a format supported by the device. Playback support varies by hardware and Fire OS / Android version.

## Enable it

1. Install EmberHome.
2. Open EmberHome Settings.
3. Choose **Set up Aerial screensaver** and grant media access if prompted.
4. In Android's screensaver settings, select **EmberHome Aerial** and set the idle delay.

Some Amazon Fire TV models do not expose Android's standard DreamService settings. On those devices the launcher remains usable, but Aerial's automatic system activation requires separate Fire OS integration and must be verified on the target model.

## Test before release

- Android 4.4.4: confirm the service appears in system screensaver settings and still-image fallback works.
- Newer Android TV: confirm media permissions, video playback, image rotation and idle activation.
- Fire TV Stick: verify whether the Fire OS screensaver selector exposes third-party DreamService entries; do not assume Android TV behaviour.
- Low-memory hardware: verify video is skipped and image decoding remains stable.
