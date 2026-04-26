package com.heysafe.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.heysafe.app.di.ServiceLocator
import com.heysafe.app.ui.about.AboutScreen
import com.heysafe.app.ui.alert.ActiveAlertScreen
import com.heysafe.app.ui.auth.LoginScreen
import com.heysafe.app.ui.auth.RegisterScreen
import com.heysafe.app.ui.contacts.AddContactScreen
import com.heysafe.app.ui.contacts.ContactsScreen
import com.heysafe.app.ui.help.HelpScreen
import com.heysafe.app.ui.home.HomeScreen
import com.heysafe.app.ui.splash.SplashScreen
import com.heysafe.app.ui.vitals.VitalsScreen

@Composable
fun HeyNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController, startDestination = startDestination) {
        composable(Routes.Splash) {
            SplashScreen(
                onAuthenticated = {
                    navController.navigate(Routes.Home) { popUpTo(Routes.Splash) { inclusive = true } }
                },
                onUnauthenticated = {
                    navController.navigate(Routes.Login) { popUpTo(Routes.Splash) { inclusive = true } }
                },
            )
        }
        composable(Routes.Login) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } }
                },
                onGoToRegister = { navController.navigate(Routes.Register) },
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onGoBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                onSoundAlarmTap = { ServiceLocator.soundAlarmController.toggle() },
                onManageContacts = { navController.navigate(Routes.Contacts) },
            )
        }
        composable(Routes.Vitals) { VitalsScreen() }
        composable(Routes.Contacts) {
            ContactsScreen(
                onAddContact = { navController.navigate(Routes.AddContact) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.AddContact) {
            AddContactScreen(
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Routes.Help) { HelpScreen() }
        composable(Routes.About) { AboutScreen() }
        composable(Routes.ActiveAlert) {
            ActiveAlertScreen(onResolved = {
                if (!navController.popBackStack(Routes.Home, inclusive = false)) {
                    navController.navigate(Routes.Home) { popUpTo(0) { inclusive = true } }
                }
            })
        }
    }
}
