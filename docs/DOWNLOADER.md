# EmberHome Downloader version picker

The TV-friendly version selector is published from this folder as the GitHub Pages site:

https://iy367-ux.github.io/EmberHome/

## Open EmberHome on your TV

1. Install and open the **Downloader** app on your Android TV or streaming device.
2. Enter this code: **7686858**
3. Select **Go**.
4. The EmberHome version picker should open in Downloader's browser.
5. Choose the version appropriate for your device and download its APK.

You can also enter the full URL above in Downloader's browser.

## Publishing and maintaining the page

The code points to the EmberHome GitHub Pages URL. AFTVnews codes cannot be edited after creation, so keep the destination page stable. If the destination ever changes, generate a new code and update the README and user instructions.

To enable or check GitHub Pages:

1. Open the repository: https://github.com/iy367-ux/EmberHome
2. Go to **Settings → Pages**.
3. Under **Build and deployment**, choose **Deploy from a branch**.
4. Select branch **main** and folder **/docs**, then press **Save**.
5. Wait for GitHub Pages to publish, then test the URL above and the Downloader code.

## Add or publish versions

The selector checks the public GitHub Releases API and only enables a version when it finds a non-draft, non-prerelease release with an attached `.apk` asset. It currently displays 2.0.0, 1.5.0 and 1.0.0.

For each version, publish a GitHub Release with a tag such as `v2.0.0` and attach the correct APK. The current workflow automatically attaches `EmberHome.apk` when a `v*` tag is built. Older APKs must be attached to their matching releases manually if they are not already there.

The selector deliberately does not link to Actions artifacts: those are temporary build artifacts, not stable public release downloads. Only mark a release stable after its APK has been tested and is ready for general users.
