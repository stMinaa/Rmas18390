package com.example.proba

import SignupScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.proba.ui.HomeScreen
import com.example.proba.ui.ProfilePage.ProfilePage
import com.example.proba.ui.clothes.AddClothesScreen
import com.example.proba.ui.clothes.ClothesDetailScreen
import com.example.proba.ui.login.LoginScreen
import com.example.proba.ui.signup.UploadProfilePictureScreen

sealed class Route {
    data class LoginScreen(val name: String = "Login") : Route()
    data class SignUpScreen(val name: String = "SignUp") : Route()
    data class HomeScreen(val name: String = "Home") : Route()
    data class ProfilePage(val name: String = "Profile") : Route()
    data class UploadProfilePictureScreen(val name: String = "ProfilePicture") : Route()
    data class AddClothesScreen(val name: String="AddClothes"):Route()
    data class ClothesDetailScreen(val name: String="ClothesDetail"):Route()
}


@Composable
fun MyNavigation(navHostController: NavHostController, startDestination: String, clothesIdFromNotifState: MutableState<String?>) {

    //dodato
    val isConsumed = remember { mutableStateOf(false) }

    // kada stigne novi clothesId iz notifikacije
    LaunchedEffect(clothesIdFromNotifState.value) {
        val id = clothesIdFromNotifState.value
        if (id != null && !isConsumed.value) {
            navHostController.navigate("ClothesDetail/$id") {
                popUpTo(navHostController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
            isConsumed.value = true // markiramo kao obrađeno
        }
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination,
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

        composable("AddClothes") {
            AddClothesScreen(navController = navHostController)
        }

        composable("ClothesDetail/{clothId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("clothId") ?: ""
            ClothesDetailScreen(navHostController, id)
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


