# Installing EmberHome

This guide covers installing EmberHome directly on an Android TV/streaming device or using ADB from a computer.

## Before you install

- Check the Android version and choose an EmberHome APK that supports it.
- Prefer a stable release for everyday use. Beta, preview, and development builds may be unfinished.
- Keep your device's original launcher installed so you can switch back if needed.
- Download APKs only from the official EmberHome Downloads page or this GitHub repository.

## Method 1: Install directly on your TV with Downloader

1. Connect your TV or streaming device to the internet.
2. Install and open the **Downloader** app.
3. Enter EmberHome's Downloader code: **7686858**.
4. Select **Go** to open the EmberHome version picker.
5. Choose a version that supports your Android version.
6. Select its download link and wait for the APK download to finish.
7. Open the downloaded file.
8. If Android blocks the installation, open the prompt's settings and allow Downloader to install unknown apps. The wording and menu location vary by Android version and device manufacturer.
9. Return to the APK installer and select **Install**.
10. When installation finishes, select **Open** or press the Home button and choose EmberHome if your device offers a launcher picker.

If the download page does not open, try the direct address in Downloader's browser:

https://iy367-ux.github.io/EmberHome/

## Method 2: Install from a computer using ADB

1. Download the correct EmberHome APK from [GitHub Releases](https://github.com/iy367-ux/EmberHome/releases).
2. Enable **Network debugging** or **ADB debugging** in your device's developer settings. The exact option varies by device.
3. Connect the computer and TV/device to the same local network.
4. Find the TV/device's IP address in its network settings.
5. Open a terminal or Command Prompt in the folder containing Android Platform Tools.
6. Connect to the device:

   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```

7. Install or update EmberHome:

   ```bash
   adb install -r EmberHome.apk
   ```

Replace `<TV_IP_ADDRESS>` with the actual IP address of your TV/device. If ADB uses a different port or USB connection, follow the connection details shown by your device.

## Choose EmberHome as your home app

Press the **Home** button on your remote. If Android displays a launcher selection prompt, choose EmberHome. If it offers **Always** and **Just once**, choose **Always** only if you are happy to make EmberHome your default home app.

Some manufacturers hide or customise the default home-app setting. Refer to your device's system settings if no chooser appears.

**Important:** Do not uninstall, disable, or use ADB to remove the stock launcher just to make EmberHome the default. Keeping the original launcher provides a recovery route if EmberHome does not behave as expected.

## Updating EmberHome

- Download and install the newer APK over the existing installation.
- If Android reports that the app cannot be installed, check that the APK is compatible with your Android version and signed with the same signing key as the installed version.
- Back up any important settings if the app provides that option before changing versions.
- Avoid installing development builds on a device you rely on every day.

## Troubleshooting

| Problem | What to try |
| --- | --- |
| Downloader code does not open | Check the internet connection and try the direct URL above. |
| APK download is unavailable | Check the GitHub Releases page; the version picker only enables versions with a published APK asset. |
| Installation is blocked | Allow Downloader or your file manager to install unknown apps, then retry. |
| App not installed / parsing error | Confirm that the APK matches your device's Android version and architecture. |
| EmberHome does not appear after pressing Home | Check the device's default Home app settings. Some TV firmware does not support third-party launchers as the default. |
| Device returns to its original home screen | The manufacturer may restrict third-party launchers. Keep the stock launcher enabled. |

## Need help?

[Submit EmberHome feedback or a bug report](https://github.com/iy367-ux/EmberHome/issues/new/choose). Include your device model, Android version, EmberHome version, and the exact error message. Do not post passwords, account details, or other private information.
