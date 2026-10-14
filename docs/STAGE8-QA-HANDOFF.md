# AdultSafe Android — Stage 8 Source QA Handoff

Date: 14 October 2026
Target Android version: 0.2.0-beta, versionCode 2
Target WordPress plugin: AdultSafe Records 1.39.7
Repo: jamiebray89/Adultsafe-2
Build policy: GitHub Actions workflow_dispatch only. Do not trigger a build before source QA is frozen.

## Checked in source

- Home screen: five stable NavigationBarItem destinations with icons and single-line Home/People/Alerts/Docs/Settings labels; Lock, Sites and Logout moved to Settings.
- Actual data: /people, /dashboard, /alerts and /workspaces/default/document-acknowledgements are fetched using the AdultSafe app token. Live failures display errors and never use DemoRepository records.
- Dashboard tile drill-down: /dashboard/tile/{slug} populates real people.
- People: volunteer and parent/carer details use WordPress metadata for roles and sections; the parent flow stores birth year rather than child name. DBS/ID controls are volunteer-only.
- DBS: validates actual YYYY-MM-DD dates; WordPress handles server-side status/expiry defaults.
- ID: AndroidX FileProvider for private full-resolution camera capture, document/image picker, 15 MB limit, upload with actual JPEG/PNG/WebP/PDF extension. Upload requires confirmed evidence response, and temporary camera files are removed after use or next launch.
- Documents: selectable required documents, signing capture, copyable confirmation link and signature-free history display. The dashboard Documents list reads actual document states.
- Authentication: existing sign-in and QR flows preserved; Keystore-backed encrypted per-site tokens, version check >= 1.39.7, biometric/device-credential unlock, validated session on restore, server-side device revocation, server logout attempted before local clearing, warning if remote revocation cannot be confirmed.
- Security: WordPress 1.39.7 restricts sensitive routes by AdultSafe roles, blocks demo-only users from live APIs, validates stored evidence, strips raw signatures from API history and fixes create-person response. No user data deletion or uninstall logic added.
- Android manifest: cleartext HTTP disabled, automatic backup disabled, temporary-photo FileProvider private and keyboard adjustResize.
- App version updated, manual-only workflow unchanged.

## Verified local checks (WordPress package)

- PHP syntax: PASS.
- ZIP CRC/integrity: PASS.
- Original REST route harness: PASS, 50 routes registered.
- Authentication and QR invite contract: PASS.
- Logout/revocation and invalid/reused invite rejection: PASS.
- Role tests (manager, ID checker, documents admin, viewer, demo-only): PASS.
- Existing plugin files preserved: all 10 paths from v1.39.6 included in v1.39.7.

## Required gates NOT yet tested — do not claim production readiness

1. Android compile (assembleDebug) and artifact verification: **NOT RUN** at Stage 8 handoff, by user request.
2. Install/update on the actual Samsung Android device, layout at accessibility font scale, camera/photo/file-picker permissions, signature dialog and biometric/PIN behaviour: **NOT RUN**.
3. Install AdultSafe WordPress 1.39.7 to the Scout website; confirm /app/health advertises 1.39.7 and all newly secured API routes work with legitimate permissions: **NOT DONE**.
4. Actual writes and read-back for *test* volunteer/parent, DBS dates, three ID images, signed handover, document confirmation and device revocation: **NOT RUN**. Do not use real sensitive ID images until this passes.
5. Existing GitHub-hosted debug APK signing may differ across runners; in-place upgrade compatibility is not yet verified.
6. Push notifications (FCM) are not implemented in this build and are separate future work.

## Installation dependency

Existing WordPress 1.39.5 permits the old app to sign in, but does **not** contain the v1.39.7 hardening. The updated Android app refuses sign-in against a plugin reporting less than v1.39.7. Install WordPress v1.39.7 through **Replace current** and back up first. Never delete/uninstall AdultSafe to upgrade: uninstall could remove data. If health reports the wrong version, do not proceed with sensitive workflows.

## Build sequence once user is ready

1. Confirm WordPress 1.39.7 is installed and backed up.
2. Trigger one manual android-debug.yml run against the latest main commit.
3. Inspect the Kotlin compiler result. Fix all compile errors before describing an APK as successful.
4. Obtain and verify the APK artifact, then proceed through test-only end-to-end workflows above.
