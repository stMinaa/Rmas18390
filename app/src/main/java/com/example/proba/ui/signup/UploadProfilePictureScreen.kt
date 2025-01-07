package com.example.proba.ui.signup

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.proba.R
import com.example.proba.Route
import com.example.proba.ui.theme.ProbaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*


@Composable
fun UploadProfilePictureScreen(navController: NavController) {
    Spacer(modifier = Modifier.height(60.dp))

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

        // Always show the buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Upload from device button
            CircleButton(
                icon = painterResource(id = R.drawable.ic_folder),
                description = "Upload from device",
                onClick = {  }
            )

            Spacer(modifier = Modifier.width(32.dp))

            // Take a photo (trigger camera permission check only on click)
            CircleButton(
                icon = painterResource(id = R.drawable.ic_camera),
                description = "Take a photo",
                onClick = {}
            )
        }

        // Labels under the buttons
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start=15.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Upload",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.width(56.dp))

            Text(
                text = "Take a photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Skip button
        TextButton(onClick = {
            navController.navigate(Route.HomeScreen().name) {
                popUpTo("upload_profile_picture") { inclusive = true }
            }
        }) {
            Text("Skip for now")
        }
    }
}

@Composable
fun CircleButton(icon: Painter, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            .padding(16.dp)
    ) {
        Image(
            painter = icon,
            contentDescription = description,
            modifier = Modifier.size(40.dp)
        )
    }
}



@Preview(showSystemUi = true)
@Composable
fun PreviewUploadProfilePictureScreen() {
    ProbaTheme {
        UploadProfilePictureScreen(navController = NavController(LocalContext.current))
    }
}

