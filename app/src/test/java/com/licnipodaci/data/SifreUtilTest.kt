package com.licnipodaci.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SifreUtilTest {

    @Test
    fun `vrednost sa @ postaje E-mail`() {
        assertEquals(SifreUtil.OZNAKA_EMAIL, SifreUtil.oznakaZaVrednost("sogli@gmail.com"))
    }

    @Test
    fun `vrednost bez @ postaje Korisnicko ime`() {
        assertEquals(SifreUtil.OZNAKA_USERNAME, SifreUtil.oznakaZaVrednost("sogli"))
    }

    @Test
    fun `prazna vrednost vraca neutralnu oznaku`() {
        assertEquals(SifreUtil.OZNAKA_NEUTRALNA, SifreUtil.oznakaZaVrednost("   "))
    }

    @Test
    fun `samo identitetske oznake se automatski menjaju`() {
        assertTrue(SifreUtil.jeIdentitetPolje("E-mail"))
        assertTrue(SifreUtil.jeIdentitetPolje("Korisničko ime"))
        assertTrue(SifreUtil.jeIdentitetPolje(SifreUtil.OZNAKA_NEUTRALNA))
        assertFalse(SifreUtil.jeIdentitetPolje("Lozinka"))
    }

    @Test
    fun `sifre sabloni postoje u kategoriji SIFRE`() {
        val sifre = Sabloni.sve.filter { it.kategorija == Kategorija.SIFRE }
        assertTrue(sifre.any { it.naziv == "Google" })
        assertTrue(sifre.any { it.naziv == "Instagram" })
        assertTrue(sifre.any { it.naziv == SifreUtil.CUSTOM_SABLON })
    }
}
