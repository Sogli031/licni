package com.licnipodaci.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabloniTest {

    @Test
    fun `svaki sablon ima jedinstven naziv`() {
        val nazivi = Sabloni.sve.map { it.naziv }
        assertEquals(nazivi.size, nazivi.toSet().size)
    }

    @Test
    fun `bankovna kartica sablon sadrzi PIN`() {
        val kartica = Sabloni.sve.first { it.naziv == "Bankovna kartica" }
        assertTrue(kartica.polja.contains("PIN"))
        assertEquals(Kategorija.FINANSIJE, kartica.kategorija)
    }

    @Test
    fun `prazna stavka nema polja`() {
        val prazna = Sabloni.sve.first { it.naziv == "Prazna stavka" }
        assertTrue(prazna.polja.isEmpty())
    }
}
