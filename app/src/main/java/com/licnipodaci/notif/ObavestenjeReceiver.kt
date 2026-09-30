package com.licnipodaci.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.licnipodaci.MainActivity
import com.licnipodaci.R
import com.licnipodaci.data.DatumUtil

class ObavestenjeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ObavestenjeManager.ACTION_PODSETNIK) return

        val stavkaId = intent.getStringExtra(ObavestenjeManager.EXTRA_STAVKA_ID) ?: return
        val naziv = intent.getStringExtra(ObavestenjeManager.EXTRA_NAZIV) ?: return
        val kategorija = intent.getStringExtra(ObavestenjeManager.EXTRA_KATEGORIJA).orEmpty()
        val oznaka = intent.getStringExtra(ObavestenjeManager.EXTRA_OZNAKA) ?: return
        val dani = intent.getIntExtra(ObavestenjeManager.EXTRA_DANI, 0)
        val istek = intent.getStringExtra(ObavestenjeManager.EXTRA_ISTEK).orEmpty()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        osiguraj(nm)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            stavkaId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val naslov = if (kategorija.isNotBlank()) "$naziv · $kategorija" else naziv
        val tekst = when {
            dani <= 0 -> "$oznaka ističe danas ($istek)"
            dani == 1 -> "$oznaka ističe sutra ($istek)"
            else -> "$oznaka ističe za $dani ${DatumUtil.recDan(dani.toLong())} ($istek)"
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(naslov)
            .setContentText(tekst)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tekst))
            .setSmallIcon(R.drawable.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        nm.notify(notifikacijaId(stavkaId, oznaka, dani), notif)
    }

    private fun osiguraj(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val kanal = NotificationChannel(
            CHANNEL_ID,
            "Podsetnici o isteku",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Obaveštenja kada se približava datum isteka"
            enableLights(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(kanal)
    }

    private fun notifikacijaId(stavkaId: String, oznaka: String, dani: Int): Int =
        "${stavkaId}_${oznaka}_$dani".hashCode()

    companion object {
        const val CHANNEL_ID = "isteci_v1"
    }
}
