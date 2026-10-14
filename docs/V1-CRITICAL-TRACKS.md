# AdultSafe Android V1 critical tracks

These are not optional polish items. They should be present before the first serious field-test APK, even if the first debug APK still uses demo data for some flows.

## 1. Camera/photo/file picker

Required for ID evidence capture.

Target behaviour:

- Camera capture
- Gallery/photo picker
- File/PDF picker
- Temporary file only
- Upload to AdultSafe private plugin storage
- No permanent evidence cache on the phone

## 2. Biometric and PIN unlock

Required because the app keeps the user logged in until logout or device revocation.

Target behaviour:

- Local unlock before showing sensitive records
- Biometric prompt where available
- PIN fallback
- Visible Lock button
- Visible Logout button

## 3. Real upload wiring

Target endpoint:

```text
POST /wp-json/adultsafe/v1/people/{id}/id-evidence
```

Rules:

- Authenticated with per-device app token
- Multipart upload
- File metadata returned by plugin
- Upload must land in plugin private storage, not WordPress Media Library
- App must not keep ID evidence after upload succeeds

## 4. QR invite scanner

Required for easy setup of Rowlands Castle / Active Support / future groups.

Target behaviour:

- Scan AdultSafe invite QR
- Validate site URL and invite token
- Accept invite through `/app/invites/accept`
- Save resulting app token securely

## 5. Push notifications

Required for day-to-day alerts eventually, but can be staged after first installable debug APK.

Likely direction:

- Firebase Cloud Messaging or equivalent
- Plugin stores device notification token
- Plugin emits DBS/document/ID action events
- App routes tap into person profile/action

## 6. UI polish

Required before sharing with anyone else.

Focus:

- mobile layout pass
- accessibility labels
- error states
- empty states
- loading states
- no raw developer wording in normal user screens

## 7. Release signing

Prepared in Gradle using environment variables only.

Secrets must never be committed to the repo.

Environment variables:

```text
ADULTSAFE_RELEASE_STORE_FILE
ADULTSAFE_RELEASE_STORE_PASSWORD
ADULTSAFE_RELEASE_KEY_ALIAS
ADULTSAFE_RELEASE_KEY_PASSWORD
```
