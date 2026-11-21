# EmberHome Downloader version picker

The TV-friendly version selector is published from this folder as the GitHub Pages site:

`https://iy367-ux.github.io/EmberHome/`

## Enable GitHub Pages once

1. Open the repository: https://github.com/iy367-ux/EmberHome
2. Go to **Settings → Pages**.
3. Under **Build and deployment**, choose **Deploy from a branch**.
4. Select branch **main** and folder **/docs**, then press **Save**.
5. Wait for GitHub Pages to publish, then test the URL above.

## Create the AFTVnews numeric code

1. Open https://go.aftvnews.com/.
2. Paste the published GitHub Pages URL: `https://iy367-ux.github.io/EmberHome/`.
3. Generate the short code and keep the code and matching short URL for EmberHome's official instructions.

**Important:** AFTVnews says a code's destination cannot be edited after creation. Create it only after the Pages URL works. Submitted URLs are public, so do not submit private links.

## Add or publish versions

The selector checks the public GitHub Releases API and only enables a version when it finds a non-draft, non-prerelease release with an attached `.apk` asset. It currently displays 2.0.0, 1.5.0 and 1.0.0.

For each version, publish a GitHub Release with a tag such as `v2.0.0` and attach the correct APK. The current workflow automatically attaches `EmberHome.apk` when a `v*` tag is built. Older APKs must be attached to their matching releases manually if they are not already there.

The selector deliberately does not link to Actions artifacts: those are temporary build artifacts, not stable public release downloads.
