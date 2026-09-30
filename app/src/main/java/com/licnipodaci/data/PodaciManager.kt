package com.licnipodaci.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.licnipodaci.data.backup.BackupScheduler
import com.licnipodaci.notif.ObavestenjeManager
import java.io.File
import java.io.FileOutputStream

/** Lokalna sigurnosna kopija podataka sačuvana neposredno pre zamene (vraćanja). */
data class LokalnaKopija(val fajl: File, val vreme: Long)

class PodaciManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val fajl = File(appContext.filesDir, "licni_podaci.json")
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val stavke: SnapshotStateList<Stavka> = mutableStateListOf()

    /**
     * Lokalni fajl sa podacima se nije mogao pročitati pa je sklonjen sa strane (nije obrisan).
     * Dok je ovo true, backup se ne šalje u oblak, da prazno/nepotpuno stanje ne pregazi dobar
     * backup. Gasi se uspešnim vraćanjem podataka ili izričitim izborom korisnika.
     */
    var lokalniPodaciOsteceni by mutableStateOf(prefs.getBoolean(KEY_OSTECENI, false))
        private set

    init {
        ucitaj()
        ObavestenjeManager.rasporediSve(appContext, stavke.toList())
    }

    private fun ucitaj() {
        if (!fajl.exists()) return
        val ucitane = runCatching {
            PodaciJson.izJson(fajl.readText(Charsets.UTF_8)).getOrThrow()
        }.getOrNull()
        if (ucitane == null) {
            sklobiOstecenFajl()
            return
        }
        stavke.clear()
        stavke.addAll(ucitane)
    }

    /** Oštećen fajl se ne prepisuje novim podacima - prvo se sačuva pod drugim imenom. */
    private fun sklobiOstecenFajl() {
        val cilj = File(fajl.parentFile, "$OSTECEN_PREFIX${System.currentTimeMillis()}.json")
        fajl.renameTo(cilj) || runCatching { fajl.copyTo(cilj, overwrite = true) }.isSuccess
        // Čak i ako sklanjanje nije uspelo, backup ostaje blokiran dok korisnik ne odluči
        postaviOsteceni(true)
    }

    private fun postaviOsteceni(vrednost: Boolean) {
        lokalniPodaciOsteceni = vrednost
        prefs.edit().putBoolean(KEY_OSTECENI, vrednost).commit()
    }

    /** Korisnik bira da nastavi sa praznim podacima umesto da vrati backup. */
    fun nastaviBezVracanja() {
        postaviOsteceni(false)
    }

    private fun sacuvaj(): Boolean {
        val sacuvano = upisiAtomski(fajl, PodaciJson.uJson(stavke.toList()))
        // Svaka snimljena izmena zakazuje i backup u oblak. Vraćanje iz oblaka
        // (zameniSve) namerno ne prolazi ovuda.
        if (sacuvano && !lokalniPodaciOsteceni) BackupScheduler.scheduleAfterChange(appContext)
        return sacuvano
    }

    fun pronadji(id: String): Stavka? = stavke.firstOrNull { it.id == id }

    fun dodaj(stavka: Stavka) {
        stavke.add(stavka)
        sacuvaj()
        ObavestenjeManager.rasporedi(appContext, stavka)
    }

    fun izmeni(stavka: Stavka) {
        val indeks = stavke.indexOfFirst { it.id == stavka.id }
        if (indeks >= 0) {
            stavke[indeks] = stavka
            sacuvaj()
            ObavestenjeManager.rasporedi(appContext, stavka)
        }
    }

    fun obrisi(id: String) {
        if (stavke.removeAll { it.id == id }) {
            sacuvaj()
            ObavestenjeManager.otkazi(appContext, id)
        }
    }

    fun izvozJson(): String = PodaciJson.uJson(stavke.toList())

    fun parsirajUvoz(json: String): Result<List<Stavka>> = PodaciJson.izJson(json)

    /**
     * Zamenjuje sve podatke novom listom. Prvo se upisuje fajl (atomski), pa tek onda
     * menja stanje u memoriji - ako upis ne uspe, stari podaci ostaju netaknuti.
     * Vraća true samo ako su novi podaci zaista sačuvani na uređaju.
     */
    fun zameniSve(nove: List<Stavka>): Boolean {
        // Trenutni podaci se prvo čuvaju sa strane; ako to ne uspe, ništa se ne menja
        if (!sacuvajLokalnuKopiju()) return false
        if (!upisiAtomski(fajl, PodaciJson.uJson(nove))) return false
        postaviOsteceni(false)
        val stareIds = stavke.map { it.id }.toList()
        stavke.clear()
        stavke.addAll(nove)
        stareIds.forEach { ObavestenjeManager.otkazi(appContext, it) }
        ObavestenjeManager.rasporediSve(appContext, nove)
        return true
    }

    /** Kopije podataka pre poslednjih vraćanja, najnovija prva. */
    fun lokalneKopije(): List<LokalnaKopija> =
        appContext.filesDir.listFiles().orEmpty()
            .mapNotNull { f ->
                val ime = f.name
                if (!ime.startsWith(KOPIJA_PREFIX) || !ime.endsWith(".json")) return@mapNotNull null
                ime.removePrefix(KOPIJA_PREFIX).removeSuffix(".json").toLongOrNull()
                    ?.let { LokalnaKopija(f, it) }
            }
            .sortedByDescending { it.vreme }

    fun parsirajLokalnuKopiju(kopija: LokalnaKopija): Result<List<Stavka>> =
        runCatching { kopija.fajl.readText(Charsets.UTF_8) }.fold(
            onSuccess = { PodaciJson.izJson(it) },
            onFailure = { Result.failure(it) }
        )

    /** true ako je kopija sačuvana ili nema šta da se čuva. */
    private fun sacuvajLokalnuKopiju(): Boolean {
        if (stavke.isEmpty() || !fajl.exists()) return true
        val cilj = File(fajl.parentFile, "$KOPIJA_PREFIX${System.currentTimeMillis()}.json")
        if (runCatching { fajl.copyTo(cilj, overwrite = true) }.isFailure) return false
        lokalneKopije().drop(MAX_LOKALNIH_KOPIJA).forEach { it.fajl.delete() }
        return true
    }

    companion object {
        private const val PREFS_NAME = "podaci_prefs"
        private const val KEY_OSTECENI = "lokalni_podaci_osteceni"
        private const val KOPIJA_PREFIX = "licni_podaci.pre_vracanja_"
        private const val OSTECEN_PREFIX = "licni_podaci.ostecen_"
        private const val MAX_LOKALNIH_KOPIJA = 3

        /** Upis preko privremenog fajla + rename, da prekid usred upisa ne ošteti postojeće podatke. */
        internal fun upisiAtomski(cilj: File, tekst: String): Boolean {
            val privremeni = File(cilj.parentFile, cilj.name + ".tmp")
            return try {
                FileOutputStream(privremeni).use { izlaz ->
                    izlaz.write(tekst.toByteArray(Charsets.UTF_8))
                    izlaz.flush()
                    izlaz.fd.sync()
                }
                if (privremeni.renameTo(cilj)) {
                    true
                } else {
                    privremeni.delete()
                    false
                }
            } catch (_: Exception) {
                privremeni.delete()
                false
            }
        }
    }
}
