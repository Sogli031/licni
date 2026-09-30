package com.licnipodaci.data

import android.content.Context

/**
 * Jedna instanca [PodaciManager] za ceo proces.
 *
 * Ranije ga je pravio MainActivity, pa mu pozadinski posao (auto-backup) nije mogao
 * da priđe. Dve instance bi držale dve odvojene liste istog fajla.
 */
object PodaciManagerProvider {
    @Volatile
    private var instance: PodaciManager? = null

    fun get(context: Context): PodaciManager {
        return instance ?: synchronized(this) {
            instance ?: PodaciManager(context.applicationContext).also { instance = it }
        }
    }
}
