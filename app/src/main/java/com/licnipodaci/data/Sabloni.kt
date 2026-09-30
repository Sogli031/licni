package com.licnipodaci.data

data class Sablon(
    val naziv: String,
    val kategorija: Kategorija,
    val polja: List<String>
)

object Sabloni {
    val sve: List<Sablon> = listOf(
        Sablon("Lična karta", Kategorija.LICNI_DOKUMENT, listOf("Broj lične karte", "JMBG", "Datum izdavanja", "Važi do", "Izdato od", "Mesto")),
        Sablon("Pasoš", Kategorija.LICNI_DOKUMENT, listOf("Broj pasoša", "Datum izdavanja", "Važi do", "Izdato od")),
        Sablon("Vozačka dozvola", Kategorija.LICNI_DOKUMENT, listOf("Broj vozačke", "Kategorije", "Datum izdavanja", "Važi do", "Izdato od")),
        Sablon("Saobraćajna / registracija", Kategorija.VOZILO, listOf("Registarske tablice", "Marka i model", "Broj šasije (VIN)", "Godina proizvodnje", "Datum isteka registracije")),
        Sablon("Osiguranje vozila", Kategorija.VOZILO, listOf("Osiguravajuća kuća", "Broj polise", "Datum isteka")),
        Sablon("Bankovna kartica", Kategorija.FINANSIJE, listOf("Banka", "Tip kartice", "Broj kartice", "Važi do", "CVV", "PIN", "Ime na kartici")),
        Sablon("Bankovni račun", Kategorija.FINANSIJE, listOf("Banka", "Broj računa / IBAN", "Naziv računa")),
        Sablon("Google", Kategorija.SIFRE, SifreUtil.standardnaPolja),
        Sablon("Instagram", Kategorija.SIFRE, SifreUtil.standardnaPolja),
        Sablon("Facebook", Kategorija.SIFRE, SifreUtil.standardnaPolja),
        Sablon("TikTok", Kategorija.SIFRE, SifreUtil.standardnaPolja),
        Sablon("Wi-Fi", Kategorija.SIFRE, listOf("Naziv mreže (SSID)", "Lozinka")),
        Sablon(SifreUtil.CUSTOM_SABLON, Kategorija.SIFRE, SifreUtil.standardnaPolja),
        Sablon("Zdravstveno osiguranje", Kategorija.OSTALO, listOf("Broj zdravstvene knjižice/kartice", "Filijala")),
        Sablon("Polisa osiguranja", Kategorija.OSTALO, listOf("Tip osiguranja", "Osiguravajuća kuća", "Broj polise", "Datum isteka")),
        Sablon("Medicinski / hitni podaci", Kategorija.OSTALO, listOf("Krvna grupa", "Alergije", "Hronične bolesti", "Hitni kontakt")),
        Sablon("Prazna stavka", Kategorija.OSTALO, emptyList())
    )
}
