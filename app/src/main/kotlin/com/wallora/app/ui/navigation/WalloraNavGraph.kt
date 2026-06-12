package com.wallora.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wallora.app.R
import com.wallora.app.ui.favorites.FavoritesScreen
import com.wallora.app.ui.history.HistoryScreen
import com.wallora.app.ui.home.HomeScreen
import com.wallora.app.ui.settings.SettingsScreen

sealed class WalloraRoute(val route: String) {
    data object Home : WalloraRoute("home")
    data object Favorites : WalloraRoute("favorites")
    data object History : WalloraRoute("history")
    data object Settings : WalloraRoute("settings")
    data object Detail : WalloraRoute("detail/{wallpaperId}") {
        fun createRoute(id: String) = "detail/$id"
    }
}

private val bottomNavItems = listOf(
    Triple(WalloraRoute.Home, Icons.Default.Home, R.string.nav_home),
    Triple(WalloraRoute.Favorites, Icons.Default.Favorite, R.string.nav_favorites),
    Triple(WalloraRoute.History, Icons.Default.History, R.string.nav_history),
    Triple(WalloraRoute.Settings, Icons.Default.Settings, R.string.nav_settings),
)

@Composable
fun WalloraNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { (route, icon, labelRes) ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == route.route } == true,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = stringResource(labelRes)) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = WalloraRoute.Home.route) {
            composable(WalloraRoute.Home.route) {
                HomeScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { id ->
                        navController.navigate(WalloraRoute.Detail.createRoute(id))
                    },
                )
            }
            composable(WalloraRoute.Favorites.route) {
                FavoritesScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { id ->
                        navController.navigate(WalloraRoute.Detail.createRoute(id))
                    },
                )
            }
            composable(WalloraRoute.History.route) {
                HistoryScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { id ->
                        navController.navigate(WalloraRoute.Detail.createRoute(id))
                    },
                )
            }
            composable(WalloraRoute.Settings.route) {
                SettingsScreen(contentPadding = innerPadding)
            }
        }
    }
}
