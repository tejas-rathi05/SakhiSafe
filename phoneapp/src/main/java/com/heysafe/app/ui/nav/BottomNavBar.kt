package com.heysafe.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val items = listOf(
    BottomItem(Routes.Home, "Home", Icons.Outlined.Home),
    BottomItem(Routes.Vitals, "Vitals", Icons.Outlined.Favorite),
    // Trip icon: Star is the closest pin/landmark glyph in core material-icons (extended
    // dependency intentionally not added). Swap when a proper place pin is available.
    BottomItem(Routes.Trip, "Trip", Icons.Outlined.Star),
    BottomItem(Routes.About, "Info", Icons.Outlined.Info),
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.Home)
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
