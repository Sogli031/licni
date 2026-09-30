package com.licnipodaci.ui.navigation

import android.net.Uri
import com.licnipodaci.data.Sekcija

object Ruta {
    const val POCETNA = "pocetna"
    const val LISTA = "lista"
    const val DETALJI = "detalji"
    const val UREDI = "uredi"
    const val GRUPA = "grupa"

    const val LISTA_OBRAZAC = "$LISTA/{sekcija}"
    const val UREDI_OBRAZAC = "$UREDI?id={id}&sekcija={sekcija}&naziv={naziv}"
    const val GRUPA_OBRAZAC = "$GRUPA/{naziv}"

    fun lista(sekcija: Sekcija): String = "$LISTA/${sekcija.name}"
    fun detalji(id: String): String = "$DETALJI/$id"
    fun uredi(sekcija: Sekcija, id: String? = null, naziv: String? = null): String {
        val osnova = if (id == null) "$UREDI?sekcija=${sekcija.name}" else "$UREDI?id=$id&sekcija=${sekcija.name}"
        return if (naziv.isNullOrBlank()) osnova else "$osnova&naziv=${Uri.encode(naziv)}"
    }

    fun grupa(naziv: String): String = "$GRUPA/${Uri.encode(naziv)}"

    fun sekcijaIz(naziv: String?): Sekcija =
        Sekcija.entries.firstOrNull { it.name == naziv } ?: Sekcija.PODACI
}
