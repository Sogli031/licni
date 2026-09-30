package com.licnipodaci.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DatumUtil {

    private val srpski = Locale("sr")

    val PRIKAZNI_FORMAT: SimpleDateFormat
        get() = SimpleDateFormat("dd.MM.yyyy", srpski).apply { isLenient = false }

    private fun fmt(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, srpski).apply { isLenient = false }

    private val MESECNI_FORMATI: List<SimpleDateFormat> = listOf(
        fmt("MM/yyyy"),
        fmt("MM/yy")
    )

    private val DNEVNI_FORMATI: List<SimpleDateFormat> = listOf(
        fmt("dd.MM.yyyy"),
        fmt("d.M.yyyy"),
        fmt("dd/MM/yyyy"),
        fmt("d/M/yyyy"),
        fmt("yyyy-MM-dd")
    )

    private val OZNAKE_ISTEKA = listOf(
        "važi do",
        "vazi do",
        "datum isteka",
        "datum isteka registracije"
    )

    fun jeIstek(oznaka: String): Boolean {
        val mala = oznaka.trim().lowercase(srpski)
        return OZNAKE_ISTEKA.any { mala.contains(it) }
    }

    fun parsirajIstek(text: String): Date? {
        val trim = text.trim()
        if (trim.isEmpty()) return null

        for (f in MESECNI_FORMATI) {
            val d = parseExact(f, trim) ?: continue
            val cal = Calendar.getInstance()
            cal.time = d
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            return cal.time
        }

        for (f in DNEVNI_FORMATI) {
            val d = parseExact(f, trim) ?: continue
            return d
        }

        return null
    }

    fun formatiraj(date: Date): String = PRIKAZNI_FORMAT.format(date)

    /** Pravilan oblik reči za dane: "dan" za 1, 21, 31… inače "dana". */
    fun recDan(broj: Long): String {
        val n = kotlin.math.abs(broj)
        return if (n % 10L == 1L && n % 100L != 11L) "dan" else "dana"
    }

    fun fromMillisUtc(millis: Long): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val local = Calendar.getInstance().apply {
            clear()
            set(y, m, d)
        }
        return local.time
    }

    fun danaDoIsteka(istek: Date, sada: Date = Date()): Long {
        val krajDanaIstek = krajDana(istek).time
        val pocetakDanaSada = pocetakDana(sada).time
        val diff = krajDanaIstek - pocetakDanaSada
        return diff / (1000L * 60 * 60 * 24)
    }

    fun krajDana(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

    fun pocetakDana(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun parseExact(fmt: SimpleDateFormat, text: String): Date? {
        val pos = ParsePosition(0)
        val d = fmt.parse(text, pos) ?: return null
        return if (pos.index == text.length) d else null
    }
}
