package com.licnipodaci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.licnipodaci.data.DatumUtil
import com.licnipodaci.data.Kategorija
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.Polje
import com.licnipodaci.data.Sabloni
import com.licnipodaci.data.Sekcija
import com.licnipodaci.data.sekcija
import com.licnipodaci.data.SifreUtil
import com.licnipodaci.data.Stavka
import com.licnipodaci.ui.stil
import java.util.UUID

@Composable
fun UrediScreen(
    id: String?,
    sekcija: Sekcija,
    manager: PodaciManager,
    navController: NavHostController,
    unapredNaziv: String? = null
) {
    val postojeca: Stavka? = remember(id) { id?.let { manager.pronadji(it) } }
    val izmena = postojeca != null
    val aktivnaSekcija = postojeca?.kategorija?.sekcija ?: sekcija
    val sabloniSekcije = remember(aktivnaSekcija) { Sabloni.sve.filter { it.kategorija.sekcija == aktivnaSekcija } }

    // Ako smo došli iz grupe (npr. "Google"), odmah otvori formu sa tim šablonom.
    val unapredSablon = remember(unapredNaziv, izmena) {
        if (izmena || unapredNaziv.isNullOrBlank()) null
        else Sabloni.sve.firstOrNull { it.kategorija.sekcija == sekcija && it.naziv.equals(unapredNaziv, ignoreCase = true) }
            ?: Sabloni.sve.firstOrNull { it.naziv == SifreUtil.CUSTOM_SABLON }
    }

    var izabranaKategorija by remember { mutableStateOf(postojeca?.kategorija ?: unapredSablon?.kategorija) }
    var naziv by remember { mutableStateOf(postojeca?.naziv ?: unapredNaziv?.trim() ?: "") }
    val polja = remember {
        mutableStateListOf<Polje>().apply {
            postojeca?.let { addAll(it.polja) }
                ?: unapredSablon?.let { s -> addAll(s.polja.map { Polje(it, "") }) }
        }
    }
    var sablonIzabran by remember { mutableStateOf(izmena || unapredSablon != null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when {
                            izmena -> if (aktivnaSekcija == Sekcija.SIFRE) "Izmena šifre" else "Izmena stavke"
                            sablonIzabran -> if (aktivnaSekcija == Sekcija.SIFRE) "Nova šifra" else "Nova stavka"
                            else -> if (aktivnaSekcija == Sekcija.SIFRE) "Za šta je šifra?" else "Izaberi šablon"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (sablonIzabran && !izmena && unapredSablon == null) sablonIzabran = false
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (!sablonIzabran) {
            IzborSablona(
                padding = padding,
                sabloni = sabloniSekcije,
                sekcija = aktivnaSekcija,
                onIzbor = { sablon ->
                    izabranaKategorija = sablon.kategorija
                    naziv = if (sablon.naziv == SifreUtil.CUSTOM_SABLON) "" else sablon.naziv
                    polja.clear()
                    polja.addAll(sablon.polja.map { Polje(it, "") })
                    sablonIzabran = true
                }
            )
        } else {
            FormaStavke(
                padding = padding,
                naziv = naziv,
                onNazivPromena = { naziv = it },
                kategorija = izabranaKategorija ?: Kategorija.OSTALO,
                polja = polja,
                onSacuvaj = {
                    val kat = izabranaKategorija ?: Kategorija.OSTALO
                    val ociscena = polja
                        .map { it.copy(oznaka = it.oznaka.trim(), vrednost = it.vrednost.trim()) }
                        .filter { it.oznaka.isNotEmpty() || it.vrednost.isNotEmpty() }
                    val stavka = Stavka(
                        id = postojeca?.id ?: UUID.randomUUID().toString(),
                        kategorija = kat,
                        naziv = naziv.trim().ifBlank { "Bez naziva" },
                        polja = ociscena
                    )
                    if (izmena) manager.izmeni(stavka) else manager.dodaj(stavka)
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun IzborSablona(
    padding: PaddingValues,
    sabloni: List<com.licnipodaci.data.Sablon>,
    sekcija: Sekcija,
    onIzbor: (com.licnipodaci.data.Sablon) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Text(
                    if (sekcija == Sekcija.SIFRE) "Izaberi servis ili upiši svoj" else "Odaberi tip stavke koji najbolje opisuje šta čuvaš",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        items(sabloni) { sablon ->
            SablonKartica(sablon = sablon, onKlik = { onIzbor(sablon) })
        }
    }
}

@Composable
private fun SablonKartica(sablon: com.licnipodaci.data.Sablon, onKlik: () -> Unit) {
    val stil = sablon.kategorija.stil()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, stil.boja.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onKlik
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(stil.boja.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(stil.ikona, contentDescription = null, tint = stil.boja, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sablon.naziv,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    sablon.kategorija.naziv,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sablon.polja.isNotEmpty()) {
                Text(
                    "${sablon.polja.size} polja",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FormaStavke(
    padding: PaddingValues,
    naziv: String,
    onNazivPromena: (String) -> Unit,
    kategorija: Kategorija,
    polja: androidx.compose.runtime.snapshots.SnapshotStateList<Polje>,
    onSacuvaj: () -> Unit
) {
    val stil = kategorija.stil()
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, stil.boja.copy(alpha = 0.5f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(stil.boja.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(stil.ikona, contentDescription = null, tint = stil.boja, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        kategorija.naziv,
                        style = MaterialTheme.typography.titleSmall,
                        color = stil.boja,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Polja: ${polja.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ModerantPolje(
            vrednost = naziv,
            onPromena = onNazivPromena,
            labela = if (kategorija == Kategorija.SIFRE) "Za šta je šifra (npr. Google, Instagram)" else "Naziv stavke"
        )

        polja.forEachIndexed { indeks, polje ->
            PoljeKartica(
                polje = polje,
                kategorija = kategorija,
                onPromena = { polja[indeks] = it },
                onObrisi = { polja.removeAt(indeks) }
            )
        }

        DugmeDodajPolje(onKlik = { polja.add(Polje("", "")) })

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSacuvaj,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Outlined.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sačuvaj", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PoljeKartica(
    polje: Polje,
    kategorija: Kategorija,
    onPromena: (Polje) -> Unit,
    onObrisi: () -> Unit
) {
    val isteciPolje = DatumUtil.jeIstek(polje.oznaka)
    val identitetPolje = kategorija == Kategorija.SIFRE && SifreUtil.jeIdentitetPolje(polje.oznaka)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModerantPolje(
                    vrednost = polje.oznaka,
                    onPromena = { onPromena(polje.copy(oznaka = it)) },
                    labela = "Naziv polja",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onObrisi) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Obriši polje",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (isteciPolje) {
                DatumskoPolje(
                    vrednost = polje.vrednost,
                    onPromena = { onPromena(polje.copy(vrednost = it)) }
                )
            } else if (identitetPolje) {
                ModerantPolje(
                    vrednost = polje.vrednost,
                    onPromena = {
                        onPromena(polje.copy(oznaka = SifreUtil.oznakaZaVrednost(it), vrednost = it))
                    },
                    labela = "E-mail ili korisničko ime"
                )
                Text(
                    when (polje.oznaka.trim()) {
                        SifreUtil.OZNAKA_EMAIL -> "Prepoznato: e-mail"
                        SifreUtil.OZNAKA_USERNAME -> "Prepoznato: korisničko ime"
                        else -> "Ukucaj e-mail (sa @) ili korisničko ime — oznaka se sama menja"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                ModerantPolje(
                    vrednost = polje.vrednost,
                    onPromena = { onPromena(polje.copy(vrednost = it)) },
                    labela = "Vrednost"
                )
            }
        }
    }
}

@Composable
private fun DatumskoPolje(vrednost: String, onPromena: (String) -> Unit) {
    var prikaziPicker by remember { mutableStateOf(false) }
    val parsiran = DatumUtil.parsirajIstek(vrednost)
    val dani = parsiran?.let { DatumUtil.danaDoIsteka(it) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModerantPolje(
                vrednost = vrednost,
                onPromena = onPromena,
                labela = "Datum (dd.MM.yyyy)",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { prikaziPicker = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = "Izaberi datum",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (dani != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    dani < 0 -> "Isteklo pre ${-dani} ${DatumUtil.recDan(dani)} — obaveštenja neće biti zakazana"
                    dani == 0L -> "Ističe danas"
                    else -> "Obaveštenja: 30, 15, 10, 5 i 1 dan pre isteka (za $dani ${DatumUtil.recDan(dani)})"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (dani < 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    if (prikaziPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = parsiran?.time
        )
        DatePickerDialog(
            onDismissRequest = { prikaziPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val d = DatumUtil.fromMillisUtc(millis)
                        onPromena(DatumUtil.formatiraj(d))
                    }
                    prikaziPicker = false
                }) { Text("U redu") }
            },
            dismissButton = {
                TextButton(onClick = { prikaziPicker = false }) { Text("Otkaži") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun ModerantPolje(
    vrednost: String,
    onPromena: (String) -> Unit,
    labela: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = vrednost,
        onValueChange = onPromena,
        label = { Text(labela) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun DugmeDodajPolje(onKlik: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onKlik
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Dodaj polje",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
