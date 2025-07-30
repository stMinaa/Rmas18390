package com.example.proba

import SignupScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.proba.ui.HomeScreen
import com.example.proba.ui.ProfilePage.ProfilePage
import com.example.proba.ui.login.LoginScreen
import com.example.proba.ui.signup.UploadProfilePictureScreen

sealed class Route {
    data class LoginScreen(val name: String = "Login") : Route()
    data class SignUpScreen(val name: String = "SignUp") : Route()
    data class HomeScreen(val name: String = "Home") : Route()
    data class ProfilePage(val name: String = "Profile") : Route()
    data class UploadProfilePictureScreen(val name: String = "ProfilePicture") : Route()
}


@Composable
fun MyNavigation(navHostController: NavHostController) {
    NavHost(
        navController = navHostController,
        startDestination = "login_flow",
    ) {
        navigation(
            startDestination = Route.LoginScreen().name,
            route = "login_flow"
        ) {
            composable(Route.LoginScreen().name) {
                LoginScreen(navController = navHostController)
            }
            composable(Route.SignUpScreen().name) {
                SignupScreen(navController = navHostController)
            }
            composable(Route.UploadProfilePictureScreen().name) {
                UploadProfilePictureScreen(navController = navHostController)
            }
        }

        composable(Route.HomeScreen().name) {
            HomeScreen(navController = navHostController)
        }

        composable(Route.ProfilePage().name) {
            ProfilePage(navController = navHostController)
        }
    }
}


fun NavController.navigateToSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}


