> **HISTORICAL DEVELOPMENT BRANCH — NOT A RELEASE**
>
> This branch preserves earlier EmberHome v1.1 development work. It is not the active source of truth and should not be installed or merged into `main` without review.
>
> Existing Android 4.4.4-compatible builds are being phased out. Future active EmberHome development targets Android 6.0 (API 23) and newer. See the [main repository](https://github.com/iy367-ux/EmberHome) for current downloads and guidance.

# EmberHome v1.1 — historical development

This branch contains earlier launcher experiments and feature work, including:

- Multiple home-screen layouts and remote-first navigation.
- Wallpaper selection and custom photo wallpaper.
- App management, hidden apps and saved app ordering.
- Aerial screensaver / Android DreamService integration.
- Device-profile and low-memory handling.
- Expanded appearance and personalisation settings.

These features are preserved as development history. Their presence in this branch does not mean they are complete, supported, or compatible with the current active project.

## Reusing code safely

Before carrying any feature forward, review its Android API requirements, manifest permissions, device-specific assumptions, memory use, and behaviour on Android 6.0+. Test it on representative Android TV and Fire TV hardware. Do not cherry-pick this branch wholesale into `main`.

## Branch status

Historical only. No release is offered from this branch. Use `main` for the official project and the [branch guide](https://github.com/iy367-ux/EmberHome/blob/main/docs/BRANCHES.md) for repository structure.
