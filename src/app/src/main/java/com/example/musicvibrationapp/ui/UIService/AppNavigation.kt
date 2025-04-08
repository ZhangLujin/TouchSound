package com.example.musicvibrationapp.ui.UIService

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicvibrationapp.MainActivity
import com.example.musicvibrationapp.ui.screens.HomeScreen

@Composable
fun AppNavigation(activity: Activity) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                activity = activity,
                onDeveloperModeClick = {
                    activity.startActivity(Intent(activity, MainActivity::class.java))
                },
                onConnectClick = {
                }
            )
        }
    }
}