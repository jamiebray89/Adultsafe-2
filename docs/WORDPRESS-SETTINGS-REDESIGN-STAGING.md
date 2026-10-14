# AdultSafe WordPress Settings redesign — staged integration, 14 Oct 2026

## Status
Implementation has **started in the WordPress plugin development source** based on WordPress 1.39.8. **NOT released/deployed; no Android APK build requested.** The Android repository is not the WordPress plugin repository. Keep its Android main branch functional and do not presume the staged WordPress source is present in this GitHub repository.

## Agreed UX
- Real server-rendered Settings category home and dedicated subpages: People & Roles, DBS & ID, Documents, Notifications, Dashboard, Access & Devices, Demo & Testing, Advanced & Privacy. Dedicated Approvals screen.
- Search Settings home; per-category Save; back navigation; preserve every existing setting and record.
- WordPress Administrators retain protected security, demo, user-access and sensitive policy controls.
- AdultSafe Managers can configure normal operations and approve specialist requests. DBS Leads can change selected DBS operational options; Document Admins can edit document definitions. The WordPress server enforces these boundaries on POST.
- Default `direct` changes. Optional `selective` approvals configured per WordPress installation (currently one AdultSafe workspace/site). Specialist proposals must remain inactive until a distinct Manager/Admin approves. Approvals cannot be self-approved; decline and stale-change conflict fail closed.

## Staged WordPress implementation (not yet on GitHub)
- Category routes `?dbs_tab=settings&as_settings={people|dbs|documents|notifications|dashboard|access|demo|advanced|approvals}`; render only authorised page.
- Form fields are allowlisted by identity/role at the server. Manager/specialists cannot change DBS validity, minimum ID evidence, signing declaration or notification recipient list; only WordPress Administrators can.
- Approved settings change queues stored separately as `adultsafe_settings_change_requests`; explicit policy in `adultsafe_settings_approval_mode` / `adultsafe_settings_approval_sections`.
- Activity audit and Manager-only settings-approval dashboard notice; Documents heading remains visible on zero-action dashboards.
- Demo AJAX enable/repair/disable/reset code preserved from 1.39.8.
- Local PHP syntax and mocked WordPress role/POST tests passed; WordPress runtime integration against Hawk **not yet tested**.

## Android dependencies to revisit before next APK
- API settings permissions: `manage_settings`, `manage_settings_dbs`, `manage_settings_documents`, `manage_settings_approvals` in WordPress permissions_map. Existing Android app may ignore these new fields until a future Settings UI.
- Approval pending state must not be shown as saved; Android writes to settings need matching endpoints and server-authoritative checks before being enabled.
- Android permanent signing keystore + GitHub secrets still not configured. Reuse signing cert and increment versionCode on next distributable APK.
- Test WordPress v1.39.8 baseline demo data (40/40) and live API compatibility; no live personal ID files during initial tests.
- Role-based automatic document requirements remains a **future separate feature** (issue #1); do not silently modify existing obligations.

## Release gates
1. Record and review WordPress source in its own repository (none linked at the time of this draft), package a **new** version only after PHP source and QA pass. Preserve the 1.39.8 ZIP as rollback.
2. Verify WordPress UI and role/approval flows on the isolated Hawk site: authorised and unauthorised form submission, pending/approve/reject/stale, demo controls, dashboard counters.
3. Verify Android integration on test people only.
4. Configure stable signing and build **only on explicit approval**.
