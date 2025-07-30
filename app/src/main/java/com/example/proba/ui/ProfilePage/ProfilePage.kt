package com.example.proba.ui.ProfilePage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.rememberAsyncImagePainter
import com.example.proba.R
import com.example.proba.ui.login.LoginScreen
import com.example.proba.ui.theme.ProbaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    // Preuzimanje korisničkih podataka
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
            modifier=Modifier
                .fillMaxWidth()
        ){

           val(topImg,profile)=createRefs()
            Image(painterResource(id = R.drawable.top_background23),null,
               modifier= Modifier
                    .fillMaxWidth()
                    .constrainAs(topImg) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                contentScale = ContentScale.Crop)

            // Profilna slika
            val imageUrl = userData?.get("profilePhotoUrl") as? String
            val profilePainter = if (!imageUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(imageUrl)
            } else {
                painterResource(id = R.drawable.ic_boy)
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
            // Prikaz indikatora učitavanja
            Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
        } else {
            userData?.let { data ->
                // Prikaz korisničkih podataka
                Text(
                    text = "${data["firstName"] ?: "N/A"} ${data["lastName"] ?: "N/A"}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
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
            } ?: run {
                // Ako nema podataka
                Text(text = "No user data available", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}



@Preview(showSystemUi = true)
@Composable
fun PrevProfPage() {
    ProbaTheme {
ProfilePage(navController = NavController(LocalContext.current))
    }
}