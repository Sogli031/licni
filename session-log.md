## 2026-05-22 23:30 [saved]
Goal: Build Licni podaci Android app from plan.
Decisions:
- Crash logs are internal-only for this app because public Documents can expose plaintext personal data.
- Imported IDs must be unique canonical UUIDs because IDs drive list keys and navigation.
- Import reads are capped at 1 MiB because user-selected files are untrusted.
Rejected:
- Public crash_log.txt for this app.
- Arbitrary imported ID strings.
Open:
- Verify SAF export/import manually.

## 2026-05-23 15:53 [saved]
Goal: Finish Google Drive backup and remove local import/export.
Decisions:
- Drive backup uses `DRIVE_APPDATA` because personal data should stay app-private.
- Drive restore keeps a destructive confirmation because it replaces all local items.
- Empty backup payloads are skipped because they could overwrite useful cloud data.
- Local file picker import/export was removed because the user chose Drive-only backup.
Rejected:
- Keeping SAF import/export menu entries.
Open:
- Verify Drive sign-in/backup/restore on device.

## 2026-05-23 16:07 [saved]
Goal: Fix release build after Drive dependency work.
Decisions:
- Added explicit `androidx.fragment:fragment:1.8.5` because `play-services-auth:20.7.0` resolves Fragment 1.0.0.
- Verified release lint because the user-facing APK builder uses release assembly.
Rejected:
- Suppressing lint instead of fixing dependency resolution.
Open:
- Keep Gradle daemon stopped if Windows locks `app/build`.

## 2026-05-23 16:14 [saved]
Goal: Move backup UI out of top-bar overflow.
Decisions:
- Backup opens from a bottom-left green FAB because the top-bar three-dot menu was visually rejected.
- Cloud Backup uses a centered dialog because status and actions need more space than a dropdown.
Rejected:
- Restoring the top-bar overflow entry.
Open:
- Manually inspect dialog spacing on device.

## 2026-05-23 16:36 [saved]
Goal: Polish app-wide UI using Cloud Backup style.
Decisions:
- Main, detail, and edit screens use bordered dark panels because the backup dialog set the preferred visual style.
- Theme typography uses zero letter spacing because negative spacing made compact UI less stable.
Rejected:
- Changing data, backup, notification, or navigation behavior during visual polish.
Open:
- Manually inspect screens on device.
