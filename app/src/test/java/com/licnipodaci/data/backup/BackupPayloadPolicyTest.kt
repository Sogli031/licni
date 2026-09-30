package com.licnipodaci.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPayloadPolicyTest {

    @Test
    fun `prazan payload se ne salje kao backup`() {
        assertTrue(BackupPayloadPolicy.shouldSkipBackup(""))
        assertTrue(BackupPayloadPolicy.shouldSkipBackup("[]"))
        assertTrue(BackupPayloadPolicy.shouldSkipBackup("   []   "))
    }

    @Test
    fun `payload sa stavkama se salje kao backup`() {
        val json = """[{"id":"11111111-1111-1111-1111-111111111111"}]"""

        assertFalse(BackupPayloadPolicy.shouldSkipBackup(json))
    }

    @Test
    fun `ime prethodne kopije sadrzi vreme i moze da se procita nazad`() {
        val ime = BackupPayloadPolicy.prevCopyName(glavniFajl, 1700000000000L)

        assertEquals("licni_podaci_backup_prev_1700000000000.json", ime)
        assertEquals(1700000000000L, BackupPayloadPolicy.prevCopyTimestamp(glavniFajl, ime))
    }

    @Test
    fun `glavni fajl i tudji fajlovi nisu prethodne kopije`() {
        assertNull(BackupPayloadPolicy.prevCopyTimestamp(glavniFajl, glavniFajl))
        assertNull(BackupPayloadPolicy.prevCopyTimestamp(glavniFajl, "licni_podaci_backup_prev_abc.json"))
        assertNull(BackupPayloadPolicy.prevCopyTimestamp(glavniFajl, "nesto_drugo_prev_1700000000000.json"))
    }

    @Test
    fun `cuva se pet najnovijih kopija a glavni fajl se nikad ne brise`() {
        val fajlovi = (1..8).map { "id$it" to BackupPayloadPolicy.prevCopyName(glavniFajl, it * 1000L) } +
            ("glavni" to glavniFajl)

        val zaBrisanje = BackupPayloadPolicy.prevCopiesToDelete(glavniFajl, fajlovi.shuffled(), nowMillis = 10_000L)

        assertEquals(setOf("id1", "id2", "id3"), zaBrisanje.toSet())
    }

    @Test
    fun `pet ili manje kopija - nista se ne brise`() {
        val fajlovi = (1..5).map { "id$it" to BackupPayloadPolicy.prevCopyName(glavniFajl, it * 1000L) }

        assertTrue(BackupPayloadPolicy.prevCopiesToDelete(glavniFajl, fajlovi, nowMillis = 10_000L).isEmpty())
    }

    @Test
    fun `po jedna najnovija kopija za svaki dan u poslednjih 30 dana se cuva`() {
        val dan = 86_400_000L
        val sada = 100 * dan
        // 3 kopije pre 10 dana (isti dan), 1 kopija pre 20 dana, 1 kopija pre 40 dana
        val stare = listOf(
            "a1" to sada - 10 * dan + 1_000L,
            "a2" to sada - 10 * dan + 2_000L,
            "a3" to sada - 10 * dan + 3_000L,
            "b" to sada - 20 * dan,
            "c" to sada - 40 * dan
        )
        // 5 sasvim svežih kopija popunjava "najnovijih pet"
        val sveze = (1..5).map { "n$it" to sada - it * 1_000L }
        val fajlovi = (stare + sveze).map { (id, vreme) -> id to BackupPayloadPolicy.prevCopyName(glavniFajl, vreme) }

        val zaBrisanje = BackupPayloadPolicy.prevCopiesToDelete(glavniFajl, fajlovi.shuffled(), sada)

        // a3 je najnovija tog dana, b je jedina tog dana; ostalo (a1, a2, c) ide
        assertEquals(setOf("a1", "a2", "c"), zaBrisanje.toSet())
    }

    @Test
    fun `kopije sa istim vremenom su duplikati - ostaje jedna`() {
        val fajlovi = listOf(
            "x" to BackupPayloadPolicy.prevCopyName(glavniFajl, 5_000L),
            "y" to BackupPayloadPolicy.prevCopyName(glavniFajl, 5_000L)
        )

        assertEquals(1, BackupPayloadPolicy.prevCopiesToDelete(glavniFajl, fajlovi, nowMillis = 6_000L).size)
    }

    @Test
    fun `md5 se poklapa sa poznatom vrednoscu`() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", BackupPayloadPolicy.md5Hex("abc".toByteArray()))
    }

    private val glavniFajl = "licni_podaci_backup.json"
}
