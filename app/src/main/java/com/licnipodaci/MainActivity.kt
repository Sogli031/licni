package com.licnipodaci

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.PodaciManagerProvider
import com.licnipodaci.data.backup.GoogleDriveBackup
import com.licnipodaci.ui.navigation.NavGraph
import com.licnipodaci.ui.screens.CloudRestorePonuda
import com.licnipodaci.ui.theme.LicniPodaciTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var googleDriveBackup: GoogleDriveBackup

    private val zahtevDozvole = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* korisnik bira; nezavisno radimo dalje */ }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
        }.getOrNull()

        googleDriveBackup.handleSignInResult(account)
        if (account != null) {
            // Sveža instalacija + postojeći backup u oblaku -> ponudi restore
            lifecycleScope.launch { googleDriveBackup.checkRestoreOfferAfterSignIn() }
        }
        Toast.makeText(
            this,
            if (account != null) "Google nalog prijavljen" else "Google prijava nije završena",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        googleDriveBackup = GoogleDriveBackup(applicationContext)
        // Ista instanca koju koristi i auto-backup u pozadini
        val manager = PodaciManagerProvider.get(applicationContext)
        zatraziDozvoluZaNotifikacije()
        setContent {
            LicniPodaciTheme {
                NavGraph(
                    manager = manager,
                    driveBackup = googleDriveBackup,
                    onGoogleSignIn = {
                        googleSignInLauncher.launch(googleDriveBackup.getSignInIntent())
                    }
                )
                CloudRestorePonuda(manager = manager, driveBackup = googleDriveBackup)
            }
        }
    }

    private fun zatraziDozvoluZaNotifikacije() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val odobreno = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!odobreno) {
            zahtevDozvole.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
