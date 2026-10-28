# Android 0.2.1-beta test plan

Testing site only; no WordPress plugin changes.

1. Open a test volunteer with blank email and membership number. Verify amber missing-information summary and field labels.
2. Open a test person with a missing required name or role. Verify required warnings and that saving is disabled until both are supplied.
3. Complete fields and verify warnings disappear before saving.
4. Verify parent/carer records do not show a volunteer membership warning.
5. Verify read-only users can see missing fields but cannot edit them.
6. Verify the APK reports versionCode 3 and versionName 0.2.1-beta-debug.
7. Verify existing login, DBS, ID evidence and document workflows against the current WordPress test backend.

This is a development candidate, not a completed security or integration certification.
