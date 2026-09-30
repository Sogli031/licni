package com.licnipodaci.data

enum class Kategorija(val naziv: String) {
    LICNI_DOKUMENT("Lični dokumenti"),
    VOZILO("Vozilo"),
    FINANSIJE("Finansije"),
    SIFRE("Šifre"),
    OSTALO("Ostalo")
}

enum class Sekcija(val naziv: String) {
    PODACI("Podaci"),
    SIFRE("Šifre")
}

val Kategorija.sekcija: Sekcija
    get() = if (this == Kategorija.SIFRE) Sekcija.SIFRE else Sekcija.PODACI

data class Polje(
    val oznaka: String,
    val vrednost: String
)

data class Stavka(
    val id: String,
    val kategorija: Kategorija,
    val naziv: String,
    val polja: List<Polje>
)
