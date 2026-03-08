package com.example.googlemusic.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.googlemusic.ui.screens.HomeScreen
import com.example.googlemusic.ui.screens.LibraryScreen
import com.example.googlemusic.ui.screens.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Library.route) {
            LibraryScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
