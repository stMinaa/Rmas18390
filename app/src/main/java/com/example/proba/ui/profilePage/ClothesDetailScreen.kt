package com.example.proba.ui.profilePage

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.proba.ui.components.PointsManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun ClothesDetailScreen(navController: NavController, clothId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var clothData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    var type by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Označi kupovinu") }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    // Učitavanje podataka
    LaunchedEffect(clothId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("clothes")
                .document(clothId)
                .get()
                .await()

            if (doc.exists()) {
                val data = doc.data!!
                clothData = data
                type = data["type"]?.toString() ?: ""
                size = data["size"]?.toString() ?: ""
                storeName = data["storeName"]?.toString() ?: ""
                price = data["price"]?.toString() ?: ""
                description = data["description"]?.toString() ?: ""
                status = data["status"]?.toString() ?: "Označi kupovinu"
                photoUrl = data["photoUrl"]?.toString()
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    fun checkDistanceAndChangeStatus(latitude: Double, longitude: Double) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val distance = FloatArray(1)
                android.location.Location.distanceBetween(
                    location.latitude, location.longitude,
                    latitude, longitude,
                    distance
                )
                if (distance[0] <= 50) {
                    if (status != "Prodato") {
                        status = "Prodato"
                        val firestore = FirebaseFirestore.getInstance()

                        // 1️⃣ Update status odeće
                        firestore.collection("clothes")
                            .document(clothId)
                            .set(mapOf("status" to status), SetOptions.merge())

                        // 2️⃣ Dodaj odeću u korisnikov array purchasedClothes
                        currentUserId?.let { userId ->
                            firestore.collection("users")
                                .document(userId)
                                .update("purchasedClothes", FieldValue.arrayUnion(clothId))
                        }

                        // 3️⃣ Dodavanje poena
                        currentUserId?.let { PointsManager.addPoints(it, 5) {} }

                        Toast.makeText(context, "Označeno kao prodato! +5 poena", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Moraš biti bliže lokaciji da označiš kupovinu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    suspend fun saveChanges() {
        try {
            isSaving = true
            FirebaseFirestore.getInstance()
                .collection("clothes")
                .document(clothId)
                .update(
                    mapOf(
                        "type" to type,
                        "size" to size,
                        "storeName" to storeName,
                        "price" to price,
                        "description" to description
                    )
                ).await()
        } catch (_: Exception) {}
        isSaving = false
    }

    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Učitavam...")
            }
        }
        clothData != null -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    photoUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Slika odeće",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(90.dp))
                                .align(Alignment.CenterHorizontally)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .clickable { isFullscreen = true }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(type, { type = it }, label = { Text("Tip") },
                        enabled = isEditing, modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(size, { size = it }, label = { Text("Veličina") },
                        enabled = isEditing, modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(storeName, { storeName = it }, label = { Text("Prodavnica") },
                        enabled = isEditing, modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(price, { price = it }, label = { Text("Cena") },
                        enabled = isEditing, modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(description, { description = it }, label = { Text("Opis") },
                        enabled = isEditing, modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5, colors = fieldColors(isEditing)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Dugme za "Prodato"
                    Button(
                        onClick = {
                            val lat = clothData?.get("latitude") as? Double ?: return@Button
                            val lon = clothData?.get("longitude") as? Double ?: return@Button
                            checkDistanceAndChangeStatus(lat, lon)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(status, color = Color.White)
                    }
                }

                if (isEditing) {
                    FloatingActionButton(
                        onClick = { scope.launch { saveChanges(); isEditing = false } },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Sačuvaj", tint = Color.White)
                    }
                }

                if (isFullscreen && photoUrl != null) {
                    Dialog(onDismissRequest = { isFullscreen = false }) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                            AsyncImage(model = photoUrl, contentDescription = "Fullscreen Slika",
                                modifier = Modifier.fillMaxSize().align(Alignment.Center),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { isFullscreen = false },
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Zatvori fullscreen", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Odeća nije pronađena") }
    }
}

@Composable
private fun fieldColors(isEditing: Boolean) = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = MaterialTheme.colorScheme.background,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.White,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.primary,
    disabledLabelColor = Color.Gray,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
    disabledIndicatorColor = Color.White
)
