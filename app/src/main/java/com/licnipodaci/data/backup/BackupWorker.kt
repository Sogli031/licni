package com.licnipodaci.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.licnipodaci.data.PodaciManagerProvider

/**
 * Pozadinski posao koji šalje backup na Google Drive. Pokreće ga [BackupScheduler]
 * (posle izmene + dnevno), uz uslov da ima mreže.
 *
 * Pravila „prazno ne gazi popunjeno" i „sveža instalacija ne prepisuje zatečeni
 * backup" su u [GoogleDriveBackup.backup]; ovde se `confirmedOverwrite` nikad ne
 * šalje, pa se takav backup tiho preskoči umesto da pita korisnika.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val backup = GoogleDriveBackup(applicationContext)

        if (backup.signInState.value !is SignInState.SignedIn) {
            Log.d(TAG, "Preskačem auto-backup: korisnik nije prijavljen")
            return Result.success()
        }

        val manager = PodaciManagerProvider.get(applicationContext)
        if (manager.lokalniPodaciOsteceni) {
            // Lokalni fajl je bio oštećen: dok korisnik ne vrati podatke, ne gazimo oblak
            Log.d(TAG, "Preskačem auto-backup: lokalni podaci su bili oštećeni")
            backup.markAutoBackupFailed()
            return Result.success()
        }

        val success = try {
            backup.backup(jsonProvider = { manager.izvozJson() }, confirmedOverwrite = false)
        } catch (e: Exception) {
            Log.w(TAG, "Auto-backup pukao: ${e.message}")
            false
        }

        if (success) {
            Log.d(TAG, "Auto-backup uspešan")
            return Result.success()
        }

        return when (val state = backup.syncState.value) {
            // Obe su odluke koje mora da donese korisnik na ekranu - ponavljanje ne pomaže.
            SyncState.CloudNotAdopted -> {
                Log.d(TAG, "Preskačem auto-backup: oblak nije preuzet na ovoj instalaciji")
                backup.markAutoBackupFailed() // u dijalogu se vidi da backup čeka odluku
                Result.success()
            }
            SyncState.EmptyData -> {
                Log.d(TAG, "Preskačem auto-backup: nema podataka")
                Result.success()
            }
            else -> {
                Log.w(TAG, "Auto-backup neuspešan: ${(state as? SyncState.Error)?.message}")
                if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
                    backup.markAutoBackupFailed()
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val MAX_RETRY_ATTEMPTS = 7
    }
}
