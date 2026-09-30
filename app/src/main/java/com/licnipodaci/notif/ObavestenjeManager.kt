package com.licnipodaci.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.licnipodaci.data.DatumUtil
import com.licnipodaci.data.Stavka
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

object ObavestenjeManager {

    val DANI_PRE_ISTEKA = listOf(30, 15, 10, 5, 1)
    private const val SAT_OBAVESTENJA = 9
    private const val MAKS_POLJA_PO_STAVCI = 32

    fun rasporedi(context: Context, stavka: Stavka) {
        otkazi(context, stavka.id)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sada = System.currentTimeMillis()

        stavka.polja.forEachIndexed { idx, polje ->
            if (!DatumUtil.jeIstek(polje.oznaka)) return@forEachIndexed
            val istek = DatumUtil.parsirajIstek(polje.vrednost) ?: return@forEachIndexed

            DANI_PRE_ISTEKA.forEach { dani ->
                val triger = vremeAlarma(istek, dani)
                if (triger <= sada) return@forEach
                val intent = napraviIntent(context, stavka, polje.oznaka, dani, istek)
                val pi = pendingIntent(context, stavka.id, idx, dani, intent, mutirajuci = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triger, pi)
                } else {
                    @Suppress("DEPRECATION")
                    am.set(AlarmManager.RTC_WAKEUP, triger, pi)
                }
            }
        }
    }

    fun otkazi(context: Context, stavkaId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (idx in 0 until MAKS_POLJA_PO_STAVCI) {
            DANI_PRE_ISTEKA.forEach { dani ->
                // Akcija mora da bude ista kao pri zakazivanju, inače PendingIntent ne
                // odgovara zakazanom i alarm ostaje aktivan.
                val intent = Intent(context, ObavestenjeReceiver::class.java).apply {
                    action = ACTION_PODSETNIK
                }
                val pi = pendingIntent(context, stavkaId, idx, dani, intent, mutirajuci = false)
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun rasporediSve(context: Context, stavke: List<Stavka>) {
        stavke.forEach { rasporedi(context, it) }
    }

    private fun vremeAlarma(istek: Date, danaPre: Int): Long {
        val cal = Calendar.getInstance()
        cal.time = istek
        cal.add(Calendar.DAY_OF_MONTH, -danaPre)
        cal.set(Calendar.HOUR_OF_DAY, SAT_OBAVESTENJA)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun napraviIntent(
        context: Context,
        stavka: Stavka,
        oznaka: String,
        dani: Int,
        istek: Date
    ): Intent = Intent(context, ObavestenjeReceiver::class.java).apply {
        action = ACTION_PODSETNIK
        putExtra(EXTRA_STAVKA_ID, stavka.id)
        putExtra(EXTRA_NAZIV, stavka.naziv)
        putExtra(EXTRA_KATEGORIJA, stavka.kategorija.naziv)
        putExtra(EXTRA_OZNAKA, oznaka)
        putExtra(EXTRA_DANI, dani)
        putExtra(EXTRA_ISTEK, DatumUtil.formatiraj(istek))
    }

    private fun pendingIntent(
        context: Context,
        stavkaId: String,
        poljeIdx: Int,
        dani: Int,
        intent: Intent,
        mutirajuci: Boolean
    ): PendingIntent {
        val request = requestCode(stavkaId, poljeIdx, dani)
        val flags = (if (mutirajuci) 0 else PendingIntent.FLAG_IMMUTABLE) or
            PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, request, intent, flags)
    }

    private fun requestCode(stavkaId: String, poljeIdx: Int, dani: Int): Int {
        val mesovito = stavkaId.hashCode().toLong() * 1009L + poljeIdx * 17L + dani
        return abs(mesovito.toInt())
    }

    const val ACTION_PODSETNIK = "com.licnipodaci.action.PODSETNIK"
    const val EXTRA_STAVKA_ID = "stavka_id"
    const val EXTRA_NAZIV = "naziv"
    const val EXTRA_KATEGORIJA = "kategorija"
    const val EXTRA_OZNAKA = "oznaka"
    const val EXTRA_DANI = "dani"
    const val EXTRA_ISTEK = "istek"
}
