package com.example.proba.ui.profilePage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.proba.R
import com.example.proba.ui.theme.ProbaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfilePage(navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    val (userData, setUserData) = remember { mutableStateOf<Map<String, Any>?>(null) }
    val (isLoading, setLoading) = remember { mutableStateOf(true) }

    // preuzimanje korisničkih podataka
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        setUserData(document.data)
                    } else {
                        setUserData(null)
                    }
                    setLoading(false)
                }
                .addOnFailureListener {
                    setUserData(null)
                    setLoading(false)
                }
        } else {
            setLoading(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ConstraintLayout(
            modifier = Modifier.fillMaxWidth()
        ) {

            val (topImg, profile) = createRefs()
            Image(
                painterResource(id = R.drawable.top_background23),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(topImg) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                contentScale = ContentScale.Crop
            )

            //profilna slika
            val imageUrl = userData?.get("profilePhotoUrl") as? String
            val profilePainter = if (!imageUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(imageUrl)
            } else {
                painterResource(id = R.drawable.ic_placeholder)
            }

            Image(
                painter = profilePainter,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .constrainAs(profile) {
                        bottom.linkTo(topImg.bottom, margin = -60.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (isLoading) {
            Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
        } else {
            userData?.let { data ->

                Text(
                    text = "${data["firstName"] ?: "N/A"} ${data["lastName"] ?: "N/A"}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = data["email"]?.toString() ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Phone: ${data["phone"] ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign-up Date: ${
                        (data["signupDate"] as? com.google.firebase.Timestamp)?.toDate()?.let {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                        } ?: "N/A"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))


                MyProductsSection(userId)
            } ?: run {
                Text(text = "No user data available", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun MyProductsSection(userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val myClothes = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // učitavanje odeće koje je korisnik dodao
    LaunchedEffect(userId) {
        try {
            val snapshot = firestore.collection("clothes")
                .whereEqualTo("authorId", userId)
                .get()
                .await()

            myClothes.value = snapshot.documents.mapNotNull { it.data }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Moji proizvodi",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (myClothes.value.isEmpty()) {
            Text(
                text = "Još niste dodali proizvode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            myClothes.value.forEach { cloth ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        val imageUrl = cloth["photoUrl"] as? String
                        if (!imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Odeća",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("N/A", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = cloth["type"]?.toString() ?: "Tip",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Cena: ${cloth["price"] ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}



