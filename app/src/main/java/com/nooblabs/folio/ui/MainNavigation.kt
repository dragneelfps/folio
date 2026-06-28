package com.nooblabs.folio.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nooblabs.folio.ui.assets.AssetsScreen
import com.nooblabs.folio.ui.dashboard.DashboardScreen
import com.nooblabs.folio.ui.dashboard.DashboardViewModel
import com.nooblabs.folio.ui.settings.SettingsScreen
import com.nooblabs.folio.ui.settings.SettingsViewModel
import com.nooblabs.folio.ui.transactions.TransactionsScreen
import com.nooblabs.folio.ui.transactions.TransactionsViewModel
import com.nooblabs.folio.ui.onboarding.WelcomeScreen

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    object Assets   : Screen("assets",    "Assets",    Icons.Filled.Wallet)
    object Transactions : Screen("transactions", "Transactions", Icons.Filled.List)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Assets,
    Screen.Transactions
)

@Composable
fun MainNavigation(settingsRepository: com.nooblabs.folio.domain.repository.SettingsRepository) {
    val navController = rememberNavController()
    val userName by settingsRepository.userName.collectAsState()
    val startDestination = if (userName.isBlank()) "welcome" else Screen.Dashboard.route

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route
            val isTopLevel = currentRoute == Screen.Dashboard.route ||
                    currentRoute?.startsWith("assets") == true ||
                    currentRoute == Screen.Transactions.route
            if (isTopLevel) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route == screen.route || (screen.route == "assets" && it.route?.startsWith("assets") == true)
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = startDestination, Modifier.padding(innerPadding)) {
            composable("welcome") {
                WelcomeScreen(
                    onGetStarted = { name ->
                        settingsRepository.setUserName(name)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToScreen = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = "assets?tab={tabIndex}",
                arguments = listOf(
                    navArgument("tabIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val tabIndex = backStackEntry.arguments?.getInt("tabIndex") ?: 0
                AssetsScreen(
                    initialTabIndex = tabIndex,
                    settingsRepository = settingsRepository,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                TransactionsScreen(viewModel, onNavigateToSettings = { navController.navigate("settings") })
            }
            composable("settings") {
                val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
