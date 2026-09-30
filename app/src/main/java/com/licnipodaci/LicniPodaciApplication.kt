package com.licnipodaci

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.licnipodaci.data.backup.BackupScheduler
import com.licnipodaci.notif.ObavestenjeReceiver
import com.programi.crashlog.CrashLogStorage

class LicniPodaciApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogStorage.install(
            application = this,
            appName = getString(R.string.app_name),
            sanitize = ::sanitizeCrashLog,
            writePublicDocuments = false
        )
        kreirajKanal()

        // Dnevni backup ako korisnik dugo ne menja ništa (KEEP: ne resetuje se na svakom startu)
        BackupScheduler.schedulePeriodic(this)
    }

    private fun kreirajKanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(ObavestenjeReceiver.CHANNEL_ID) != null) return
        val kanal = NotificationChannel(
            ObavestenjeReceiver.CHANNEL_ID,
            "Podsetnici o isteku",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Obaveštenja kada se približava datum isteka"
            enableLights(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(kanal)
    }

    private fun sanitizeCrashLog(text: String): String {
        return longDigitSequenceRegex.replace(
            sensitiveLabeledValueRegex.replace(text) { match ->
                "${match.groupValues[1]}${match.groupValues[2]}[REDACTED]"
            },
            "[REDACTED]"
        )
    }

    private companion object {
        private val sensitiveLabeledValueRegex = Regex(
            pattern = """\b(pin|cvv|jmbg|lozinka|broj\s+kartice|broj\s+lične\s+karte|broj\s+pasoša|broj\s+vozačke|broj\s+računa\s*/\s*iban|broj\s+računa|iban)\b(\s*[:=]\s*)([^\r\n,;]*)""",
            option = RegexOption.IGNORE_CASE
        )

        private val longDigitSequenceRegex = Regex("""(?<!\d)(?:\d[\s-]?){6,}\d(?!\d)""")
    }
}
