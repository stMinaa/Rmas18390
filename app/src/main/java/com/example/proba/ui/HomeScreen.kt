package com.example.proba.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.proba.R
import com.example.proba.service.LocationService
import com.example.proba.service.ServiceStateManager
import com.example.proba.ui.components.LocationTracker
import com.example.proba.ui.components.PointsManager
import com.example.proba.ui.components.createCustomMarker
import com.example.proba.ui.theme.Pink60
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.tasks.await


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission", "RememberReturnType")
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

    // Table/Map toggle
    var tableMode by rememberSaveable { mutableStateOf(false) }

    // Marker highlight
    var highlightedMarker by remember { mutableStateOf<String?>(null) }

    // Sirovi podaci i filtrirani prikaz
    val clothesList = remember { mutableStateListOf<Map<String, Any>>() }
    var filteredClothes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

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
        snapshotFlow { activity?.intent?.getStringExtra("clothesId") }
            .collect { id ->
                id?.let {
                    navController.navigate("ClothesDetail/$it") { launchSingleTop = true }
                    activity?.intent?.removeExtra("clothesId")
                }
            }
    }

    LaunchedEffect(activity) {
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
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(LatLng(lat, lon), 17f)
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
                // inicijalno bez filtera
                filteredClothes = clothesList.toList()
            }
    }

    // Sheet state
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    // === PROMENLJIVE ZA UI FILTERA ===
    var localQuery by rememberSaveable { mutableStateOf("") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState()
    val endPickerState = rememberDatePickerState()
    var startDateText by remember { mutableStateOf<String?>(null) }
    var endDateText by remember { mutableStateOf<String?>(null) }
    var localRadius by rememberSaveable { mutableStateOf(0f) }

    val sdfDisplay = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfFilter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var localAuthorId by rememberSaveable { mutableStateOf("") } // novi filter po autoru

    // === FILTER LOGIKA ===
    fun applyFilters() {
        val startDate = startDateText?.let { runCatching { sdfFilter.parse(it) }.getOrNull() }
        val endDate = endDateText?.let { runCatching { sdfFilter.parse(it) }.getOrNull() }

        filteredClothes = clothesList.filter { data ->
            // 1) Tekstualni filter (type/description/storeName)
            val query = localQuery.trim().lowercase()
            val type = data["type"]?.toString().orEmpty().lowercase()
            val desc = data["description"]?.toString().orEmpty().lowercase()
            val store = data["storeName"]?.toString().orEmpty().lowercase()
            val queryMatch = query.isBlank() || type.contains(query) || desc.contains(query) || store.contains(query)

            // 2) Datum OD/DO (koristi se polje createdAt kao Timestamp)
            val createdAtDate = (data["createdAt"] as? Timestamp)?.toDate()
            val dateMatch =
                (startDate == null || (createdAtDate != null && !createdAtDate.before(startDate))) &&
                        (endDate == null || (createdAtDate != null && !createdAtDate.after(endDate)))

            // 3) Radijus od trenutne lokacije (km)
            val radiusMatch = if (localRadius > 0f) {
                val lat = (data["latitude"] as? Double) ?: return@filter false
                val lon = (data["longitude"] as? Double) ?: return@filter false
                val distance = FloatArray(1)
                android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    lat, lon,
                    distance
                )
                distance[0] <= localRadius //* 1000 // km -> m

            } else true

            val authorMatch = if (localAuthorId.isNotEmpty()) {
                data["authorId"] == localAuthorId
            } else true

            queryMatch && dateMatch && radiusMatch && authorMatch
        }
    }

    // Reaguj na promene: lokacije (za radijus), teksta, datuma i izvornog niza
    LaunchedEffect(userLocation, localRadius, localQuery, startDateText, endDateText, clothesList) {
        applyFilters()
    }


    // === DEO ZA AUTORE (PASTE OVDE, ZAMENI STARI BLOK) ===
    val authorsMap = remember { mutableStateMapOf<String, String>() } // authorId -> display name (stateful map)

// Observe changes to clothesList contents and fetch only needed user docs.
// snapshotFlow emituje svaki put kad se lista (sadržaj) promeni.
    LaunchedEffect(Unit) {
        snapshotFlow { clothesList.mapNotNull { it["authorId"]?.toString() }.distinct() }
            .collect { ids ->
                val firestore = FirebaseFirestore.getInstance()
                // fetch missing authors
                ids.forEach { authorId ->
                    if (!authorsMap.containsKey(authorId)) {
                        try {
                            val doc = firestore.collection("users").document(authorId).get().await()
                            if (doc.exists()) {
                                val firstName = doc.getString("firstName").orEmpty()
                                val lastName = doc.getString("lastName").orEmpty()
                                val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
                                authorsMap[authorId] = if (fullName.isBlank()) "Nepoznat" else fullName
                            } else {
                                authorsMap[authorId] = "Nepoznat"
                            }
                        } catch (e: Exception) {
                            // ako fetch pukne (npr. pravila), stavimo fallback
                            authorsMap[authorId] = "Nepoznat"
                        }
                    }
                }
                // ukloni autore koji vise nisu aktivni (opciono, ali držimo map "čistom")
                val toRemove = authorsMap.keys - ids.toSet()
                toRemove.forEach { authorsMap.remove(it) }
            }
    }

// derived list for dropdown (sorted by display name)
    val authorsList by remember {
        derivedStateOf {
            authorsMap.map { it.key to it.value }.sortedBy { it.second }
        }
    }

// ensure filters re-run also when author selection changes (automatic filtering)
    LaunchedEffect(userLocation, localRadius, localQuery, startDateText, endDateText, clothesList, localAuthorId) {
        applyFilters()
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
                            if (isChecked) startLocationService() else stopLocationService()
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
                            contentDescription = "Rang lista"
                        )
                    },
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
                    title = {
                        Text(
                            if (tableMode) "Tabela objekata" else "Mapa i Lokacija",
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Meni",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {

                        // Otvaranje filter sheet-a
                        IconButton(onClick = { showSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Filteri",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        // Toggle Table/Map
                        IconButton(onClick = { tableMode = !tableMode }) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = if (tableMode) "Prikaži mapu" else "Prikaži tabelu",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
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
                    .padding(paddingValues)
            ) {
                // === MAPA ili TABELA ===
                if (!tableMode) {
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

                        // marker-i odeće (FILTRIRANI)
                        filteredClothes.forEach { data ->
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
                                    val distance = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        userLocation.latitude, userLocation.longitude,
                                        lat, lon,
                                        distance
                                    )

                                    if (userId != null && distance[0] <= 20f) {
                                        PointsManager.addPoints(userId, 1)
                                        Toast.makeText(context, "Dobili ste poen +1.", Toast.LENGTH_SHORT).show()
                                    } else if (distance[0] > 20f) {
                                        Toast.makeText(
                                            context,
                                            "Morate biti bliže markeru da biste dobili poen",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    navController.navigate("ClothesDetail/$id")
                                    true
                                }
                            )
                        }
                    }
                } else {
                    ClothesTable(
                        items = filteredClothes,
                        onRowClick = { id ->
                            navController.navigate("ClothesDetail/$id")
                        }
                    )
                }

                // Dozvole za lokaciju
                if (!locationPermissionState.allPermissionsGranted && !tableMode) {
                    Button(
                        onClick = { locationPermissionState.launchMultiplePermissionRequest() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text("Dozvoli lokaciju")
                    }
                }

                // FAB za dodavanje
                FloatingActionButton(
                    onClick = { navController.navigate("AddClothes") },
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

                // FAB za centriranje – samo kad je mapa aktivna
                if (!tableMode) {
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

            // ======== FILTER SHEET ========
            if (showSheet) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { showSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Filteri", style = MaterialTheme.typography.headlineSmall)

                        // 1) Autor
                        var expandedAuthor by remember { mutableStateOf(false) }
                        Column {
                            Text("Autor")
                            ExposedDropdownMenuBox(
                                expanded = expandedAuthor,
                                onExpandedChange = { expandedAuthor = !expandedAuthor }
                            ) {
                                val authorDisplay = when {
                                    localAuthorId.isEmpty() -> "Svi autori"
                                    authorsMap.containsKey(localAuthorId) -> authorsMap[localAuthorId]!!
                                    else -> "Učitavanje..."
                                }

                                OutlinedTextField(
                                    value = authorDisplay,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Izaberi autora") },
                                    modifier = Modifier.menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedAuthor,
                                    onDismissRequest = { expandedAuthor = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Svi autori") },
                                        onClick = {
                                            localAuthorId = ""
                                            expandedAuthor = false
                                        }
                                    )
                                    // prikazujemo samo autore koji imaju oglas
                                    authorsList.forEach { (id, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                localAuthorId = id
                                                expandedAuthor = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2) Tekstualna pretraga
                        OutlinedTextField(
                            value = localQuery,
                            onValueChange = { localQuery = it },
                            label = { Text("Pretraga (tip, opis, radnja…)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3) Period OD/DO
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Period")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { showStartPicker = true }) {
                                    Text(startDateText ?: "Datum OD")
                                }
                                OutlinedButton(onClick = { showEndPicker = true }) {
                                    Text(endDateText ?: "Datum DO")
                                }
                            }
                        }

                        // 4) Radijus (m)
                        Column {
                            Text("Radijus (m)")
                            Slider(
                                value = localRadius,
                                onValueChange = { localRadius = it },
                                valueRange = 0f..2000f,
                                steps = 39,
                                onValueChangeFinished = {
                                    localRadius = (localRadius / 50).roundToInt() * 50f
                                }
                            )
                            Text("${"%.1f".format(localRadius)} m")
                        }

                        // Dugmad Reset i Primeni
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                localQuery = ""
                                startDateText = null
                                endDateText = null
                                localRadius = 0f
                                localAuthorId = ""
                                applyFilters()
                            }) { Text("Reset") }

                            Button(onClick = {
                                applyFilters()
                                showSheet = false
                            }) {
                                Text("Primeni")
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }



            // DatePicker dijalozi
            if (showStartPicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = startPickerState.selectedDateMillis
                            startDateText = millis?.let {
                                sdfDisplay.format(it)
                            }
                            showStartPicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartPicker = false }) { Text("Otkaži") }
                    }
                ) {
                    DatePicker(state = startPickerState)
                }
            }
            if (showEndPicker) {
                DatePickerDialog(
                    onDismissRequest = { showEndPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = endPickerState.selectedDateMillis
                            endDateText = millis?.let {
                                sdfDisplay.format(it)
                            }
                            showEndPicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndPicker = false }) { Text("Otkaži") }
                    }
                ) {
                    DatePicker(state = endPickerState)
                }
            }
            // ======== KRAJ SHEET-A ========
        }
    }

    // Lokacijski tracker (zadržano)
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

/** ---------- Tabela (isto, samo koristi filtrirane podatke) ---------- **/
@Composable
private fun ClothesTable(
    items: List<Map<String, Any>>,
    onRowClick: (id: String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Slika", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                Text("Tip", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                Text("Veličina", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                Text("Cena", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                Text("Radnja", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                Text("Status", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                Text("Kreirano", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))

            items.forEach { data ->
                val id = data["id"]?.toString().orEmpty()
                val type = data["type"]?.toString().orEmpty()
                val size = data["size"]?.toString().orEmpty()
                val price = data["price"]?.toString().orEmpty()
                val storeName = data["storeName"]?.toString().orEmpty()
                val status = data["status"]?.toString().orEmpty()
                val createdAt = (data["createdAt"] as? Timestamp)?.toDate()
                val createdAtText = createdAt?.let { sdf.format(it) } ?: "-"
                val imageUrl = data["photoUrl"]?.toString() ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = id.isNotEmpty()) { onRowClick(id) }
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Slika odeće",
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .background(Color.LightGray)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nema slike", color = Color.DarkGray)
                        }
                    }

                    Text(type, modifier = Modifier.width(100.dp))
                    Text(size, modifier = Modifier.width(80.dp))
                    Text(price, modifier = Modifier.width(80.dp))
                    Text(storeName, modifier = Modifier.width(120.dp))
                    Text(status, modifier = Modifier.width(100.dp))
                    Text(createdAtText, modifier = Modifier.width(120.dp))
                }
            }
        }
    }
}
