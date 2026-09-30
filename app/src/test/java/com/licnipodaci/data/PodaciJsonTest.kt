package com.licnipodaci.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodaciJsonTest {

    private val validId1 = "11111111-1111-1111-1111-111111111111"
    private val validId2 = "22222222-2222-2222-2222-222222222222"

    @Test
    fun `round-trip cuva sve podatke`() {
        val original = listOf(
            Stavka(
                id = validId1,
                kategorija = Kategorija.FINANSIJE,
                naziv = "Visa kartica",
                polja = listOf(
                    Polje("Broj kartice", "1234 5678"),
                    Polje("PIN", "4321")
                )
            ),
            Stavka(
                id = validId2,
                kategorija = Kategorija.LICNI_DOKUMENT,
                naziv = "Lična karta",
                polja = listOf(Polje("JMBG", "0101990800000"))
            )
        )
        val json = PodaciJson.uJson(original)
        val vraceno = PodaciJson.izJson(json).getOrThrow()
        assertEquals(original, vraceno)
    }

    @Test
    fun `izvoz - uvoz - ponovni izvoz daje identican json i cuva redosled`() {
        val original = listOf(
            Stavka(
                id = validId2,
                kategorija = Kategorija.SIFRE,
                naziv = "Šifra sa čćžšđ",
                polja = listOf(
                    Polje("Lozinka", "a\"b\\c\n<>&"),
                    Polje("E-mail", ""),
                    Polje("Lozinka", "duplirana oznaka")
                )
            ),
            Stavka(id = validId1, kategorija = Kategorija.VOZILO, naziv = "", polja = emptyList())
        )

        val prviIzvoz = PodaciJson.uJson(original)
        val uvezeno = PodaciJson.izJson(prviIzvoz).getOrThrow()
        val drugiIzvoz = PodaciJson.uJson(uvezeno)

        assertEquals(original, uvezeno)
        assertEquals(prviIzvoz, drugiIzvoz)
    }

    @Test
    fun `jedna neispravna stavka obara ceo uvoz`() {
        val json =
            """[
                {"id":"$validId1","kategorija":"FINANSIJE","naziv":"Ispravna","polja":[]},
                {"id":"$validId2","kategorija":"FINANSIJE","polja":[]}
            ]"""

        assertTrue(PodaciJson.izJson(json).isFailure)
    }

    @Test
    fun `neispravan json vraca neuspeh`() {
        val rezultat = PodaciJson.izJson("ovo nije json {{{")
        assertTrue(rezultat.isFailure)
    }

    @Test
    fun `prazan niz se parsira u praznu listu`() {
        val rezultat = PodaciJson.izJson("[]")
        assertEquals(emptyList<Stavka>(), rezultat.getOrThrow())
    }

    @Test
    fun `nedostajuca obavezna struktura vraca neuspeh`() {
        val neispravniJsonovi = listOf(
            """[{"id":"$validId1","kategorija":"FINANSIJE","naziv":"Visa kartica"}]""",
            """[{"id":"$validId1","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[{"vrednost":"1234"}]}]""",
            """[{"id":"$validId1","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[{"oznaka":null,"vrednost":"1234"}]}]""",
            """[{"id":"$validId1","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[{"oznaka":"Broj kartice"}]}]""",
            """[{"id":"$validId1","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[{"oznaka":"Broj kartice","vrednost":null}]}]""",
            """[{"id":"$validId1","kategorija":"NEPOZNATO","naziv":"Visa kartica","polja":[]}]"""
        )

        neispravniJsonovi.forEach { json ->
            assertTrue("Ocekivan neuspeh za: $json", PodaciJson.izJson(json).isFailure)
        }
    }

    @Test
    fun `pogresni primitivni tipovi vracaju neuspeh`() {
        val neispravniJsonovi = listOf(
            """[{"id":123,"kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]""",
            """[{"id":"$validId1","kategorija":"FINANSIJE","naziv":true,"polja":[]}]"""
        )

        neispravniJsonovi.forEach { json ->
            assertTrue("Ocekivan neuspeh za: $json", PodaciJson.izJson(json).isFailure)
        }
    }

    @Test
    fun `import odbija prazan id`() {
        val neispravniJsonovi = listOf(
            """[{"id":"","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]""",
            """[{"id":"   ","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]"""
        )

        neispravniJsonovi.forEach { json ->
            assertTrue("Ocekivan neuspeh za: $json", PodaciJson.izJson(json).isFailure)
        }
    }

    @Test
    fun `import odbija duplirane id vrednosti`() {
        val json =
            """[
                {"id":"$validId1","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]},
                {"id":"$validId1","kategorija":"OSTALO","naziv":"Druga stavka","polja":[]}
            ]"""

        assertTrue(PodaciJson.izJson(json).isFailure)
    }

    @Test
    fun `import odbija id koji nije uuid`() {
        val json = """[{"id":"nije-uuid","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]"""

        assertTrue(PodaciJson.izJson(json).isFailure)
    }

    @Test
    fun `import odbija id koji lomi rutu`() {
        val neispravniJsonovi = listOf(
            """[{"id":"$validId1/extra","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]""",
            """[{"id":"$validId1?edit=true","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]""",
            """[{"id":"$validId1&delete=true","kategorija":"FINANSIJE","naziv":"Visa kartica","polja":[]}]"""
        )

        neispravniJsonovi.forEach { json ->
            assertTrue("Ocekivan neuspeh za: $json", PodaciJson.izJson(json).isFailure)
        }
    }
}
