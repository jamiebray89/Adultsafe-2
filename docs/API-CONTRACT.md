# AdultSafe app API contract summary

The Android app talks to the self-hosted AdultSafe Records WordPress plugin.

Base path:

```text
/wp-json/adultsafe/v1
```

## Connection and compatibility

```text
GET /site
GET /app/health
POST /app/login
POST /app/logout
POST /app/invites/accept
GET /app/me
GET /app/devices
POST /app/devices/revoke
POST /app/token/refresh
```

## Dashboard and alerts

```text
GET /dashboard
GET /dashboard/tile/{slug}
GET /alerts
```

## People

```text
GET /people
GET /people/{id}
POST /people
PUT /people/{id}
```

## DBS / ID

```text
GET /people/{id}/dbs
PUT /people/{id}/dbs
GET /people/{id}/id-check
PUT /people/{id}/id-check
```

## ID evidence

```text
GET /people/{id}/id-evidence
POST /people/{id}/id-evidence
DELETE /id-evidence/{evidence_id}
GET /id-evidence/{evidence_id}/view
```

Evidence must be private plugin storage only. It must not be exposed through WordPress Media Library or public URLs.

## Documents given and confirmations

```text
GET /people/{id}/documents
PUT /people/{id}/documents/requirements
POST /people/{id}/documents/given
POST /people/{id}/documents/request-confirmation
GET /people/{id}/documents/history
```

## Settings metadata

```text
GET /settings/meta
```

## Access and demo mode

```text
GET /access/users
POST /access/users
PUT /access/users/{id}
POST /access/invites
GET /demo/status
POST /demo/enable
POST /demo/disable
POST /demo/reset
POST /demo/viewer
```

## App rules

- WordPress admin role does not automatically grant AdultSafe data access.
- The plugin returns the app user's AdultSafe permissions.
- App sessions are per-site and per-device.
- The user stays logged in until logout or revocation.
- The phone requires local unlock before showing sensitive data.


## App first-run authentication (client expectation)

A fresh install offers **Sign in with email** and **Scan QR code**. Direct login uses
the WordPress site URL over HTTPS, validates GET /site and GET /app/health,
then sends POST /app/login with username, password, device_name and app_version.
It must **not** navigate to the dashboard on discovery alone or on an empty token.

The expected successful response contains token, user, optional site metadata
and permissions. The Android app accepts both the top-level response and one
wrapped in "data". A missing token or user fails closed. HTTP 401/403 and
network errors do not open the dashboard.

The camera scanner expects UTF-8 JSON in this agreed shape:

\`\`\`json
{"site_url":"https://example.org","invite_token":"single-use-token"}
\`\`\`

The app validates HTTPS and submits the invite_token, device_name and app_version
to POST /app/invites/accept. **WordPress backend compatibility must be verified**:
the API routes are documented but their implementation/response contract has
not been verified against a live AdultSafe plugin during this Android-only update.

A successful live session token is kept in Keystore-backed encrypted
preferences. Logging out clears it. The existing local-unlock and live-data-sync
foundations are still incomplete and must not be mistaken for production security.


## Stage 8 hardened production contract (minimum WordPress 1.39.7)

The Android 0.2.0-beta client refuses live authentication or local-session unlock
unless /app/health or /app/me reports AdultSafe API >= 1.39.7. This guards against
the older plugin's weak REST authorisation.

All live authenticated requests use X-AdultSafe-App-Token. Required response keys:
- GET /people: people array.
- GET /dashboard: tiles array; tiles have key, label, count, filter.
- GET /alerts: items array.
- GET /workspaces/default/document-acknowledgements: rows array;
  each row has person_id and documents states keyed by requirement.
- GET /dashboard/tile/{slug}: people array.
- POST /app/login: token, device_id, user, permissions.
- GET /app/me: permissions array and site.api_version.

ID evidence: POST /people/{id}/id-evidence uses multipart with the file part
bearing a real .jpg/.png/.webp/.pdf extension; the WordPress plugin checks both
the file type and that private evidence storage actually completed. The client
rejects a missing evidence confirmation. Signed history responses redact raw
base64 signature data while retaining audit events.

v1.39.7 also fixes POST /people to return the created person's full detail,
rather than attempting to look up an ID missing from a new request.
