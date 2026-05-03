package com.heysafe.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.nav.BottomNavBar
import com.heysafe.app.ui.nav.HeyNavGraph
import com.heysafe.app.ui.nav.Routes
import com.heysafe.app.ui.theme.HeySafeTheme

private val ROUTES_WITH_BOTTOM_NAV = setOf(
    Routes.Home, Routes.Vitals, Routes.Help, Routes.About
)

@Composable
fun HeySafeApp(startDestination: String = Routes.Splash) {
    HeySafeTheme {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showBar = currentRoute in ROUTES_WITH_BOTTOM_NAV

        // Observe orchestrator progress and auto-navigate to ActiveAlert when one starts.
        val progress by ServiceLocator.alertOrchestrator.progress.collectAsState()
        LaunchedEffect(progress.alertId, progress.resolved) {
            val id = progress.alertId
            if (id != null && !progress.resolved && currentRoute != Routes.ActiveAlert) {
                navController.navigate(Routes.ActiveAlert)
            }
        }

        Scaffold(
            bottomBar = { if (showBar) BottomNavBar(navController) }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                HeyNavGraph(navController, startDestination)
            }
        }
    }
}
