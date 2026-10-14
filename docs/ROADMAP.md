# AdultSafe Android roadmap

## First APK target

Although the work is split into phases internally, the first APK intended for real testing should cover phases A to E well enough to exercise the main AdultSafe journeys.

The first serious field-test APK should also include the V1-critical tracks in `docs/V1-CRITICAL-TRACKS.md`.

## Phase A — shell and connection

Status: implemented as app shell foundation.

- AdultSafe app name, launcher icon and splash/logo usage.
- Connect to an AdultSafe site by URL.
- Prepare QR/invite connection flow.
- Store connected sites separately.
- Support switching between sites/groups.
- Read `/site`, `/app/health` and `/dashboard`.
- Persistent app session model.
- Local lock screen structure.

## Phase B — people

Status: implemented as UI/workflow foundation.

- People cards matching AdultSafe dashboard style.
- Search, sort and filter.
- Person profile with Details, DBS / ID, Documents and History.
- Route-gated quick actions.

## Phase C — DBS / ID

Status: implemented as UI/workflow foundation.

- DBS status view and update shape.
- Result/completed and expiry date handling shape.
- ID evidence wizard shape.
- Camera/photo/file upload entry points.
- No permanent evidence storage on device rule documented.
- Evidence view/download audit route noted.

## Phase D — documents

Status: implemented as UI/workflow foundation.

- Update document requirements route shape.
- Record documents given route shape.
- Signature/confirmation capture placeholder.
- Request confirmation link route shape.
- Follow-up needed handling.
- Document history.

## Phase E — polish and alerts

Status: implemented as UI/workflow foundation.

- Alerts summary.
- Device list and logout direction.
- PIN/biometric local unlock direction.
- Empty, loading and error states started.
- Demo mode support.
- Connected site management started.

## Pre-build tidy pass

Status: completed foundation pass.

- GitHub Actions changed to manual-only while stabilising.
- Android SDK setup no longer asks for obsolete `tools` package.
- `MainActivity.kt` reduced to an entry point.
- Models moved into `model/`.
- Demo data moved into `data/DemoRepository.kt`.
- API discovery/login/upload route shape moved into `api/AdultSafeApi.kt`.
- Theme and reusable components moved into `ui/theme/` and `ui/components/`.
- V1-critical tracks documented.
- Release signing prepared through environment variables only.

## Deferred

- Cross-group shared volunteer/parent records.
- Offline ID evidence capture.
- iOS app.
- Hosted SaaS model.
