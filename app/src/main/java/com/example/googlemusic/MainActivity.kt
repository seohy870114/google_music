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
import com.example.googlemusic.ui.player.PlayerViewModel
import com.example.googlemusic.ui.screens.*
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val authRepository = remember { AuthRepository(context) }
    
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context, settingsRepository))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val libraryViewModel: LibraryViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show the bottom bar
    val showBottomBar = screens.any { it.route == navBackStackEntry?.destination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
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
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController, 
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(homeViewModel, authViewModel)
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = libraryViewModel, 
                        authViewModel = authViewModel,
                        onPlayMedia = { list, index ->
                            playerViewModel.setPlaylist(list, index)
                            navController.navigate("player")
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsRepository, authViewModel)
                }
                composable("player") {
                    PlayerScreen(
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
