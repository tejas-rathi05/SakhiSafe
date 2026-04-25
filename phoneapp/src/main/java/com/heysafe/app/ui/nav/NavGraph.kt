package com.heysafe.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
        composable(Routes.Splash) { SplashScreen() }
        composable(Routes.Login) { LoginScreen() }
        composable(Routes.Register) { RegisterScreen() }
        composable(Routes.Home) { HomeScreen() }
        composable(Routes.Vitals) { VitalsScreen() }
        composable(Routes.Contacts) { ContactsScreen() }
        composable(Routes.AddContact) { AddContactScreen() }
        composable(Routes.Help) { HelpScreen() }
        composable(Routes.About) { AboutScreen() }
        composable(Routes.ActiveAlert) { ActiveAlertScreen() }
    }
}
