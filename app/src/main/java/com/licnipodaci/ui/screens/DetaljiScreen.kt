package com.licnipodaci.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.licnipodaci.data.DatumUtil
import com.licnipodaci.data.Polje
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.ui.navigation.Ruta
import com.licnipodaci.data.sekcija
import com.licnipodaci.ui.stil
import com.licnipodaci.ui.theme.Danger
import com.licnipodaci.ui.theme.Warning

@Composable
fun DetaljiScreen(
    id: String,
    manager: PodaciManager,
    navController: NavHostController
) {
    val context = LocalContext.current
    val stavka = manager.stavke.firstOrNull { it.id == id }
    var prikaziBrisanje by remember { mutableStateOf(false) }

    if (stavka == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    val stil = stavka.kategorija.stil()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stavka.naziv,
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
                actions = {
                    IconButton(onClick = { navController.navigate(Ruta.uredi(stavka.kategorija.sekcija, stavka.id)) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Izmeni")
                    }
                    IconButton(onClick = { prikaziBrisanje = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Obriši",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, stil.boja.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(stil.boja.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                stil.ikona,
                                contentDescription = null,
                                tint = stil.boja,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                stavka.kategorija.naziv,
                                style = MaterialTheme.typography.titleSmall,
                                color = stil.boja,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${stavka.polja.size} ${if (stavka.polja.size == 1) "polje" else "polja"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            val upozorenja = stavka.polja
                .filter { DatumUtil.jeIstek(it.oznaka) }
                .mapNotNull { polje ->
                    val datum = DatumUtil.parsirajIstek(polje.vrednost) ?: return@mapNotNull null
                    Triple(polje.oznaka, datum, DatumUtil.danaDoIsteka(datum))
                }
            if (upozorenja.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                }
                upozorenja.forEach { (_, _, dani) ->
                    item {
                        UpozorenjeKartica(dani = dani)
                    }
                }
            }

            if (stavka.polja.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Text(
                            "Nema unetih polja",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                items(stavka.polja.size) { idx ->
                    val polje = stavka.polja[idx]
                    PoljePrikazKartica(polje = polje, context = context)
                }
            }
        }
    }

    if (prikaziBrisanje) {
        AlertDialog(
            onDismissRequest = { prikaziBrisanje = false },
            title = { Text("Brisanje stavke") },
            text = { Text("Obrisati \"${stavka.naziv}\"? Ovu radnju nije moguće poništiti.") },
            confirmButton = {
                TextButton(onClick = {
                    prikaziBrisanje = false
                    manager.obrisi(stavka.id)
                }) { Text("Obriši", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { prikaziBrisanje = false }) { Text("Otkaži") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun UpozorenjeKartica(dani: Long) {
    val (poruka, boja, ikona) = when {
        dani < 0 -> Triple("Isteklo pre ${-dani} ${DatumUtil.recDan(dani)}", Danger, Icons.Outlined.WarningAmber)
        dani == 0L -> Triple("Ističe danas", Danger, Icons.Outlined.WarningAmber)
        dani <= 5 -> Triple("Ističe za $dani ${DatumUtil.recDan(dani)}", Danger, Icons.Outlined.Notifications)
        dani <= 30 -> Triple("Ističe za $dani ${DatumUtil.recDan(dani)}", Warning, Icons.Outlined.Notifications)
        else -> Triple("$dani ${DatumUtil.recDan(dani)} do isteka", Warning, Icons.Outlined.Notifications)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = boja.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, boja.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ikona, contentDescription = null, tint = boja)
            Spacer(Modifier.width(12.dp))
            Text(
                poruka,
                color = boja,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PoljePrikazKartica(polje: Polje, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { kopiraj(context, polje.oznaka, polje.vrednost) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    polje.oznaka.ifBlank { "(bez naziva)" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    polje.vrednost.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = "Kopiraj",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

private fun kopiraj(context: Context, oznaka: String, vrednost: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(oznaka, vrednost))
    Toast.makeText(context, "Kopirano: $oznaka", Toast.LENGTH_SHORT).show()
}
