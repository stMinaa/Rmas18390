package com.example.proba.ui.profilePage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.proba.R
import com.example.proba.ui.components.PointsManager.calculateRank
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class UserScore(
    val id: String = "",
    val name: String = "",
    val profilePhotoUrl: String? = null,
    val points: Int = 0
)


@Composable
fun LeaderboardScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance().reference
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var users by remember { mutableStateOf(listOf<UserScore>()) }

    LaunchedEffect(true) {
        val snapshot = db.collection("users").get().await()
        val list = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val firstName = doc.getString("firstName") ?: ""
            val lastName = doc.getString("lastName") ?: ""
            val points = doc.getLong("points")?.toInt() ?: 0
            val profileUrl  = doc.getString("profilePhotoUrl")


            UserScore(id, "$firstName $lastName", profileUrl, points)
        }.sortedByDescending { it.points }

        users = list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "RANG LISTA",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .padding(top = 40.dp)
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(users) { user ->
                val isCurrentUser = user.id == currentUserId
                val rank = calculateRank(user.points)

                val rankIcon = when (rank) {
                    "Bronze" -> R.drawable.bronze
                    "Silver" -> R.drawable.silver
                    "Gold" -> R.drawable.gold
                    "Trophy" -> R.drawable.trophy_largest
                    else -> null
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentUser) Color(0xFF373B66) else MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profilna slika
                        val profilePainter = if (!user.profilePhotoUrl.isNullOrEmpty()) {
                            rememberAsyncImagePainter(user.profilePhotoUrl)
                        } else {
                            painterResource(id = R.drawable.ic_boy)
                        }

                        Image(
                            painter = profilePainter,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.width(12.dp))

                        // Ime + poeni
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = user.name,
                                color = Color.White,
                                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${user.points} pts",
                                color = Color(0xFFFFD700),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Ikonica ranga
                        rankIcon?.let {
                            Icon( painter = painterResource(id = it),
                                contentDescription = rank,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp) ) }
                    }
                }
            }
        }
    }

}

