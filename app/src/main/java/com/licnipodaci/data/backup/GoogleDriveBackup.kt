package com.licnipodaci.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val TAG = "GoogleDriveBackup"
private const val BACKUP_FILE_NAME = "licni_podaci_backup.json"
private const val APP_DATA_FOLDER = "appDataFolder"
private const val PREFS_NAME = "backup_prefs"
private const val KEY_LAST_SYNC_TIME = "last_sync_time"
private const val KEY_CLOUD_ADOPTED = "cloud_adopted" // staro: jedan flag za sve naloge
private const val KEY_ADOPTED_ACCOUNT = "cloud_adopted_account"
private const val KEY_LAST_SYNC_ACCOUNT = "last_sync_account"
private const val KEY_AUTO_FAILED = "auto_backup_failed"

// Deli ga svaka instanca (UI i pozadinski posao), da se backup/restore nikad ne preklope
private val syncMutex = Mutex()

/** Glavni backup fajl u oblaku. */
private data class BackupFileInfo(val id: String, val modifiedMillis: Long?, val md5: String?)

/** Prethodna verzija podataka sačuvana u oblaku; [timestampMillis] je kad je ta verzija upisana. */
data class BackupVersion(val id: String, val timestampMillis: Long)

class GoogleDriveBackup(private val context: Context) {

    private val _signInState = MutableStateFlow<SignInState>(SignInState.SignedOut)
    val signInState: StateFlow<SignInState> = _signInState

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime

    // Posle prijave: u oblaku postoji backup koji ova instalacija još nije preuzela
    private val _restoreOffer = MutableStateFlow(false)
    val restoreOffer: StateFlow<Boolean> = _restoreOffer

    private var driveService: Drive? = null

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            _signInState.value = SignInState.SignedIn(account.email ?: "Nepoznato")
            initDriveService(account)
        }
        refreshLastSync()
    }

    private fun currentEmail(): String? =
        (_signInState.value as? SignInState.SignedIn)?.email?.lowercase()

    /** Poslednji backup se prikazuje samo ako je rađen za nalog koji je trenutno prijavljen. */
    private fun refreshLastSync() {
        val time = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        val email = currentEmail()
        val forThisAccount = when {
            time <= 0 || email == null -> false
            prefs.contains(KEY_LAST_SYNC_ACCOUNT) -> prefs.getString(KEY_LAST_SYNC_ACCOUNT, null) == email
            else -> isCloudAdopted() // stariji zapis bez naloga: važi ako je oblak preuzet za ovaj nalog
        }
        _lastSyncTime.value = if (forThisAccount) time else null
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            _signInState.value = SignInState.SignedIn(account.email ?: "Nepoznato")
            initDriveService(account)
        } else {
            _signInState.value = SignInState.SignedOut
        }
        refreshLastSync()
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut()
                _signInState.value = SignInState.SignedOut
                _restoreOffer.value = false
                driveService = null
                refreshLastSync()
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }

    private fun initDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Licni podaci")
            .build()
    }

    /**
     * Da li je ova instalacija "preuzela" cloud backup TRENUTNO PRIJAVLJENOG naloga: uspešan
     * restore, uspešan backup kad u oblaku nije bilo fajla, ili izričita potvrda korisnika da
     * prepiše postojeći backup. Preuzimanje važi samo za nalog za koji je učinjeno - drugi
     * nalog ima svoj oblak, koji se ne prepisuje bez pitanja.
     */
    fun isCloudAdopted(): Boolean {
        val email = currentEmail() ?: return false
        if (!prefs.contains(KEY_ADOPTED_ACCOUNT)) {
            // Stare instalacije su imale jedan flag za sve naloge: pripiši ga trenutnom nalogu
            val legacy = if (prefs.contains(KEY_CLOUD_ADOPTED)) {
                prefs.getBoolean(KEY_CLOUD_ADOPTED, false)
            } else {
                prefs.getLong(KEY_LAST_SYNC_TIME, 0L) > 0
            }
            prefs.edit()
                .putString(KEY_ADOPTED_ACCOUNT, if (legacy) email else "")
                .remove(KEY_CLOUD_ADOPTED)
                .apply()
        }
        return prefs.getString(KEY_ADOPTED_ACCOUNT, null) == email
    }

    /** Poziva se kada su podaci iz oblaka uspešno vraćeni i sačuvani na uređaju. */
    fun markCloudAdopted() {
        val email = currentEmail() ?: return
        prefs.edit().putString(KEY_ADOPTED_ACCOUNT, email).remove(KEY_CLOUD_ADOPTED).apply()
        _restoreOffer.value = false
    }

    /** Automatski backup ne radi (neuspeh posle svih pokušaja ili čeka odluku korisnika). */
    fun markAutoBackupFailed() {
        prefs.edit().putBoolean(KEY_AUTO_FAILED, true).apply()
    }

    fun autoBackupFailed(): Boolean = prefs.getBoolean(KEY_AUTO_FAILED, false)

    fun dismissRestoreOffer() {
        _restoreOffer.value = false
    }

    /** Posle uspešne prijave: ako u oblaku postoji backup koji nije preuzet, ponudi restore. */
    suspend fun checkRestoreOfferAfterSignIn() {
        if (isCloudAdopted()) return
        if (cloudBackupStatus() == CloudBackupStatus.EXISTS) {
            _restoreOffer.value = true
        }
    }

    /** Neuspelo listanje (mreža/nalog) je UNKNOWN - nikad se ne tumači kao "oblak je prazan". */
    suspend fun cloudBackupStatus(): CloudBackupStatus = syncMutex.withLock {
        val drive = driveService ?: return@withLock CloudBackupStatus.UNKNOWN
        withContext(Dispatchers.IO) {
            try {
                if (findBackupFile(drive) != null) CloudBackupStatus.EXISTS else CloudBackupStatus.MISSING
            } catch (e: Exception) {
                Log.e(TAG, "Cloud check failed", e)
                CloudBackupStatus.UNKNOWN
            }
        }
    }

    /**
     * @param jsonProvider podaci se čitaju tek kad backup dobije red (posle [syncMutex]),
     * da se ne pošalje stanje koje je u međuvremenu zastarelo (npr. posle restore-a).
     * @param confirmedOverwrite korisnik je izričito potvrdio da postojeći cloud backup
     * sme da se prepiše iako ga ova instalacija nije preuzela.
     */
    suspend fun backup(jsonProvider: () -> String, confirmedOverwrite: Boolean = false): Boolean =
        syncMutex.withLock {
            val drive = driveService ?: return@withLock false
            val jsonData = jsonProvider()

            // Prazno nikad ne gazi popunjeno
            if (BackupPayloadPolicy.shouldSkipBackup(jsonData)) {
                Log.w(TAG, "Backup preskocen - nema podataka")
                _syncState.value = SyncState.EmptyData
                return@withLock false
            }

            withContext(Dispatchers.IO) {
                try {
                    _syncState.value = SyncState.Syncing

                    val bytes = jsonData.toByteArray(Charsets.UTF_8)
                    val localMd5 = BackupPayloadPolicy.md5Hex(bytes)
                    val existing = findBackupFile(drive)

                    if (existing != null && existing.md5 == localMd5) {
                        // Oblak već ima tačno ove podatke: bez novog upisa, da ponovljeni backup
                        // istih podataka ne istiskuje prave prethodne verzije.
                        markCloudAdopted()
                        recordBackupSuccess()
                        _syncState.value = SyncState.Success
                        return@withContext true
                    }

                    if (existing != null && !isCloudAdopted()) {
                        if (!confirmedOverwrite) {
                            _syncState.value = SyncState.CloudNotAdopted
                            return@withContext false
                        }
                        markCloudAdopted()
                    }

                    val content = ByteArrayContent("application/json", bytes)

                    val saved = if (existing != null) {
                        // Ako kopija ne uspe, baca izuzetak i backup se prekida pre prepisivanja
                        createPrevCopy(drive, existing)
                        drive.files().update(existing.id, null, content)
                            .setFields("id, md5Checksum")
                            .execute()
                    } else {
                        val fileMetadata = com.google.api.services.drive.model.File().apply {
                            name = BACKUP_FILE_NAME
                            parents = listOf(APP_DATA_FOLDER)
                        }
                        drive.files().create(fileMetadata, content)
                            .setFields("id, md5Checksum")
                            .execute()
                    }

                    // Provera da je u oblak stigao tačno ono što je poslato
                    val remoteMd5 = saved.md5Checksum
                    if (remoteMd5 != null && remoteMd5 != localMd5) {
                        throw IOException("Provera backup-a nije uspela (sadržaj se ne poklapa)")
                    }

                    markCloudAdopted()
                    recordBackupSuccess()
                    prunePrevCopies(drive)
                    _syncState.value = SyncState.Success
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Backup failed", e)
                    _syncState.value = SyncState.Error(e.message ?: "Backup nije uspeo")
                    false
                }
            }
        }

    private fun recordBackupSuccess() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIME, now)
            .putString(KEY_LAST_SYNC_ACCOUNT, currentEmail())
            .putBoolean(KEY_AUTO_FAILED, false)
            .apply()
        _lastSyncTime.value = now
    }

    suspend fun restore(): String? = syncMutex.withLock {
        val drive = driveService ?: return@withLock null

        withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                val file = findBackupFile(drive)
                if (file == null) {
                    _syncState.value = SyncState.Idle
                    return@withContext null
                }

                val data = downloadFile(drive, file.id)
                _syncState.value = SyncState.Success
                data
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Restore nije uspeo")
                null
            }
        }
    }

    /** Prethodne verzije podataka iz oblaka, najnovija prva. `null` = listanje nije uspelo. */
    suspend fun listVersions(): List<BackupVersion>? = syncMutex.withLock {
        val drive = driveService ?: return@withLock null
        withContext(Dispatchers.IO) {
            try {
                listAllFiles(drive)
                    .mapNotNull { (id, name) ->
                        BackupPayloadPolicy.prevCopyTimestamp(BACKUP_FILE_NAME, name)
                            ?.let { BackupVersion(id, it) }
                    }
                    .distinctBy { it.timestampMillis }
                    .sortedByDescending { it.timestampMillis }
            } catch (e: Exception) {
                Log.e(TAG, "Listing versions failed", e)
                null
            }
        }
    }

    /** Sadržaj jedne prethodne verzije; `null` ako preuzimanje nije uspelo. Ništa ne menja u oblaku. */
    suspend fun downloadVersion(versionId: String): String? = syncMutex.withLock {
        val drive = driveService ?: return@withLock null
        withContext(Dispatchers.IO) {
            try {
                downloadFile(drive, versionId)
            } catch (e: Exception) {
                Log.e(TAG, "Version download failed", e)
                null
            }
        }
    }

    /**
     * Traži glavni backup fajl po tačnom imenu. Ništa ne briše - ako postoji više fajlova
     * sa istim imenom, koristi se najnoviji, a ostali se ne diraju. Greška pri listanju
     * se prosleđuje pozivaocu (ne tumači se kao "nema fajla").
     */
    private fun findBackupFile(drive: Drive): BackupFileInfo? {
        val result = drive.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setOrderBy("modifiedTime desc")
            .setFields("files(id, name, modifiedTime, md5Checksum)")
            .execute()

        return result.files.orEmpty()
            .firstOrNull { it.name == BACKUP_FILE_NAME && it.id != null }
            ?.let { BackupFileInfo(it.id, it.modifiedTime?.value, it.md5Checksum) }
    }

    private fun downloadFile(drive: Drive, fileId: String): String {
        val outputStream = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        return outputStream.toString("UTF-8")
    }

    /**
     * Pre prepisivanja čuva postojeći fajl kao "<ime>_prev_<vreme>.json", gde je vreme kad je
     * ta verzija upisana u oblak. Baca izuzetak ako ne uspe.
     */
    private fun createPrevCopy(drive: Drive, file: BackupFileInfo) {
        val copyMetadata = com.google.api.services.drive.model.File().apply {
            name = BackupPayloadPolicy.prevCopyName(
                BACKUP_FILE_NAME,
                file.modifiedMillis ?: System.currentTimeMillis()
            )
            parents = listOf(APP_DATA_FOLDER)
        }
        try {
            drive.files().copy(file.id, copyMetadata).execute()
        } catch (e: Exception) {
            Log.w(TAG, "files.copy nije uspeo, pokusavam download + create", e)
            val content = ByteArrayContent.fromString("application/json", downloadFile(drive, file.id))
            drive.files().create(copyMetadata, content).execute()
        }
    }

    private fun listAllFiles(drive: Drive): List<Pair<String, String>> {
        val files = mutableListOf<Pair<String, String>>()
        var pageToken: String? = null
        do {
            val result = drive.files().list()
                .setSpaces(APP_DATA_FOLDER)
                .setQ("trashed = false")
                .setFields("nextPageToken, files(id, name)")
                .setPageSize(200)
                .setPageToken(pageToken)
                .execute()
            result.files.orEmpty().forEach { file ->
                val id = file.id
                val name = file.name
                if (id != null && name != null) files.add(id to name)
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
        return files
    }

    /** Čuva najnovije kopije i po jednu za svaki dan u poslednjih 30 dana. Neuspeh čišćenja ne obara backup. */
    private fun prunePrevCopies(drive: Drive) {
        try {
            val toDelete = BackupPayloadPolicy.prevCopiesToDelete(
                BACKUP_FILE_NAME,
                listAllFiles(drive),
                System.currentTimeMillis()
            )
            toDelete.forEach { id ->
                try {
                    drive.files().delete(id).execute()
                } catch (e: Exception) {
                    Log.w(TAG, "Neuspesno brisanje stare kopije: $id", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ciscenje starih kopija nije uspelo", e)
        }
    }
}

sealed class SignInState {
    data object SignedOut : SignInState()
    data class SignedIn(val email: String) : SignInState()
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data object EmptyData : SyncState()
    /** U oblaku postoji backup koji ova instalacija nije preuzela - traži se odluka korisnika. */
    data object CloudNotAdopted : SyncState()
    data class Error(val message: String) : SyncState()
}

enum class CloudBackupStatus { EXISTS, MISSING, UNKNOWN }
