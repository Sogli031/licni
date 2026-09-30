package com.licnipodaci.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.licnipodaci.data.PodaciJson
import java.io.File

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val app = context.applicationContext
        val fajl = File(app.filesDir, "licni_podaci.json")
        if (!fajl.exists()) return

        val tekst = runCatching { fajl.readText(Charsets.UTF_8) }.getOrNull() ?: return
        val stavke = PodaciJson.izJson(tekst).getOrNull() ?: return

        ObavestenjeManager.rasporediSve(app, stavke)
    }
}
