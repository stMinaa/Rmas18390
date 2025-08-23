package com.example.proba.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.proba.R
import com.example.proba.service.LocationService
import com.example.proba.ui.components.LocationTracker
import com.example.proba.ui.theme.Pink60
import com.example.proba.service.ServiceStateManager
import com.example.proba.ui.components.PointsManager
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.example.proba.ui.components.bitmapDescriptorFromVector
import com.example.proba.ui.components.createCustomMarker
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(navController: NavController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var userLocation by remember { mutableStateOf(LatLng(44.8176, 20.4569)) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    var hasMovedCamera by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(ServiceStateManager.isServiceRunning(context)) }

    // Marker za isticanje pin-a
    var highlightedMarker by remember { mutableStateOf<String?>(null) }

    val clothesList = remember { mutableStateListOf<Map<String, Any>>() }

    // NOTIFIKACIJE
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Dozvola za notifikacije nije odobrena. Nećete primati obaveštenja.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // START/STOP servis
    fun startLocationService() {
        val intent = Intent(context, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
        ServiceStateManager.saveServiceState(context, true)
    }

    fun stopLocationService() {
        val intent = Intent(context, LocationService::class.java)
        context.stopService(intent)
        ServiceStateManager.saveServiceState(context, false)
    }

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // --- FIX ZA NOTIFIKACIJE I PIN ---
    LaunchedEffect(activity) {
        // Navigacija ka ClothesDetail odmah
        snapshotFlow { activity?.intent?.getStringExtra("clothesId") }
            .collect { id ->
                id?.let {
                    navController.navigate("ClothesDetail/$it") { launchSingleTop = true }
                    activity?.intent?.removeExtra("clothesId")
                }
            }
    }

    LaunchedEffect(activity) {
        // Isticanje pin-a
        snapshotFlow {
            activity?.intent?.getStringExtra("highlightPinId")
                ?: activity?.intent?.getStringExtra("open_object_id")
        }.collect { pinId ->
            pinId?.let {
                highlightedMarker = it
                FirebaseFirestore.getInstance()
                    .collection("clothes")
                    .document(it)
                    .get()
                    .addOnSuccessListener { doc ->
                        val lat = (doc["latitude"] as? Double) ?: return@addOnSuccessListener
                        val lon = (doc["longitude"] as? Double) ?: return@addOnSuccessListener
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lon), 17f)
                    }
                delay(5000)
                highlightedMarker = null
                activity?.intent?.removeExtra("highlightPinId")
                activity?.intent?.removeExtra("open_object_id")
            }
        }
    }

    // Učitaj sve pinove odeće
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("clothes")
            .get()
            .addOnSuccessListener { result ->
                clothesList.clear()
                for (doc in result) {
                    val clothData = doc.data.toMutableMap()
                    clothData["id"] = doc.id
                    clothesList.add(clothData)
                }
            }
    }




    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.5f),
                drawerContentColor = MaterialTheme.colorScheme.background
            ) {
                Text(
                    "Meni",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servis",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = isTracking,
                        onCheckedChange = { isChecked ->
                            isTracking = isChecked
                            if (isChecked) startLocationService()
                            else stopLocationService()
                        }
                    )
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") },
                    label = { Text("Profil", color = MaterialTheme.colorScheme.onBackground) },
                    selected = false,
                    onClick = { navController.navigate("Profile") }
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_leadboard),
                            contentDescription = "Rang lista") },
                    label = { Text("Rang lista", color = MaterialTheme.colorScheme.onBackground) },
                    selected = false,
                    onClick = { navController.navigate("leaderboard") }
                )

                NavigationDrawerItem(

                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        stopLocationService()
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login_flow") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mapa i Lokacija", color = Color.White) },
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
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    // marker za usera
                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "Tvoja lokacija",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // marker-i odeće
                    clothesList.forEach { data ->
                        val status = data["status"]?.toString() ?: ""
                        if (status == "Prodato") return@forEach

                        val lat = (data["latitude"] as? Double) ?: return@forEach
                        val lon = (data["longitude"] as? Double) ?: return@forEach
                        val id = data["id"]?.toString() ?: return@forEach
                        val description = data["description"]?.toString() ?: "Nema opisa"

                        val markerIcon = if (id == highlightedMarker) {
                            createCustomMarker(context, R.drawable.ic_purple_pin, R.drawable.ic_clothes)
                        } else {
                            createCustomMarker(context, R.drawable.ic_pink_pin2, R.drawable.ic_clothes)
                        }

                        Marker(
                            state = MarkerState(position = LatLng(lat, lon)),
                            title = description,
                            icon = markerIcon,

                            onClick = {
                                // Provera distance u metrima
                                val distance = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    userLocation.latitude, userLocation.longitude,
                                    lat, lon,
                                    distance
                                )

                                if (userId != null && distance[0] <= 20f) { // 20 metara
                                    PointsManager.addPoints(userId, 1)
                                    Toast.makeText(context, "Dobili ste poen +1.", Toast.LENGTH_SHORT).show()

                                } else if (distance[0] > 20f) {
                                    Toast.makeText(context, "Morate biti bliže markeru da biste dobili poen", Toast.LENGTH_SHORT).show()
                                }

                                navController.navigate("ClothesDetail/$id")
                                true

                            }
                        )

                    }
                }


                if (!locationPermissionState.allPermissionsGranted) {
                    Button(
                        onClick = { locationPermissionState.launchMultiplePermissionRequest() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text("Dozvoli lokaciju")
                    }
                }

                FloatingActionButton(
                    onClick = {
                        navController.navigate("AddClothes")
                    },
                    containerColor = Pink60,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Dodaj odeću",
                        modifier = Modifier
                            .size(60.dp)
                            .padding(16.dp)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation, 15f)
                    },
                    containerColor = Pink60,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 65.dp, bottom = 16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_center),
                        contentDescription = "Centiraj me",
                        modifier = Modifier
                            .size(60.dp)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // Lokacijski tracker
    if (userId != null && locationPermissionState.allPermissionsGranted) {
        LocationTracker(
            userId = userId,
            locationPermissionState = locationPermissionState,
            onLocationUpdate = { newLocation ->
                userLocation = newLocation
            }
        )

        LaunchedEffect(userLocation) {
            if (!hasMovedCamera && userLocation != LatLng(44.8176, 20.4569)) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation, 15f)
                hasMovedCamera = true
            }
        }
    }
}



