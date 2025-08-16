package com.example.proba.ui.clothes

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.window.Dialog

@Composable
fun ClothesDetailScreen(navController: NavController, clothId: String) {
    var clothData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isAuthor by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var isFullscreen by remember { mutableStateOf(false) }

    var type by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

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
                photoUrl = data["photoUrl"]?.toString()

                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val authorId = data["authorId"]?.toString()
                isAuthor = currentUserId != null && currentUserId == authorId
            }
        } catch (_: Exception) { }
        isLoading = false
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
        } catch (_: Exception) { }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
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
                                .clickable {
                                    isFullscreen = true // OTVARA FULLSCREEN
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = type,
                        shape = RoundedCornerShape(30),
                        onValueChange = { type = it },
                        label = { Text("Tip") },
                        enabled = isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = size,
                        shape = RoundedCornerShape(30),
                        onValueChange = { size = it },
                        label = { Text("Veličina") },
                        enabled = isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = storeName,
                        shape = RoundedCornerShape(30),
                        onValueChange = { storeName = it },
                        label = { Text("Prodavnica") },
                        enabled = isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = price,
                        shape = RoundedCornerShape(30),
                        onValueChange = { price = it },
                        label = { Text("Cena") },
                        enabled = isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(isEditing)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        shape = RoundedCornerShape(30),
                        onValueChange = { description = it },
                        label = { Text("Opis") },
                        enabled = isEditing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5,
                        colors = fieldColors(isEditing)
                    )
                }

                if (isAuthor) {
                    FloatingActionButton(
                        onClick = {
                            if (isEditing) {
                                scope.launch {
                                    saveChanges()
                                    isEditing = false
                                }
                            } else {
                                isEditing = true
                            }
                        },
                        shape = CircleShape,
                        containerColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 40.dp, bottom = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isEditing) "Sačuvaj" else "Uredi",
                            tint = Color.White
                        )
                    }
                }

                // FULLSCREEN DIALOG
                if (isFullscreen && photoUrl != null) {
                    Dialog(onDismissRequest = { isFullscreen = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                        ) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Fullscreen Slika",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .align(Alignment.Center),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { isFullscreen = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Zatvori fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Odeća nije pronađena")
            }
        }
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
