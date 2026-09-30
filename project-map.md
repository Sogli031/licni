# Project Map
_Generated: 2026-05-23 16:36 | Staleness: timestamps_

## Directory Structure
app/ — Android application module with Compose UI, persistence, notifications, Google Drive backup, and tests.
app/src/main/java/com/licnipodaci/data/ — Domain model, JSON validation, templates, date parsing, and local persistence.
app/src/main/java/com/licnipodaci/data/backup/ — Google Sign-In and Google Drive AppData backup/restore implementation.
app/src/main/java/com/licnipodaci/notif/ — Expiry reminder scheduling, boot rescheduling, and notification display.
app/src/main/java/com/licnipodaci/ui/ — Material 3 Compose UI screens, navigation, theme, and category styling.
app/src/main/java/com/programi/crashlog/ — Shared crash log storage component configured for internal-only logs here.
app/src/main/res/ — App name, launcher icon, Android theme resources.
app/src/test/java/com/licnipodaci/ — JUnit tests for JSON import/export, templates, and backup payload policy.
docs/ — Original design and implementation plan; backup details there are partly superseded by current Drive-only behavior.
gradle/ — Gradle wrapper files.
app/build/ and build/ — Generated outputs; do not use for source-of-truth mapping.

## Key Files
settings.gradle.kts — Defines the single-module `LicniPodaci` Gradle project.
build.gradle.kts — Root Android/Kotlin/Compose plugin versions.
app/build.gradle.kts — Compile SDK, Compose dependencies, Gson, coroutines, Google Sign-In, Drive API, and explicit Fragment override.
app/src/main/AndroidManifest.xml — App permissions, `allowBackup=false`, MainActivity, notification receivers, and boot receiver.
app/src/main/java/com/licnipodaci/MainActivity.kt — Creates `PodaciManager`, `GoogleDriveBackup`, notification permission request, and Google sign-in launcher.
app/src/main/java/com/licnipodaci/LicniPodaciApplication.kt — Installs sanitized internal crash logging and creates the expiry notification channel.
app/src/main/java/com/licnipodaci/data/Model.kt — Core `Stavka`, `Polje`, and `Kategorija` (LICNI_DOKUMENT, VOZILO, FINANSIJE, SIFRE, OSTALO) data model.
app/src/main/java/com/licnipodaci/data/PodaciManager.kt — Owns private `filesDir/licni_podaci.json`, observable state, CRUD, import parsing, replacement, and notification rescheduling.
app/src/main/java/com/licnipodaci/data/PodaciJson.kt — Gson serialization plus strict import validation for required structure, UUID IDs, and duplicate IDs.
app/src/main/java/com/licnipodaci/data/Sabloni.kt — Built-in templates for documents, vehicle, finance, passwords (Google/Instagram/Facebook/TikTok/Wi-Fi/custom), medical, and empty item flows.
app/src/main/java/com/licnipodaci/data/SifreUtil.kt — Password-category helpers: custom template name and auto-labeling of the identity field as E-mail (contains @) or Korisničko ime.
app/src/main/java/com/licnipodaci/data/DatumUtil.kt — Expiry-field detection and strict date parsing/formatting for reminder logic.
app/src/main/java/com/licnipodaci/data/backup/GoogleDriveBackup.kt — Google account state, Drive service initialization, AppData upload/download, duplicate cleanup, and sync state.
app/src/main/java/com/licnipodaci/data/backup/BackupPayloadPolicy.kt — Empty-payload guard, prev-copy naming/retention (5 newest + 1/day for 30 days), MD5 helper.
app/src/main/java/com/licnipodaci/data/backup/BackupWorker.kt, BackupScheduler.kt — WorkManager auto-backup (1 min after change + daily), pauses while local data is marked corrupt.
app/src/main/java/com/licnipodaci/ui/navigation/NavGraph.kt — Four-screen Compose navigation: home chooser (Podaci / Sifre), section list, details, edit; routes carry the `Sekcija`.
app/src/main/java/com/licnipodaci/ui/screens/SifreGrupaScreen.kt — All passwords for one service (grouped by name), account cards show e-mail/username, FAB adds another account for the same service.
app/src/main/java/com/licnipodaci/ui/screens/PocetnaScreen.kt — Home screen with two big cards (Podaci, Sifre) showing counts, plus the Backup FAB.
app/src/main/java/com/licnipodaci/ui/screens/ListaScreen.kt — Main list, overview panel, bordered search, category filters, item cards, bottom Backup FAB, and bottom New Item FAB.
app/src/main/java/com/licnipodaci/ui/screens/IzvozUvozMeni.kt — Centered Cloud Backup dialog: sign in, account status, backup, restore with confirmation, sign out.
app/src/main/java/com/licnipodaci/ui/screens/UrediScreen.kt — Template selection and edit form with bordered dark panels, dynamic fields, date picker, and save behavior.
app/src/main/java/com/licnipodaci/ui/screens/DetaljiScreen.kt — Detail display with bordered dark panels, copy-to-clipboard, expiry warnings, edit navigation, and delete confirmation.
app/src/main/java/com/licnipodaci/notif/ObavestenjeManager.kt — Schedules reminders 30/15/10/5/1 days before expiry at 09:00 using `setAndAllowWhileIdle`.
app/src/main/java/com/licnipodaci/notif/BootReceiver.kt — Re-reads JSON directly and reschedules alarms after boot/package update.
app/src/main/java/com/licnipodaci/notif/ObavestenjeReceiver.kt — Builds the reminder notification and opens MainActivity when tapped.
app/src/test/java/com/licnipodaci/data/PodaciJsonTest.kt — Validates JSON round-trip and rejects malformed or unsafe imports.
app/src/test/java/com/licnipodaci/data/backup/BackupPayloadPolicyTest.kt — Covers empty-vs-nonempty Drive backup payload behavior.

## Critical Constraints
- This checkout intentionally has no Git repository; use timestamp freshness for `project-map.md` unless the user later asks for Git.
- Verification command from project root: `.\gradlew.bat assembleDebug testDebugUnitTest`.
- Personal data is plaintext in private app storage; `allowBackup=false` and internal-only crash logs reduce cloud/log exposure but do not encrypt data.
- Crash logs must remain internal-only and sanitized; do not enable public Documents crash logs for this app.
- Local file picker import/export was removed; do not re-add SAF `CreateDocument` / `OpenDocument` menu entries unless explicitly requested.
- Backup access is a bottom-left green `Backup` FAB plus centered Cloud Backup dialog; do not re-add the top-bar three-dot overflow.
- General app UI now follows the Cloud Backup visual language: dark panels, subtle borders, cyan/category accents, and compact readable cards.
- Google Drive backup uses `DRIVE_APPDATA`; the backup file is app-private and not visible as a normal Drive file.
- Drive restore is destructive and must keep a confirmation dialog before `manager.zameniSve(...)`.
- Empty JSON backup payloads are skipped unless `force=true`, to avoid replacing useful Drive data with `[]`.
- Imported/restored item IDs must be canonical UUIDs and unique because they are list keys and navigation arguments.
- Expiry reminders apply when field labels contain `Važi do`, `Datum isteka`, or `Datum isteka registracije` case-insensitively.
- Date values remain strings; parser supports daily formats plus `MM/yyyy` / `MM/yy`, with month-only dates treated as the last day of that month.
- Reminder defaults are fixed at 30, 15, 10, 5, and 1 day before expiry at 09:00; there is no settings screen.
- `ObavestenjeManager.otkazi` only scans 32 fields per item; items exceeding that can leave later field alarms uncancelled.
- Real Google sign-in, Drive backup, and Drive restore still require manual device/OAuth validation.
- Real notification delivery still requires manual device or emulator validation.
- Keep explicit `androidx.fragment:fragment:1.8.5` or newer; `play-services-auth:20.7.0` otherwise resolves Fragment 1.0.0 and release lint fails.

## Hot Files
app/src/main/java/com/licnipodaci/ui/screens/IzvozUvozMeni.kt, app/src/main/java/com/licnipodaci/data/backup/GoogleDriveBackup.kt, app/src/main/java/com/licnipodaci/MainActivity.kt, app/src/main/java/com/licnipodaci/data/PodaciManager.kt, app/src/main/java/com/licnipodaci/data/PodaciJson.kt, app/src/main/java/com/licnipodaci/notif/ObavestenjeManager.kt, app/src/main/AndroidManifest.xml, app/build.gradle.kts
