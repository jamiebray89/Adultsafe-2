# AdultSafe Android

AdultSafe is the Android companion app for the AdultSafe Records WordPress plugin.

This repository is separate from OVL, EBM and the WordPress plugin repositories.

## V1 direction

The first installable APK should be useful enough to test as a real AdultSafe app, not just a blank shell.

The app is being built in phases internally:

1. App shell, branding, site connection and dashboard
2. People list and person profile
3. DBS / ID evidence workflow
4. Documents given and acknowledgement workflow
5. Alerts, lock screen, connected site management and polish

## Current status

A pre-build tidy pass has been completed:

- `MainActivity.kt` is now only the Android entry point.
- Models live under `model/`.
- Demo data lives under `data/DemoRepository.kt`.
- API connection/login/upload route shape lives under `api/AdultSafeApi.kt`.
- Theme and shared components live under `ui/theme/` and `ui/components/`.
- GitHub Actions is manual-only while stabilising builds.
- The Android SDK setup no longer asks for the obsolete `tools` package.
- Release signing is prepared through environment variables only.

See:

- `docs/ROADMAP.md`
- `docs/API-CONTRACT.md`
- `docs/V1-CRITICAL-TRACKS.md`

## Product rules

- Android first.
- Proper native app, not a WebView wrapper.
- Online only for V1.
- No ID evidence stored permanently on the phone.
- Camera/photo/file upload goes to the AdultSafe plugin private storage endpoint.
- One app can connect to multiple self-hosted AdultSafe sites.
- Data stays on each group's own WordPress site.
- WordPress admin access does not automatically mean AdultSafe data access.
- Local app unlock should use PIN/biometric, while keeping the user logged in until they choose to log out.

## Package

`uk.co.skills4all.adultsafe`

Debug builds use:

`uk.co.skills4all.adultsafe.debug`

## Backend

The app talks to the AdultSafe Records plugin REST API under:

`/wp-json/adultsafe/v1/`

## Release signing

Release signing is configured from environment variables. Signing files and passwords must not be committed.

```text
ADULTSAFE_RELEASE_STORE_FILE
ADULTSAFE_RELEASE_STORE_PASSWORD
ADULTSAFE_RELEASE_KEY_ALIAS
ADULTSAFE_RELEASE_KEY_PASSWORD
```
