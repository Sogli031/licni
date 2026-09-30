package com.licnipodaci.data

/**
 * Pomoćne funkcije za kategoriju Šifre: automatsko označavanje polja
 * kao "E-mail" ili "Korisničko ime" na osnovu unete vrednosti.
 */
object SifreUtil {

    /** Naziv šablona u kome korisnik sam upisuje za šta je šifra. */
    const val CUSTOM_SABLON = "Druga šifra (upiši za šta je)"

    const val OZNAKA_NEUTRALNA = "E-mail / Korisničko ime"
    const val OZNAKA_EMAIL = "E-mail"
    const val OZNAKA_USERNAME = "Korisničko ime"

    val standardnaPolja: List<String> = listOf(OZNAKA_NEUTRALNA, "Lozinka")

    private val automatskeOznake = setOf(OZNAKA_NEUTRALNA, OZNAKA_EMAIL, OZNAKA_USERNAME)

    /** Da li se za ovo polje oznaka automatski menja prema unosu. */
    fun jeIdentitetPolje(oznaka: String): Boolean = oznaka.trim() in automatskeOznake

    /** Ključ za grupisanje šifri po servisu (isti naziv = ista grupa). */
    fun kljucGrupe(naziv: String): String = naziv.trim().lowercase()

    /** Vrednost e-mail / korisničkog imena stavke, ako postoji. */
    fun identitet(stavka: Stavka): String? =
        stavka.polja.firstOrNull { jeIdentitetPolje(it.oznaka) && it.vrednost.isNotBlank() }?.vrednost

    /** Vraća oznaku koja odgovara unetoj vrednosti. */
    fun oznakaZaVrednost(vrednost: String): String {
        val v = vrednost.trim()
        return when {
            v.isEmpty() -> OZNAKA_NEUTRALNA
            v.contains('@') -> OZNAKA_EMAIL
            else -> OZNAKA_USERNAME
        }
    }
}
