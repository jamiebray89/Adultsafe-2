# AdultSafe Android – in-place updates and signing

## Why old test APKs require uninstalling

Each Actions runner normally creates its own Android debug signing certificate. Two APKs with
the same package `uk.co.skills4all.adultsafe.debug` but different signers cannot update
one another, even if the new `versionCode` is greater. Avoid uninstalling merely to
retry a broken build.

## Permanent solution (one-time owner setup)

1. Generate or adopt **one** long-term Android signing keystore under the repository owner's
   control. Store an offline encrypted backup; losing the key prevents future in-place updates.
2. In the GitHub repo Settings → Secrets and variables → Actions, securely create the **repository secrets**
   `ADULTSAFE_KEYSTORE_BASE64` (single-line base64 encoding of that keystore),
   `ADULTSAFE_KEYSTORE_PASSWORD`, `ADULTSAFE_KEY_ALIAS` and `ADULTSAFE_KEY_PASSWORD`.
   Never post passwords or the private keystore into GitHub issues, source code or chat.
3. In `app/build.gradle.kts`, the debug build now selects the persistent keystore **only when**
   `ADULTSAFE_RELEASE_STORE_FILE` is provided. The new manual
   `.github/workflows/android-updateable.yml` restores the key and sets all Gradle variables.
   **It intentionally fails** if any required secret is missing rather than silently producing
   another incompatible APK. The original manual `android-debug.yml` remains available for
   compilation checks but its artifacts are *not* reliable update packages.
4. Each subsequent updateable build must use the same certificate, the same `applicationId`
   (currently `uk.co.skills4all.adultsafe.debug`) and an incremented Android `versionCode`.
5. Compare signing-certificate SHA-256 fingerprints before and after a new build.
   Install release N+1 over release N without uninstalling. Verify local sessions remain.
6. A **single final uninstall** may be necessary when migrating from an already-installed
   temporary/debug APK with a different signing certificate. Keep WordPress data safe;
   only local encrypted sessions on the phone may need re-entering. Do not promise lossless
   migration without matching signing keys.

## App updates versus Play Protect

Permanent signing enables **installing an update over the old app**, not unattended downloads.
For automatic update delivery and fewer sideloading prompts, separately investigate
Google Play internal testing/closed testing or a supported updater. Keep Play Protect enabled.
A verified signature alone cannot suppress Play Protect scanning prompts.
