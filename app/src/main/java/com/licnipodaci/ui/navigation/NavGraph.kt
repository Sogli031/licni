package com.licnipodaci.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.licnipodaci.data.PodaciManager
import com.licnipodaci.data.backup.GoogleDriveBackup
import com.licnipodaci.ui.screens.DetaljiScreen
import com.licnipodaci.ui.screens.ListaScreen
import com.licnipodaci.ui.screens.PocetnaScreen
import com.licnipodaci.ui.screens.SifreGrupaScreen
import com.licnipodaci.ui.screens.UrediScreen

@Composable
fun NavGraph(
    manager: PodaciManager,
    driveBackup: GoogleDriveBackup,
    onGoogleSignIn: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Ruta.POCETNA) {
        composable(Ruta.POCETNA) {
            PocetnaScreen(
                manager = manager,
                navController = navController,
                driveBackup = driveBackup,
                onGoogleSignIn = onGoogleSignIn
            )
        }
        composable(
            route = Ruta.LISTA_OBRAZAC,
            arguments = listOf(navArgument("sekcija") { type = NavType.StringType })
        ) { entry ->
            ListaScreen(
                sekcija = Ruta.sekcijaIz(entry.arguments?.getString("sekcija")),
                manager = manager,
                navController = navController,
                driveBackup = driveBackup,
                onGoogleSignIn = onGoogleSignIn
            )
        }
        composable(
            route = Ruta.GRUPA_OBRAZAC,
            arguments = listOf(navArgument("naziv") { type = NavType.StringType })
        ) { entry ->
            SifreGrupaScreen(
                naziv = entry.arguments?.getString("naziv").orEmpty(),
                manager = manager,
                navController = navController
            )
        }
        composable(
            route = "${Ruta.DETALJI}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            DetaljiScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                manager = manager,
                navController = navController
            )
        }
        composable(
            route = Ruta.UREDI_OBRAZAC,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("sekcija") {
                    type = NavType.StringType
                    defaultValue = "PODACI"
                },
                navArgument("naziv") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            UrediScreen(
                id = entry.arguments?.getString("id"),
                sekcija = Ruta.sekcijaIz(entry.arguments?.getString("sekcija")),
                unapredNaziv = entry.arguments?.getString("naziv"),
                manager = manager,
                navController = navController
            )
        }
    }
}
