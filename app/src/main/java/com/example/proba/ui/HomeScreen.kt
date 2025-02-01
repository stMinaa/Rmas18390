package com.example.proba.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(navController: NavController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Koristimo više dozvola odjednom (FINE i COARSE LOCATION)
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf(LatLng(44.7866, 20.4489)) } // Default: Beograd
    var cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    // Tražimo dozvolu i dobijamo trenutnu lokaciju
    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            fusedLocationClient.getCurrentLocation(100, null).addOnSuccessListener { location: Location? ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }
        }
    }

    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }
    val properties = remember { MapProperties(isMyLocationEnabled = locationPermissionState.allPermissionsGranted) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.5f), // Drawer zauzima pola ekrana
                drawerContentColor = MaterialTheme.colorScheme.background
            ) {
                Text(
                    "Meni",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") },
                    label = { Text("Profil", color = MaterialTheme.colorScheme.onBackground) },
                    selected = false,
                    onClick = { navController.navigate("Profile") }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mapa i Kupovina", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Meni", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = properties,
                    uiSettings = uiSettings,
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "Trenutna lokacija"
                    )
                }

                // Ako dozvola NIJE odobrena, prikazujemo dugme da je tražimo
                if (!locationPermissionState.allPermissionsGranted) {
                    Button(
                        onClick = { locationPermissionState.launchMultiplePermissionRequest() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Dozvoli pristup lokaciji")
                    }
                }
            }
        }
    }
}
