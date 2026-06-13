package com.wallora.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wallora.app.R
import com.wallora.app.ui.editor.EditorScreen
import com.wallora.app.ui.favorites.FavoritesScreen
import com.wallora.app.ui.history.HistoryScreen
import com.wallora.app.ui.home.HomeScreen
import com.wallora.app.ui.search.SearchScreen
import com.wallora.app.ui.settings.SettingsScreen

sealed class WalloraRoute(val route: String) {
    data object Home : WalloraRoute("home")
    data object Search : WalloraRoute("search")
    data object Favorites : WalloraRoute("favorites")
    data object History : WalloraRoute("history")
    data object Settings : WalloraRoute("settings")
    // Detail is a full-screen route, not in bottom nav
    data object Detail : WalloraRoute("detail/{globalKey}") {
        fun createRoute(globalKey: String): String {
            // URL-encode the globalKey to avoid path conflicts with the colon separator
            return "detail/${java.net.URLEncoder.encode(globalKey, "UTF-8")}"
        }
    }

    // Editor is a full-screen route launched from Detail
    data object Editor : WalloraRoute("editor/{globalKey}") {
        fun createRoute(globalKey: String): String =
            "editor/${java.net.URLEncoder.encode(globalKey, "UTF-8")}"
    }
}

private val fullScreenRoutes = setOf("detail/", "editor/")

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

    val isDetailRoute = fullScreenRoutes.any { prefix ->
        currentDestination?.route?.startsWith(prefix) == true
    }

    Scaffold(
        bottomBar = {
            if (!isDetailRoute) {
                NavigationBar {
                    bottomNavItems.forEach { (route, icon, labelRes) ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any {
                                it.route == route.route
                            } == true,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(androidx.compose.ui.res.stringResource(labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = WalloraRoute.Home.route) {
            composable(WalloraRoute.Home.route) {
                HomeScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { key ->
                        navController.navigate(WalloraRoute.Detail.createRoute(key))
                    },
                    onSearchClick = { navController.navigate(WalloraRoute.Search.route) },
                )
            }
            composable(WalloraRoute.Search.route) {
                SearchScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { key ->
                        navController.navigate(WalloraRoute.Detail.createRoute(key))
                    },
                )
            }
            composable(WalloraRoute.Favorites.route) {
                FavoritesScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { key ->
                        navController.navigate(WalloraRoute.Detail.createRoute(key))
                    },
                )
            }
            composable(WalloraRoute.History.route) {
                HistoryScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { key ->
                        navController.navigate(WalloraRoute.Detail.createRoute(key))
                    },
                )
            }
            composable(WalloraRoute.Settings.route) {
                SettingsScreen(contentPadding = innerPadding)
            }
            // Detail screen: full-screen, no bottom bar
            // NOTE: We pass the wallpaper via back-stack saved state to avoid re-fetching
            // for the MVP. If wallpaper isn't found in state, show loading.
            composable(WalloraRoute.Detail.route) { backStackEntry ->
                // Retrieve wallpaper from the previous back stack entry's saved state
                val wallpaper = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<com.wallora.app.domain.model.Wallpaper>("wallpaper")
                if (wallpaper != null) {
                    com.wallora.app.ui.detail.DetailScreen(
                        wallpaper = wallpaper,
                        onBack = { navController.popBackStack() },
                        onSetWallpaper = { /* handled inside DetailScreen via ViewModel */ },
                        onEditAndSet = { w ->
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("wallpaper", w)
                            navController.navigate(WalloraRoute.Editor.createRoute(w.globalKey))
                        },
                        onMoreLikeThis = { /* TODO Phase 2 search */ },
                    )
                } else {
                    // Wallpaper not in saved state — navigate back
                    navController.popBackStack()
                }
            }
            composable(WalloraRoute.Editor.route) {
                val wallpaper = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<com.wallora.app.domain.model.Wallpaper>("wallpaper")
                if (wallpaper != null) {
                    EditorScreen(
                        wallpaper = wallpaper,
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    navController.popBackStack()
                }
            }
        }
    }
}
