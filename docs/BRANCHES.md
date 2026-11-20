# Branch guide

`main` is the only official public branch and the source of truth for current releases.

The other branches are historical workspaces. They are retained to preserve development history; they are not promises of supported releases and should not be merged into `main` without a deliberate review.

| Branch | Role | Status |
| --- | --- | --- |
| `main` | Public project and active release line | Current |
| `msd338-edition` | CVTE / MStar PB801 (MSD338) television edition for Android 4.4.4 / API 19 | Legacy; frozen |
| `thiago-kitkat-port` | Thiago-style KitKat port for Android 4.4.4 / API 19 | Legacy / experimental; frozen |
| `v1.1-development` | Earlier launcher development, including customisation and screensaver work | Historical; review before reusing |
| `icon-sharpness-fix` | Short-lived icon-rendering diagnostic experiment | Superseded experiment |
| `pre-launch-cleanup` | Work used to prepare the repository structure and documentation | Merged into `main`; obsolete |

## Compatibility policy

- Existing Android 4.4.4-compatible editions are being deprecated.
- The MSD338 and KitKat branches are preserved for historical/device-specific reference, not active feature development.
- Future active development targets Android 6.0 (API 23) and newer.
- Do not assume code from a legacy branch is compatible with the active source or safe to cherry-pick. Review dependencies, manifest entries, SDK requirements, device-specific behaviour, and tests first.

## Where should contributors start?

Start from `main`. Use the active source archive under `source/` and the build workflow as the current project baseline. Open an issue before proposing a change that affects compatibility or the release baseline.

## Preservation note

A branch being listed here does not mean it is maintained. Historical branches are kept to make the project's evolution auditable and to avoid losing device-specific work.
