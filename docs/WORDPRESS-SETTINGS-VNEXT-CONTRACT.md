# AdultSafe — WordPress Settings vNext contract and release gates
Date: 15 October 2026. Status: **design/QA contract; NOT a committed PHP implementation or a release**.

## Source protection
WordPress baseline: AdultSafe Records v1.39.8 (`adultsafe-records-v1.39.8-demo-data-fix.zip`). Earlier source-only development snapshot: `AdultSafe-Settings-Development-Snapshot.zip`. Those conversation artifacts are **not presently available in this repository** and must be recovered intact and independently inspected before any WordPress plugin modification. Preserve both originals as rollback reference. Do not reconstruct a replacement from this document and claim it is the recovered source. Store plugin source in a separate **private** WordPress repository, not this Android repository.

## UI layout
Replace the huge WordPress Settings tab with a searchable home and dedicated server-rendered pages:
1. People & Roles
2. DBS & ID
3. Documents
4. Notifications
5. Dashboard
6. Access & Devices
7. Demo & Testing
8. Advanced & Privacy
9. Approvals (only visible to eligible approvers).

Only show categories accessible to the signed-in user. Each category has Back, Save and Cancel as appropriate and a concise explanation. A hidden category is **not** security: permission-check every GET, POST and action on the WordPress server. Preserve existing setting names, stored data, document versions, signed records, user permissions and demo records.

## WordPress role restrictions
- Administrator: all, including protected policies, demo controls and access/device security.
- AdultSafe Manager: agreed non-sensitive operational categories and approval of *other people's* permitted proposals.
- DBS Lead: specifically allowlisted DBS operational options, not validity-years, ID evidence minimum, signer declaration or notification recipient list.
- Document Admin: specifically allowlisted document definitions and operational options, not protected signing declaration or global security policy.
- Viewer, ID Checker and demo-only: cannot change Settings.
- Default deny for any undeclared POST field, not just hidden controls. Enforce server-side field allowlists and check each submitted key. WordPress Administrators do not automatically bypass AdultSafe data-access checks.

## Save modes
- Per-WordPress-installation default: `direct` for authorised specialist settings.
- Optional `selective`: Administrator chooses sections requiring a distinct authorised Manager/Administrator to approve specialist changes.
- A proposal with `pending_approval` has **no effect** until approval; never label it 'saved'.
- For each request retain actor, section, field-level change set, timestamp, base revision and current status. Reject self-approval, stale revisions, replay, unknown fields, forged POST keys and unprivileged approvals. Rejection must not change active settings.
- Approvals must run the same field-permission checks again at application time and create an audit record.

## History, restore defaults and documents
- Change history per category: date/time, actor, changed field names, request/approval status and independent approver. Mask sensitive values; constrain who may read it.
- Restore defaults: initially Administrator-only, category-scoped, reviewable before confirm, never destructive to people, ID photos, DBS records, existing signatures, historical document versions or audit logs; record every reset.
- Always render Documents dashboard section and a quiet 'No outstanding document actions' message when all requirements are `Not required` / none are outstanding. Do not generate distracting zero-count tiles. Verify both WordPress and Android views.
- Do **not** silently implement future automatic document requirements by role (tracked in issue #1).

## API integration
Existing Android contract: `/wp-json/adultsafe/v1` (`GET /app/me`, `GET /settings/meta`, `GET /dashboard`, existing person and document routes).
Planned capabilities include `manage_settings`, `manage_settings_dbs`, `manage_settings_documents`, `manage_settings_approvals`. These must be returned by an actual verified WordPress API response before the app enables corresponding controls.
**No Android Settings-write endpoints are confirmed**, so do not fabricate REST URLs or client write calls; document and implement the server contract first. Explicitly distinguish direct saved, pending approval, approved, rejected and revision conflict responses.

## QA acceptance — all must pass before full release
- Verify exact v1.39.8 baseline source and snapshot contents, then commit tested WordPress changes to their own private repository.
- Check existing 40 demo people still survive; test demo enable, repair, disable and reset without changing real records.
- Administrator/Manager/DBS Lead/Document Admin/ID Checker/Viewer/demo-only role matrix tests on Hawk, including forged form fields and direct page/action access.
- Direct-save, pending, approve, reject, self-approve-denied, stale/replayed-change-denied and audit-history tests on real WordPress runtime.
- Test settings change history, category reset, navigation and zero-action Documents heading on desktop and phone width.
- Back up actual WordPress database and files; no live personal ID images in early tests. Never uninstall AdultSafe to upgrade the WordPress plugin.
- Verify Android API compatibility and real role-aware workflows, then Android compile/artifact/signature check *only after user authorises build*.
- Persistent APK signing certificate and four GitHub Actions secrets must be installed privately; verify versionCode and matching signing fingerprints before claiming install-over support.
- Do not start APK build, deployment or release automatically from this prep branch.
