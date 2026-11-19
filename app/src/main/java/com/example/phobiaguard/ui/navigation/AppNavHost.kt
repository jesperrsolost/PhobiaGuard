package com.example.phobiaguard.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.phobiaguard.ui.screens.CameraViewModel
import com.example.phobiaguard.ui.components.BottomNavigationBar
import com.example.phobiaguard.ui.components.bottomNavItems
import com.example.phobiaguard.ui.screens.MonitoringScreen
import com.example.phobiaguard.ui.screens.CameraScreen
import com.example.phobiaguard.ui.viewmodel.UserSettingsViewModel
import com.example.phobiaguard.ui.settings.SettingsScreen
import com.example.phobiaguard.ui.settings.SpiderModelTestViewModel

private const val TAG = "PhobiaNavHost"

/**
 * Navigates between the main screens: camera, screen monitoring, settings
 *
 * @param modifier - Ui modification
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val currentRoute = backStackEntry?.destination?.route
    val selectedItem = bottomNavItems.firstOrNull { it.route::class.qualifiedName == currentRoute }

    val cameraViewModel: CameraViewModel = viewModel(
        factory = CameraViewModel.factory(context)
    )
    val settingsViewModel: UserSettingsViewModel = viewModel(
        factory = UserSettingsViewModel.factory(context)
    )
    val spiderModelTestViewModel: SpiderModelTestViewModel = viewModel(
        factory = SpiderModelTestViewModel.factory(context)
    )
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "Navigated to: ${destination.route}")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (selectedItem != null) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { item ->
                        navController.navigate(item.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRo,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<CameraRo> {
                CameraScreen(cameraViewModel)
            }
            composable<MainRo> {
                MonitoringScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen(settingsViewModel, spiderModelTestViewModel,
                    onNavigateBack = {navController.popBackStack()})
            }
        }
    }
}
