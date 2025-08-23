package com.example.proba.ui.profilePage

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class CommentItem(
    val authorName: String,
    val comment: String,
    val timestamp: Date?
)

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun ClothesDetailScreen(navController: NavController, clothId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var authorId by remember { mutableStateOf<String?>(null) }

    // Ocene i komentari
    var avgRating by remember { mutableStateOf(0.0) }
    var ratingCount by remember { mutableStateOf(0) }
    var showRateDialog by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf(listOf<CommentItem>()) }

    // Permissions
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    // Učitavanje podataka artikla
    LaunchedEffect(clothId) {
        try {
            val doc = db.collection("clothes").document(clothId).get().await()
            if (doc.exists()) {
                val data = doc.data!!
                clothData = data
                type = data["type"]?.toString() ?: ""
                size = data["size"]?.toString() ?: ""
                storeName = data["storeName"]?.toString() ?: ""
                price = data["price"]?.toString() ?: ""
                description = data["description"]?.toString() ?: ""
                photoUrl = data["photoUrl"]?.toString()
                authorId = data["authorId"]?.toString()
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    // Listener za ocene
    DisposableEffect(clothId) {
        val ratingsRef = db.collection("clothes").document(clothId).collection("ratings")
        val listener = ratingsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val ratings = snapshot.documents.mapNotNull { it.getLong("rating")?.toInt() }
                ratingCount = ratings.size
                avgRating = if (ratings.isNotEmpty()) ratings.average() else 0.0
            }
        }
        onDispose { listener.remove() }
    }

    // Listener za komentare
    DisposableEffect(clothId) {
        val commentsRef = db.collection("clothes").document(clothId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = commentsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val tasks = snapshot.documents.mapNotNull { doc ->
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    val comment = doc.getString("comment") ?: return@mapNotNull null
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()

                    db.collection("users").document(userId).get()
                        .continueWith { task ->
                            val userDoc = task.result
                            val firstName = userDoc?.getString("firstName") ?: ""
                            val lastName = userDoc?.getString("lastName") ?: ""
                            val authorName = "$firstName $lastName".trim()

                            CommentItem(
                                authorName = if (authorName.isNotBlank()) authorName else "Nepoznat korisnik",
                                comment = comment,
                                timestamp = timestamp
                            )
                        }
                }

                Tasks.whenAllSuccess<CommentItem>(tasks)
                    .addOnSuccessListener { loadedComments ->
                        commentsList = loadedComments
                    }
            } else {
                commentsList = emptyList()
            }
        }
        onDispose { listener.remove() }
    }

    suspend fun saveChanges() {
        try {
            isSaving = true
            db.collection("clothes")
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

    fun submitRatingAndComment(rating: Int, comment: String) {
        val raterId = currentUserId ?: return
        val clothRef = db.collection("clothes").document(clothId)

        // Save rating
        clothRef.collection("ratings").document(raterId).set(
            mapOf(
                "userId" to raterId,
                "rating" to rating,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener {
            // Poeni za osobu koja oceni
            PointsManager.addPoints(raterId, 1) {
                Toast.makeText(context, "Hvala na ocenjivanju! Dobijate +1 poen.", Toast.LENGTH_SHORT).show()
            }
            // Poeni za autora na osnovu ocene
            authorId?.let { author ->
                if (rating in 3..5) {
                    PointsManager.addPoints(author, rating) {}
                }
            }
        }

        // Save comment
        if (comment.isNotBlank()) {
            db.collection("users").document(raterId).get()
                .addOnSuccessListener { userDoc ->
                    val fullName = if (userDoc.exists()) {
                        val firstName = userDoc.getString("firstName") ?: ""
                        val lastName = userDoc.getString("lastName") ?: ""
                        "$firstName $lastName".trim()
                    } else {
                        "Nepoznat korisnik"
                    }

                    clothRef.collection("comments").add(
                        mapOf(
                            "userId" to raterId,
                            "userName" to fullName,
                            "comment" to comment,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    )
                }
        }
    }

    fun handleObisaoSam() {
        val userId = currentUserId ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val visitDoc = db.collection("clothes").document(clothId)
            .collection("visits").document(userId)

        visitDoc.get().addOnSuccessListener { doc ->
            val lastVisit = doc.getString("date")
            if (lastVisit == today) {
                Toast.makeText(context, "Već si danas označio/la ovaj komad!", Toast.LENGTH_SHORT).show()
            } else {
                visitDoc.set(mapOf("date" to today))
                PointsManager.addPoints(userId, 1) {}
                Toast.makeText(context, "Dodato +1 poen!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Učitavam...")
        }

        clothData != null -> Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Slika
                item {
                    photoUrl?.let { url ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Slika odeće",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(90.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .clickable { isFullscreen = true }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Prosečna ocena + dugme Oceni
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format("%.1f", avgRating), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Star, contentDescription = "Prosečna ocena", tint = Color(0xFFFFD700))
                            Spacer(Modifier.width(8.dp))
                            Text("($ratingCount ocena)")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val isOwner = authorId == currentUserId
                        Button(
                            onClick = {
                                if (isOwner) {
                                    Toast.makeText(context, "Ne možete oceniti svoj proizvod", Toast.LENGTH_SHORT).show()
                                } else {
                                    showRateDialog = true
                                }
                            },
                            enabled = !isOwner
                        ) {
                            Text("Oceni")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Polja Tip, Veličina, Prodavnica, Cena, Opis
                item {
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Tip") }, enabled = isEditing, modifier = Modifier.fillMaxWidth(), colors = fieldColors(isEditing))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("Veličina") }, enabled = isEditing, modifier = Modifier.fillMaxWidth(), colors = fieldColors(isEditing))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Prodavnica") }, enabled = isEditing, modifier = Modifier.fillMaxWidth(), colors = fieldColors(isEditing))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Cena") }, enabled = isEditing, modifier = Modifier.fillMaxWidth(), colors = fieldColors(isEditing))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis") }, enabled = isEditing, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 5, colors = fieldColors(isEditing))
                    Spacer(Modifier.height(12.dp))
                }

                // Dugme Obišao/la sam
                item {
                    Button(onClick = { handleObisaoSam() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Obišao/la sam", color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Naslov komentara
                item {
                    Text("Komentari:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }

                // Lista komentara
                items(commentsList) { commentItem ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = commentItem.authorName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            commentItem.timestamp?.let {
                                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                Text(sdf.format(it), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                            }
                        }
                        Text(commentItem.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                    Divider()
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Dijalog za ocenjivanje
            if (showRateDialog && authorId != currentUserId) {
                var tempRating by remember { mutableStateOf(0) }
                var tempComment by remember { mutableStateOf("") }

                Dialog(onDismissRequest = { showRateDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Oceni ovaj komad", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            Row {
                                (1..5).forEach { star ->
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clickable { tempRating = star },
                                        tint = if (star <= tempRating) Color(0xFFFFD700) else Color.Gray
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = tempComment, onValueChange = { tempComment = it }, label = { Text("Tvoj komentar") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                submitRatingAndComment(tempRating, tempComment)
                                showRateDialog = false
                            }) { Text("Pošalji") }
                        }
                    }
                }
            }

            // Dugme za edit (pokretanje editovanja) – samo vlasnik
            if (authorId == currentUserId && !isEditing) {
                IconButton(
                    onClick = { isEditing = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Uredi",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Dugme za Save dok je u edit mode (mali check)
            if (isEditing) {
                IconButton(
                    onClick = {
                        scope.launch {
                            saveChanges()
                            isEditing = false
                            Toast.makeText(context, "Izmene sačuvane!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sačuvaj",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Fullscreen image
            if (isFullscreen && photoUrl != null) {
                Dialog(onDismissRequest = { isFullscreen = false }) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        AsyncImage(model = photoUrl, contentDescription = "Fullscreen Slika", modifier = Modifier.fillMaxSize().align(Alignment.Center), contentScale = ContentScale.Fit)
                        IconButton(onClick = { isFullscreen = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Zatvori fullscreen", tint = Color.White)
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
