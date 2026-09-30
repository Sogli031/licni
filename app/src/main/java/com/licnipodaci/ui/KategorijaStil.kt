package com.licnipodaci.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.licnipodaci.data.Kategorija
import com.licnipodaci.ui.theme.FinansijeBoja
import com.licnipodaci.ui.theme.LicniDokumentBoja
import com.licnipodaci.ui.theme.OstaloBoja
import com.licnipodaci.ui.theme.SifreBoja
import com.licnipodaci.ui.theme.VoziloBoja

data class StilKategorije(
    val boja: Color,
    val ikona: ImageVector
)

fun Kategorija.stil(): StilKategorije = when (this) {
    Kategorija.LICNI_DOKUMENT -> StilKategorije(LicniDokumentBoja, Icons.Outlined.Badge)
    Kategorija.VOZILO -> StilKategorije(VoziloBoja, Icons.Outlined.DirectionsCar)
    Kategorija.FINANSIJE -> StilKategorije(FinansijeBoja, Icons.Outlined.AccountBalanceWallet)
    Kategorija.SIFRE -> StilKategorije(SifreBoja, Icons.Outlined.Key)
    Kategorija.OSTALO -> StilKategorije(OstaloBoja, Icons.Outlined.Inventory2)
}
