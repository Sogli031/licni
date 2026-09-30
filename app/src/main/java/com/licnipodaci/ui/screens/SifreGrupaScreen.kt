package com.licnipodaci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.Sekcija
import com.licnipodaci.data.SifreUtil
import com.licnipodaci.data.Stavka
import com.licnipodaci.data.sekcija
import com.licnipodaci.ui.navigation.Ruta
import com.licnipodaci.ui.theme.SifreBoja

/** Sve šifre za jedan servis (npr. sve Google šifre). */
@Composable
fun SifreGrupaScreen(
    naziv: String,
    manager: PodaciManager,
    navController: NavHostController
) {
    val kljuc = SifreUtil.kljucGrupe(naziv)
    val stavke = manager.stavke.filter {
        it.kategorija.sekcija == Sekcija.SIFRE && SifreUtil.kljucGrupe(it.naziv) == kljuc
    }
    val prikazNaziv = stavke.firstOrNull()?.naziv?.trim() ?: naziv

    // Ako je poslednja šifra u grupi obrisana, vrati se na listu.
    if (stavke.isEmpty()) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        prikazNaziv,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Ruta.uredi(Sekcija.SIFRE, naziv = prikazNaziv)) },
                containerColor = SifreBoja,
                contentColor = Color.Black
            ) {
                Text("Nova $prikazNaziv šifra", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, SifreBoja.copy(alpha = 0.35f))
                ) {
                    Text(
                        "${stavke.size} ${if (stavke.size == 1) "nalog" else "naloga"} za $prikazNaziv",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(stavke, key = { it.id }) { stavka ->
                SifraNalogKartica(
                    stavka = stavka,
                    onKlik = { navController.navigate(Ruta.detalji(stavka.id)) }
                )
            }
        }
    }
}

/** Kartica jedne šifre unutar grupe: prikazuje e-mail ili korisničko ime. */
@Composable
private fun SifraNalogKartica(stavka: Stavka, onKlik: () -> Unit) {
    val identitet = SifreUtil.identitet(stavka)
    val jeEmail = identitet?.contains('@') == true
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SifreBoja.copy(alpha = 0.25f)),
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
                    .background(SifreBoja.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        identitet == null -> Icons.Outlined.Key
                        jeEmail -> Icons.Outlined.AlternateEmail
                        else -> Icons.Outlined.Person
                    },
                    contentDescription = null,
                    tint = SifreBoja,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    identitet ?: "(bez e-maila / korisničkog imena)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        identitet == null -> "Samo lozinka"
                        jeEmail -> "E-mail"
                        else -> "Korisničko ime"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Kartica grupe u listi Šifre: jedan servis, broj naloga. Koristi je ListaScreen. */
@Composable
internal fun SifraGrupaKartica(naziv: String, stavke: List<Stavka>, onKlik: () -> Unit) {
    val identiteti = stavke.mapNotNull { SifreUtil.identitet(it) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SifreBoja.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onKlik
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SifreBoja.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = SifreBoja, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    naziv,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                identiteti.forEach { identitet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (identitet.contains('@')) Icons.Outlined.AlternateEmail else Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            identitet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = SifreBoja.copy(alpha = 0.15f)
            ) {
                Text(
                    "${stavke.size} ${if (stavke.size == 1) "nalog" else "naloga"}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = SifreBoja,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
