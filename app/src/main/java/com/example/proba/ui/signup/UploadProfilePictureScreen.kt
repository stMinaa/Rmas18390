package com.example.proba.ui.signup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.proba.R
import com.example.proba.Route
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

@Composable
fun UploadProfilePictureScreen(navController: NavController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        imageUri = uri
        cameraImageBitmap = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        cameraImageBitmap = bitmap
        imageUri = null
    }

    // Launcher za permisiju
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch() // sad može da pokrene kameru
        } else {
            Toast.makeText(context, "Dozvola za kameru je odbijena", Toast.LENGTH_SHORT).show()
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
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (cameraImageBitmap != null) {
                Image(
                    bitmap = cameraImageBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            } else {
                val painter: Painter = if (imageUri != null) {
                    rememberAsyncImagePainter(imageUri)
                } else {
                    painterResource(id = R.drawable.ic_placeholder)
                }
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircleButton(
                icon = painterResource(id = R.drawable.ic_folder),
                description = "Upload from device",
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )

            Spacer(modifier = Modifier.width(32.dp))

            CircleButton(
                icon = painterResource(id = R.drawable.ic_camera),
                description = "Take a photo",
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        cameraLauncher.launch()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                when {
                    cameraImageBitmap != null -> {
                        uploadBitmapToFirebase(cameraImageBitmap!!, context) { downloadUrl ->
                            updateUserPhotoUrl(downloadUrl, context) {
                                navController.navigate(Route.HomeScreen().name) {
                                    popUpTo("login_flow") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    imageUri != null -> {
                        uploadImageToFirebase(imageUri!!, context) { downloadUrl ->
                            updateUserPhotoUrl(downloadUrl, context) {
                                navController.navigate(Route.HomeScreen().name) {
                                    popUpTo("login_flow") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    else -> {
                        Toast.makeText(
                            context,
                            "Molimo izaberite ili uslikajte sliku",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Postavi sliku",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = {
            navController.navigate(Route.HomeScreen().name) {
                popUpTo("login_flow") { inclusive = true }
                launchSingleTop = true
            }
        }) {
            Text("Preskoči za sada")
        }
    }
}

fun uploadImageToFirebase(uri: Uri, context: Context, onComplete: (Uri?) -> Unit) {
    val storage = FirebaseStorage.getInstance()
    val fileName = generateUniqueFileName(context, uri)
    val imageRef = storage.reference.child("images/$fileName")

    val tempUri = copyUriToTempFile(context, uri)
    if (tempUri == null) {
        Toast.makeText(context, "Greška pri pripremi fajla", Toast.LENGTH_LONG).show()
        onComplete(null)
        return
    }

    imageRef.putFile(tempUri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri)
            }.addOnFailureListener {
                onComplete(null)
            }
        }
        .addOnFailureListener {
            it.printStackTrace()
            onComplete(null)
        }
}

fun uploadBitmapToFirebase(bitmap: Bitmap, context: Context, onComplete: (Uri?) -> Unit) {
    val fileName = "camera_${System.currentTimeMillis()}.jpg"
    val imageRef = FirebaseStorage.getInstance().reference.child("images/$fileName")

    val tempFile = File.createTempFile("upload_camera", ".jpg", context.cacheDir)
    try {
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Greška pri pripremi slike iz kamere", Toast.LENGTH_LONG).show()
        onComplete(null)
        return
    }

    val tempUri = Uri.fromFile(tempFile)
    imageRef.putFile(tempUri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri)
            }.addOnFailureListener {
                onComplete(null)
            }
        }
        .addOnFailureListener {
            it.printStackTrace()
            onComplete(null)
        }
}

fun updateUserPhotoUrl(photoUrl: Uri?, context: Context, onSuccess: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid != null && photoUrl != null) {
        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .update("profilePhotoUrl", photoUrl.toString())
            .addOnSuccessListener {
                Toast.makeText(context, "Profilna slika sačuvana!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Greška pri čuvanju URL-a slike", Toast.LENGTH_LONG).show()
            }
    } else {
        Toast.makeText(context, "Korisnik nije pronađen ili URL je null", Toast.LENGTH_LONG).show()
    }
}

fun generateUniqueFileName(context: Context, uri: Uri): String {
    var name = UUID.randomUUID().toString()
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                val originalName = it.getString(nameIndex)
                if (!originalName.isNullOrEmpty()) {
                    name = "${System.currentTimeMillis()}_$originalName"
                }
            }
        }
    }
    return name
}

fun copyUriToTempFile(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        Uri.fromFile(tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
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
