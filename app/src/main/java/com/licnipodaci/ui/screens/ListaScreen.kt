package com.licnipodaci.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.licnipodaci.data.DatumUtil
import com.licnipodaci.data.Kategorija
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.Sekcija
import com.licnipodaci.data.SifreUtil
import com.licnipodaci.data.sekcija
import com.licnipodaci.data.Stavka
import com.licnipodaci.data.backup.GoogleDriveBackup
import com.licnipodaci.ui.navigation.Ruta
import com.licnipodaci.ui.stil
import com.licnipodaci.ui.theme.Danger
import com.licnipodaci.ui.theme.Warning

@Composable
fun ListaScreen(
    sekcija: Sekcija,
    manager: PodaciManager,
    navController: NavHostController,
    driveBackup: GoogleDriveBackup,
    onGoogleSignIn: () -> Unit
) {
    var pretraga by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<Kategorija?>(null) }
    var prikaziBackup by remember { mutableStateOf(false) }
    val upit = pretraga.trim().lowercase()

    val uSekciji = manager.stavke.filter { it.kategorija.sekcija == sekcija }
    val kategorijeSekcije = Kategorija.entries.filter { it.sekcija == sekcija }

    val filtrirane = uSekciji.filter { stavka ->
        (filter == null || stavka.kategorija == filter) &&
            (upit.isEmpty() ||
                stavka.naziv.lowercase().contains(upit) ||
                stavka.polja.any {
                    it.vrednost.lowercase().contains(upit) || it.oznaka.lowercase().contains(upit)
                })
    }

    val istekIscit = uSekciji.flatMap { stavka ->
        stavka.polja
            .filter { DatumUtil.jeIstek(it.oznaka) }
            .mapNotNull { polje -> DatumUtil.parsirajIstek(polje.vrednost)?.let { it } }
            .map { DatumUtil.danaDoIsteka(it) }
    }
    val brojUpozorenja = istekIscit.count { it in 0..30 }
    val brojIsteklih = istekIscit.count { it < 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        sekcija.naziv,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExtendedFloatingActionButton(
                    onClick = { prikaziBackup = true },
                    modifier = Modifier.width(BottomFabWidth),
                    containerColor = BackupGreen,
                    contentColor = Color.Black
                ) {
                    Text(
                        "Backup",
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Ruta.uredi(sekcija)) },
                    modifier = Modifier.width(BottomFabWidth),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(if (sekcija == Sekcija.SIFRE) "Nova šifra" else "Nova stavka", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (sekcija == Sekcija.PODACI) {
                item {
                    PregledKartica(
                        ukupno = uSekciji.size,
                        upozorenja = brojUpozorenja,
                        isteklih = brojIsteklih
                    )
                }
            }

            item {
                PretragaPolje(
                    vrednost = pretraga,
                    onPromena = { pretraga = it }
                )
            }

            if (kategorijeSekcije.size > 1) {
                item {
                    FilterRed(
                        kategorije = kategorijeSekcije,
                        izabrana = filter,
                        onIzbor = { filter = it }
                    )
                }
            }

            if (filtrirane.isEmpty()) {
                item {
                    PrazanPrikaz(
                        praznoStanje = uSekciji.isEmpty()
                    )
                }
            } else if (sekcija == Sekcija.SIFRE) {
                val grupe = filtrirane
                    .groupBy { SifreUtil.kljucGrupe(it.naziv) }
                    .values
                    .sortedBy { SifreUtil.kljucGrupe(it.first().naziv) }
                items(grupe, key = { "grupa_" + SifreUtil.kljucGrupe(it.first().naziv) }) { grupa ->
                    val nazivGrupe = grupa.first().naziv.trim()
                    SifraGrupaKartica(
                        naziv = nazivGrupe,
                        stavke = grupa,
                        onKlik = { navController.navigate(Ruta.grupa(nazivGrupe)) }
                    )
                }
            } else {
                kategorijeSekcije.forEach { kategorija ->
                    val uKategoriji = filtrirane.filter { it.kategorija == kategorija }
                    if (uKategoriji.isNotEmpty()) {
                        if (kategorijeSekcije.size > 1) {
                            item(key = "naslov_${kategorija.name}") {
                                ZaglavljeKategorije(kategorija = kategorija, broj = uKategoriji.size)
                            }
                        }
                        items(uKategoriji, key = { it.id }) { stavka ->
                            StavkaKartica(
                                stavka = stavka,
                                onKlik = { navController.navigate(Ruta.detalji(stavka.id)) }
                            )
                        }
                    }
                }
            }
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

private val BackupGreen = Color(0xFF00C853)
private val BottomFabWidth = 128.dp

@Composable
private fun PregledKartica(ukupno: Int, upozorenja: Int, isteklih: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pregled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$ukupno ${if (ukupno == 1) "stavka" else "stavki"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBedz(
                    tekst = "$upozorenja uskoro",
                    boja = Warning,
                    ikona = Icons.Outlined.Notifications
                )
                StatusBedz(
                    tekst = "$isteklih isteklo",
                    boja = Danger,
                    ikona = Icons.Outlined.WarningAmber
                )
            }
        }
    }
}

@Composable
private fun StatusBedz(tekst: String, boja: Color, ikona: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(50),
        color = boja.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                tekst,
                color = boja,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PretragaPolje(vrednost: String, onPromena: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        TextField(
            value = vrednost,
            onValueChange = onPromena,
            placeholder = { Text("Pretraga po nazivu ili polju") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun FilterRed(kategorije: List<Kategorija>, izabrana: Kategorija?, onIzbor: (Kategorija?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = izabrana == null,
                onClick = { onIzbor(null) },
                label = { Text("Sve") },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        items(kategorije) { k ->
            val stil = k.stil()
            FilterChip(
                selected = izabrana == k,
                onClick = { onIzbor(if (izabrana == k) null else k) },
                label = { Text(k.naziv) },
                leadingIcon = {
                    Icon(stil.ikona, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = stil.boja.copy(alpha = 0.2f),
                    selectedLabelColor = stil.boja,
                    selectedLeadingIconColor = stil.boja
                )
            )
        }
    }
}

@Composable
private fun ZaglavljeKategorije(kategorija: Kategorija, broj: Int) {
    val stil = kategorija.stil()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(stil.boja)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            kategorija.naziv.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = stil.boja,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Text(
            broj.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StavkaKartica(stavka: Stavka, onKlik: () -> Unit) {
    val stil = stavka.kategorija.stil()
    val isteciInfo = stavka.polja
        .filter { DatumUtil.jeIstek(it.oznaka) }
        .mapNotNull { polje ->
            DatumUtil.parsirajIstek(polje.vrednost)?.let { polje.oznaka to it }
        }
    val najblizi = isteciInfo.minByOrNull { it.second }
    val daniDoIsteka = najblizi?.let { DatumUtil.danaDoIsteka(it.second) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, stil.boja.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onKlik
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(stil.boja.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(stil.ikona, contentDescription = null, tint = stil.boja, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                stavka.naziv,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (daniDoIsteka != null) {
                Spacer(Modifier.width(8.dp))
                IstekIndikator(dani = daniDoIsteka)
            }
        }
    }
}

@Composable
private fun IstekIndikator(dani: Long) {
    val (tekst, boja) = when {
        dani < 0 -> "Isteklo" to Danger
        dani == 0L -> "Danas" to Danger
        dani <= 5 -> "$dani d." to Danger
        dani <= 30 -> "$dani d." to Warning
        else -> "$dani d." to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = boja.copy(alpha = 0.15f)
    ) {
        Text(
            tekst,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = boja,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PrazanPrikaz(praznoStanje: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 28.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (praznoStanje) "Još uvek nema sačuvanih podataka" else "Nema rezultata",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (praznoStanje) "Dodaj prvu stavku klikom na dugme dole" else "Promeni pretragu ili filter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
