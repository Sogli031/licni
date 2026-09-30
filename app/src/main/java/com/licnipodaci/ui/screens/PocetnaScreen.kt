package com.licnipodaci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.Sekcija
import com.licnipodaci.data.backup.GoogleDriveBackup
import com.licnipodaci.data.sekcija
import com.licnipodaci.ui.navigation.Ruta
import com.licnipodaci.ui.theme.SifreBoja

/** Početni ekran: izbor između sekcije Podaci i sekcije Šifre. */
@Composable
fun PocetnaScreen(
    manager: PodaciManager,
    navController: NavHostController,
    driveBackup: GoogleDriveBackup,
    onGoogleSignIn: () -> Unit
) {
    var prikaziBackup by remember { mutableStateOf(false) }
    val brojPodataka = manager.stavke.count { it.kategorija.sekcija == Sekcija.PODACI }
    val brojSifri = manager.stavke.count { it.kategorija.sekcija == Sekcija.SIFRE }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lični podaci", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { prikaziBackup = true },
                containerColor = Color(0xFF00C853),
                contentColor = Color.Black
            ) {
                Text("Backup", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Šta želiš da otvoriš?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            SekcijaKartica(
                naslov = "Podaci",
                opis = "Dokumenti, vozila, finansije i ostalo",
                broj = brojPodataka,
                boja = MaterialTheme.colorScheme.primary,
                ikona = Icons.Outlined.Badge,
                onKlik = { navController.navigate(Ruta.lista(Sekcija.PODACI)) }
            )
            SekcijaKartica(
                naslov = "Šifre",
                opis = "Google, Instagram, Wi-Fi i ostali nalozi",
                broj = brojSifri,
                boja = SifreBoja,
                ikona = Icons.Outlined.Key,
                onKlik = { navController.navigate(Ruta.lista(Sekcija.SIFRE)) }
            )
        }
    }

    if (prikaziBackup) {
        CloudBackupDialog(
            manager = manager,
            driveBackup = driveBackup,
            onGoogleSignIn = onGoogleSignIn,
            onDismiss = { prikaziBackup = false }
        )
    }
}

@Composable
private fun SekcijaKartica(
    naslov: String,
    opis: String,
    broj: Int,
    boja: Color,
    ikona: ImageVector,
    onKlik: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, boja.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onKlik
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(boja.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    naslov,
                    style = MaterialTheme.typography.headlineSmall,
                    color = boja,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    opis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$broj ${if (broj == 1) "stavka" else "stavki"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = boja
            )
        }
    }
}
