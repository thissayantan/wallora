package com.wallora.app.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wallora.app.R
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.ui.detail.DetailScreen
import com.wallora.app.ui.editor.EditorScreen
import com.wallora.app.ui.favorites.FavoritesScreen
import com.wallora.app.ui.history.HistoryScreen
import com.wallora.app.ui.home.HomeScreen
import com.wallora.app.ui.search.SearchScreen
import com.wallora.app.ui.settings.SettingsScreen
import com.wallora.app.ui.settings.SettingsViewModel
import com.wallora.app.ui.settings.WalloraSettingsRoute
import com.wallora.app.ui.settings.pages.SettingsAboutPage
import com.wallora.app.ui.settings.pages.SettingsCategoriesPage
import com.wallora.app.ui.settings.pages.SettingsLivePage
import com.wallora.app.ui.settings.pages.SettingsRemotePage
import com.wallora.app.ui.settings.pages.SettingsRotationPage
import com.wallora.app.ui.settings.pages.SettingsSourcesPage

sealed class WalloraRoute(val route: String) {
    data object Home : WalloraRoute("home")
    data object Search : WalloraRoute("search")
    data object Favorites : WalloraRoute("favorites")
    data object History : WalloraRoute("history")
    data object Settings : WalloraRoute("settings")
    // Settings sub-pages (bottom bar hidden on these)
    data object SettingsSources : WalloraRoute(WalloraSettingsRoute.SOURCES)
    data object SettingsCategories : WalloraRoute(WalloraSettingsRoute.CATEGORIES)
    data object SettingsRotation : WalloraRoute(WalloraSettingsRoute.ROTATION)
    data object SettingsLive : WalloraRoute(WalloraSettingsRoute.LIVE)
    data object SettingsRemote : WalloraRoute(WalloraSettingsRoute.REMOTE)
    data object SettingsAbout : WalloraRoute(WalloraSettingsRoute.ABOUT)
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

// "settings/" prefix matches all sub-pages but NOT the master "settings" tab itself.
private val fullScreenRoutes = setOf("detail/", "editor/", "settings/")

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
                            label = { Text(stringResource(labelRes)) },
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
                    onWallpaperClick = { wallpaper ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("wallpaper", wallpaper)
                        navController.navigate(WalloraRoute.Detail.createRoute(wallpaper.globalKey))
                    },
                    onSearchClick = { navController.navigate(WalloraRoute.Search.route) },
                )
            }
            composable(WalloraRoute.Search.route) {
                SearchScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { wallpaper ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("wallpaper", wallpaper)
                        navController.navigate(WalloraRoute.Detail.createRoute(wallpaper.globalKey))
                    },
                )
            }
            composable(WalloraRoute.Favorites.route) {
                FavoritesScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { wallpaper ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("wallpaper", wallpaper)
                        navController.navigate(WalloraRoute.Detail.createRoute(wallpaper.globalKey))
                    },
                )
            }
            composable(WalloraRoute.History.route) {
                HistoryScreen(
                    contentPadding = innerPadding,
                    onWallpaperClick = { wallpaper ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle?.set("wallpaper", wallpaper)
                        navController.navigate(WalloraRoute.Detail.createRoute(wallpaper.globalKey))
                    },
                )
            }
            composable(WalloraRoute.Settings.route) {
                SettingsScreen(
                    contentPadding = innerPadding,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }

            // ── Settings sub-pages (shared single ViewModel scoped to master entry) ──
            // Slide transition: drill-in from right, pop back to left.
            val settingsEnter = slideInHorizontally { it }
            val settingsExit = slideOutHorizontally { -it / 3 }
            val settingsPopEnter = slideInHorizontally { -it / 3 }
            val settingsPopExit = slideOutHorizontally { it }

            composable(
                route = WalloraRoute.SettingsSources.route,
                enterTransition = { settingsEnter },
                exitTransition = { settingsExit },
                popEnterTransition = { settingsPopEnter },
                popExitTransition = { settingsPopExit },
            ) { entry ->
                val parent = remember(entry) {
                    navController.getBackStackEntry(WalloraRoute.Settings.route)
                }
                val vm: SettingsViewModel = hiltViewModel(parent)
                SettingsSourcesPage(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = WalloraRoute.SettingsCategories.route,
                enterTransition = { settingsEnter },
                exitTransition = { settingsExit },
                popEnterTransition = { settingsPopEnter },
                popExitTransition = { settingsPopExit },
            ) { entry ->
                val parent = remember(entry) {
                    navController.getBackStackEntry(WalloraRoute.Settings.route)
                }
                val vm: SettingsViewModel = hiltViewModel(parent)
                SettingsCategoriesPage(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = WalloraRoute.SettingsRotation.route,
                enterTransition = { settingsEnter },
                exitTransition = { settingsExit },
                popEnterTransition = { settingsPopEnter },
                popExitTransition = { settingsPopExit },
            ) { entry ->
                val parent = remember(entry) {
                    navController.getBackStackEntry(WalloraRoute.Settings.route)
                }
                val vm: SettingsViewModel = hiltViewModel(parent)
                SettingsRotationPage(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = WalloraRoute.SettingsLive.route,
                enterTransition = { settingsEnter },
                exitTransition = { settingsExit },
                popEnterTransition = { settingsPopEnter },
                popExitTransition = { settingsPopExit },
            ) { entry ->
                val parent = remember(entry) {
                    navController.getBackStackEntry(WalloraRoute.Settings.route)
                }
                val vm: SettingsViewModel = hiltViewModel(parent)
                SettingsLivePage(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = WalloraRoute.SettingsRemote.route,
                enterTransition = { settingsEnter },
                exitTransition = { settingsExit },
                popEnterTransition = { settingsPopEnter },
                popExitTransition = { settingsPopExit },
            ) {
                SettingsRemotePage(onBack = { navController.popBackStack() })
            }
            composable(
                route = WalloraRoute.SettingsAbout.route,
                enterTransition = { settingsEnter },
                exitTransition = { settingsExit },
                popEnterTransition = { settingsPopEnter },
                popExitTransition = { settingsPopExit },
            ) {
                SettingsAboutPage(onBack = { navController.popBackStack() })
            }
            // Detail screen: full-screen, no bottom bar
            // NOTE: We pass the wallpaper via back-stack saved state to avoid re-fetching
            // for the MVP. If wallpaper isn't found in state, show loading.
            composable(WalloraRoute.Detail.route) { backStackEntry ->
                // Remember the wallpaper keyed on backStackEntry so it is read exactly once
                // when the composable enters the composition. During the exit animation the
                // back-stack changes and previousBackStackEntry becomes null, which without
                // this remember would trigger a spurious second popBackStack() and blank the screen.
                val wallpaper = remember(backStackEntry) {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.get<Wallpaper>("wallpaper")
                }
                if (wallpaper != null) {
                    DetailScreen(
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
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
            composable(WalloraRoute.Editor.route) {
                val wallpaper = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Wallpaper>("wallpaper")
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
