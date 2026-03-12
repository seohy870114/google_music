package com.example.googlemusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.googlemusic.data.auth.AuthRepository
import com.example.googlemusic.data.auth.AuthViewModel
import com.example.googlemusic.data.auth.AuthViewModelFactory
import com.example.googlemusic.data.repository.SettingsRepository
import com.example.googlemusic.navigation.Screen
import com.example.googlemusic.ui.home.HomeViewModel
import com.example.googlemusic.ui.home.HomeViewModelFactory
import com.example.googlemusic.ui.library.LibraryViewModel
import com.example.googlemusic.ui.screens.HomeScreen
import com.example.googlemusic.ui.screens.LibraryScreen
import com.example.googlemusic.ui.screens.SettingsScreen
import com.example.googlemusic.ui.theme.GoogleMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoogleMusicTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Settings
    )

    // Shared ViewModels to persist state across navigation
    val settingsRepository = SettingsRepository(androidx.compose.ui.platform.LocalContext.current)
    val authRepository = AuthRepository(androidx.compose.ui.platform.LocalContext.current)
    
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(settingsRepository))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val libraryViewModel: LibraryViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    HomeScreen(homeViewModel, authViewModel)
                }
                composable(Screen.Library.route) {
                    LibraryScreen(libraryViewModel, authViewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsRepository, authViewModel)
                }
            }
        }
    }
}
