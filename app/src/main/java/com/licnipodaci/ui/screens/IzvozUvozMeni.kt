package com.licnipodaci.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.LokalnaKopija
import com.licnipodaci.data.Stavka
import com.licnipodaci.data.backup.BackupVersion
import com.licnipodaci.data.backup.GoogleDriveBackup
import com.licnipodaci.data.backup.SignInState
import com.licnipodaci.data.backup.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val BackupGreen = Color(0xFF00C853)
private val RestoreBlue = Color(0xFF40BFF2)

@Composable
fun CloudBackupDialog(
    manager: PodaciManager,
    driveBackup: GoogleDriveBackup,
    onGoogleSignIn: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signInState by driveBackup.signInState.collectAsState()
    val syncState by driveBackup.syncState.collectAsState()
    val lastSyncTime by driveBackup.lastSyncTime.collectAsState()
    val sinhronizacijaUToku = syncState is SyncState.Syncing
    val akcije = remember(manager, driveBackup) { CloudAkcije(context, scope, manager, driveBackup) }
    // Cita se iznova posle svake sinhronizacije (auto-backup ga postavlja iz pozadine)
    val autoBackupProblem = remember(syncState, signInState) { driveBackup.autoBackupFailed() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cloud Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zatvori")
                    }
                }

                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                StatusPanel(
                    signInState = signInState,
                    syncState = syncState,
                    lastSyncTime = lastSyncTime,
                    autoBackupProblem = autoBackupProblem && signInState is SignInState.SignedIn
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Akcije",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        when (signInState) {
                            SignInState.SignedOut -> {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onGoogleSignIn()
                                    },
                                    enabled = !sinhronizacijaUToku,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BackupGreen,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Prijavi Google nalog", fontWeight = FontWeight.Bold)
                                }
                            }

                            is SignInState.SignedIn -> {
                                Button(
                                    onClick = { akcije.backup() },
                                    enabled = !sinhronizacijaUToku,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BackupGreen,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (sinhronizacijaUToku) "Backup u toku..." else "Backup",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { akcije.restore() },
                                    enabled = !sinhronizacijaUToku,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RestoreBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (sinhronizacijaUToku) "Vraćanje u toku..." else "Restore",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { akcije.otvoriVerzije() },
                                    enabled = !sinhronizacijaUToku,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.History, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Prethodne verzije", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            driveBackup.signOut()
                                            Toast.makeText(context, "Google nalog odjavljen", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !sinhronizacijaUToku,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Odjavi se", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    CloudAkcijeDijalozi(akcije)
}

/**
 * Ponuda za vraćanje podataka odmah posle prijave, kada u oblaku postoji backup koji ova
 * instalacija još nije preuzela (npr. sveže instalirana aplikacija).
 */
@Composable
fun CloudRestorePonuda(
    manager: PodaciManager,
    driveBackup: GoogleDriveBackup
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ponuda by driveBackup.restoreOffer.collectAsState()
    val akcije = remember(manager, driveBackup) { CloudAkcije(context, scope, manager, driveBackup) }
    val signInState by driveBackup.signInState.collectAsState()
    var ostecenSakriven by remember { mutableStateOf(false) }

    if (manager.lokalniPodaciOsteceni && !ostecenSakriven) {
        AlertDialog(
            onDismissRequest = { ostecenSakriven = true },
            title = { Text("Podaci na telefonu su oštećeni") },
            text = {
                Text(
                    "Fajl sa podacima nije mogao da se pročita. Oštećeni fajl je sačuvan sa strane, " +
                        "a backup u oblak je pauziran da ne prepiše dobre podatke. " +
                        if (signInState is SignInState.SignedIn) {
                            "Vratite podatke iz oblaka."
                        } else {
                            "Prijavite Google nalog (dugme Backup) pa vratite podatke iz oblaka."
                        }
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (signInState is SignInState.SignedIn) {
                        Button(
                            onClick = { akcije.restore() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Vrati iz oblaka") }
                    }
                    TextButton(
                        onClick = { ostecenSakriven = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Kasnije") }
                    TextButton(
                        onClick = { manager.nastaviBezVracanja() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Nastavi prazno (uključi backup)") }
                }
            }
        )
    }

    if (ponuda) {
        AlertDialog(
            onDismissRequest = { driveBackup.dismissRestoreOffer() },
            title = { Text("Pronađen backup u oblaku") },
            text = {
                Text(
                    "Na Google Drive-u postoji backup vaših podataka. " +
                        "Želite li da ga vratite na ovaj uređaj?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    driveBackup.dismissRestoreOffer()
                    akcije.restore()
                }) { Text("Vrati iz oblaka") }
            },
            dismissButton = {
                TextButton(onClick = { driveBackup.dismissRestoreOffer() }) { Text("Ne sada") }
            }
        )
    }

    CloudAkcijeDijalozi(akcije)
}

/** Podaci spremni za zamenu, cekaju potvrdu korisnika. */
private class RestoreZahtev(
    val stavke: List<Stavka>,
    val opis: String,
    val izOblaka: Boolean
)

/** Backup/restore tok sa svim proverama; deli ga Cloud Backup dijalog i ponuda posle prijave. */
private class CloudAkcije(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
    private val manager: PodaciManager,
    private val driveBackup: GoogleDriveBackup
) {
    var restorePotvrda by mutableStateOf<RestoreZahtev?>(null)
    var prikaziNijePreuzeto by mutableStateOf(false)

    var prikaziVerzije by mutableStateOf(false)
    var verzijeUcitavanje by mutableStateOf(false)
    var verzije by mutableStateOf<List<BackupVersion>?>(null)
    var lokalneKopije by mutableStateOf<List<LokalnaKopija>>(emptyList())

    fun backup(potvrdjenoPrepisivanje: Boolean = false) {
        if (manager.lokalniPodaciOsteceni) {
            poruka("Podaci na telefonu su oštećeni - prvo vratite podatke iz oblaka")
            return
        }
        scope.launch {
            val uspeh = driveBackup.backup({ manager.izvozJson() }, potvrdjenoPrepisivanje)
            val stanje = driveBackup.syncState.value
            if (!uspeh && stanje is SyncState.CloudNotAdopted) {
                // U oblaku je backup koji nije preuzet - ne prepisuj bez pitanja
                prikaziNijePreuzeto = true
            } else {
                poruka(porukaZaBackup(uspeh, stanje))
            }
        }
    }

    fun restore() {
        scope.launch {
            val tekst = driveBackup.restore()
            if (tekst == null) {
                poruka(porukaZaRestoreBezPodataka(driveBackup.syncState.value))
                return@launch
            }
            ponudiZamenu(tekst, "Google Drive backup", izOblaka = true)
        }
    }

    fun otvoriVerzije() {
        prikaziVerzije = true
        verzije = null
        verzijeUcitavanje = true
        lokalneKopije = manager.lokalneKopije()
        scope.launch {
            verzije = driveBackup.listVersions()
            verzijeUcitavanje = false
        }
    }

    fun restoreVerzije(verzija: BackupVersion) {
        scope.launch {
            val tekst = driveBackup.downloadVersion(verzija.id)
            if (tekst == null) {
                poruka("Preuzimanje verzije nije uspelo - ništa nije promenjeno")
                return@launch
            }
            ponudiZamenu(tekst, "Verzija od ${verzija.timestampMillis.formatirajVreme()}", izOblaka = true)
        }
    }

    fun restoreLokalne(kopija: LokalnaKopija) {
        manager.parsirajLokalnuKopiju(kopija).fold(
            onSuccess = {
                restorePotvrda = RestoreZahtev(
                    it,
                    "Kopija sa telefona od ${kopija.vreme.formatirajVreme()}",
                    izOblaka = false
                )
            },
            onFailure = { poruka("Kopija je neispravna - ništa nije promenjeno") }
        )
    }

    /** Ceo sadržaj se proverava pre prvog upisa - neispravan backup ne menja ništa. */
    private fun ponudiZamenu(tekst: String, opis: String, izOblaka: Boolean) {
        manager.parsirajUvoz(tekst).fold(
            onSuccess = { stavke ->
                val zahtev = RestoreZahtev(stavke, opis, izOblaka)
                if (manager.stavke.isEmpty()) primeni(zahtev) else restorePotvrda = zahtev
            },
            onFailure = { poruka("$opis je neispravan - ništa nije promenjeno") }
        )
    }

    fun primeni(zahtev: RestoreZahtev) {
        if (manager.zameniSve(zahtev.stavke)) {
            if (zahtev.izOblaka) driveBackup.markCloudAdopted()
            poruka("Podaci vraćeni: ${zahtev.opis}")
        } else {
            poruka("Čuvanje na uređaju nije uspelo - podaci nisu vraćeni")
        }
    }

    private fun poruka(tekst: String) {
        Toast.makeText(context, tekst, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun CloudAkcijeDijalozi(akcije: CloudAkcije) {
    val zaRestore = akcije.restorePotvrda
    if (zaRestore != null) {
        AlertDialog(
            onDismissRequest = { akcije.restorePotvrda = null },
            title = { Text("Vraćanje podataka") },
            text = {
                Text(
                    "${zaRestore.opis} sadrži ${zaRestore.stavke.size} stavki. Zameniti sve trenutne podatke? " +
                        "Trenutni podaci se prethodno čuvaju na telefonu (Prethodne verzije)."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    akcije.restorePotvrda = null
                    akcije.primeni(zaRestore)
                }) { Text("Zameni") }
            },
            dismissButton = {
                TextButton(onClick = { akcije.restorePotvrda = null }) { Text("Otkaži") }
            }
        )
    }

    if (akcije.prikaziVerzije) {
        val verzije = akcije.verzije
        AlertDialog(
            onDismissRequest = { akcije.prikaziVerzije = false },
            title = { Text("Prethodne verzije") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Izaberite verziju podataka koju želite da vratite.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Google Drive",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    when {
                        akcije.verzijeUcitavanje -> Text("Učitavanje...")
                        verzije == null -> Text("Verzije nije moguće učitati (proverite mrežu)")
                        verzije.isEmpty() -> Text("Nema prethodnih verzija")
                        else -> verzije.forEach { verzija ->
                            TextButton(
                                onClick = {
                                    akcije.prikaziVerzije = false
                                    akcije.restoreVerzije(verzija)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(verzija.timestampMillis.formatirajVreme()) }
                        }
                    }
                    if (akcije.lokalneKopije.isNotEmpty()) {
                        Text(
                            "Na telefonu (pre poslednjeg vraćanja)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        akcije.lokalneKopije.forEach { kopija ->
                            TextButton(
                                onClick = {
                                    akcije.prikaziVerzije = false
                                    akcije.restoreLokalne(kopija)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(kopija.vreme.formatirajVreme()) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { akcije.prikaziVerzije = false }) { Text("Zatvori") }
            }
        )
    }

    if (akcije.prikaziNijePreuzeto) {
        AlertDialog(
            onDismissRequest = { akcije.prikaziNijePreuzeto = false },
            title = { Text("U oblaku već postoji backup") },
            text = {
                Text(
                    "Na Google Drive-u postoji backup koji na ovom uređaju još nije vraćen. " +
                        "Ako sada napravite backup, on će biti zamenjen trenutnim podacima sa ovog uređaja."
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            akcije.prikaziNijePreuzeto = false
                            akcije.restore()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Vrati iz oblaka") }
                    TextButton(
                        onClick = {
                            akcije.prikaziNijePreuzeto = false
                            akcije.backup(potvrdjenoPrepisivanje = true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ipak napravi backup") }
                    TextButton(
                        onClick = { akcije.prikaziNijePreuzeto = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Otkaži") }
                }
            }
        )
    }
}

@Composable
private fun StatusPanel(
    signInState: SignInState,
    syncState: SyncState,
    lastSyncTime: Long?,
    autoBackupProblem: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            InfoRow(
                label = "Nalog",
                value = when (signInState) {
                    SignInState.SignedOut -> "Nije prijavljen"
                    is SignInState.SignedIn -> signInState.email
                },
                highlighted = signInState is SignInState.SignedIn
            )
            InfoRow(
                label = "Poslednji backup",
                value = lastSyncTime?.formatirajVreme() ?: "Nema"
            )
            InfoRow(
                label = "Sinhronizacija",
                value = syncState.prikazniTekst(),
                highlighted = syncState is SyncState.Syncing || syncState is SyncState.Success
            )
            if (autoBackupProblem) {
                Text(
                    "Automatski backup ne radi - pokreni Backup ručno.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) BackupGreen else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.25f)
        )
    }
}

private fun SyncState.prikazniTekst(): String =
    when (this) {
        SyncState.Idle -> "Spremno"
        SyncState.Syncing -> "U toku..."
        SyncState.Success -> "Uspešno"
        SyncState.EmptyData -> "Nema podataka"
        SyncState.CloudNotAdopted -> "Čeka potvrdu"
        is SyncState.Error -> "Greška"
    }

private fun porukaZaBackup(uspeh: Boolean, stanje: SyncState): String =
    when {
        uspeh -> "Backup sačuvan na Google Drive"
        stanje is SyncState.EmptyData -> "Nema podataka za Google Drive backup"
        stanje is SyncState.Error -> "Google Drive backup nije uspeo"
        else -> "Google Drive backup nije uspeo"
    }

private fun porukaZaRestoreBezPodataka(stanje: SyncState): String =
    when (stanje) {
        is SyncState.Error -> "Google Drive restore nije uspeo"
        else -> "Nema Google Drive backup-a"
    }

private fun Long.formatirajVreme(): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
