package com.example.proba

import SignupScreen
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.proba.ui.login.LoginScreen
import com.example.proba.ui.theme.ProbaTheme
import android.Manifest
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    //dodato
    private val clothesIdFromNotifState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //provera da li je prvi put posle instalacije
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val firstRun = prefs.getBoolean("first_run", true)

        if (firstRun) {
            // samo prvi put posle instalacije logout user
            FirebaseAuth.getInstance().signOut()
            prefs.edit().putBoolean("first_run", false).apply()
        }

        //dodato
        // notifikacija se obrađuje samo ako je app u backgroundu
        if (!isAppInForeground()) {
            clothesIdFromNotifState.value = intent?.getStringExtra("clothesId")
        }

        enableEdgeToEdge()
        setContent {
            ProbaTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color=MaterialTheme.colorScheme.background

                ) {
                    val navController = rememberNavController()
                    val currentUser = FirebaseAuth.getInstance().currentUser


                    val startDestination = if (currentUser != null) {
                        Route.HomeScreen().name
                    } else {
                        "login_flow"
                    }

                    //MyNavigation(navHostController = navController, startDestination = startDestination)
                    MyNavigation(
                        navHostController = navController,
                        startDestination = startDestination,
                        clothesIdFromNotifState = clothesIdFromNotifState
                    )
                }

            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // ignorisemo notifikaciju ako je app u foregroundu
        if (!isAppInForeground()) {
            clothesIdFromNotifState.value = intent.getStringExtra("clothesId")
        }
    }

    // helper funkcija
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

}

