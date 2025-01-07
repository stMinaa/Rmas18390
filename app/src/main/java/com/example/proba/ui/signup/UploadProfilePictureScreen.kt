package com.example.proba.ui.signup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.proba.Route
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.proba.ui.theme.ProbaTheme
import com.google.firebase.storage.FirebaseStorage

@Composable
fun UploadProfilePictureScreen(navController: NavController) {
    val context = LocalContext.current
    val storageReference = FirebaseStorage.getInstance().reference
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // States for image selection
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val profileImageRef = storageReference.child("profile_pictures/${currentUser?.uid}.jpg")
            val uploadTask = profileImageRef.putFile(uri)

            uploadTask.addOnSuccessListener {
                Toast.makeText(context, "Profile picture uploaded successfully!", Toast.LENGTH_SHORT).show()
                navController.navigate(Route.HomeScreen().name) {
                    popUpTo("upload_profile_picture") { inclusive = true }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to upload picture: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Upload Profile Picture",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Select and Upload Picture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            // Skip uploading profile picture
            navController.navigate(Route.HomeScreen().name) {
                popUpTo("upload_profile_picture") { inclusive = true }
            }
        }) {
            Text("Skip for now")
        }
    }
}


@Preview(showSystemUi = true)
@Composable
fun PrevScreen() {
    ProbaTheme {
        UploadProfilePictureScreen(navController = NavController(LocalContext.current))
    }
}