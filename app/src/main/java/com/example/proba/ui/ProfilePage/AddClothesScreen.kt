package com.example.proba.ui.clothes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.proba.R
import com.example.proba.ui.components.LoginTextField
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Composable
fun AddClothesScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var type by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        imageUri = uri
        cameraBitmap = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        cameraBitmap = bmp
        imageUri = null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dodaj komad odeće", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(150.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .clickable {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                cameraBitmap != null -> {
                    Image(
                        bitmap = cameraBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                imageUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_placeholder),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row {
            Button(onClick = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Galerija") }

            Spacer(Modifier.width(16.dp))

            Button(onClick = { cameraLauncher.launch(null) }) {
                Text("Kamera")
            }
        }

        Spacer(Modifier.height(16.dp))

        LoginTextField(value = type, onValueChange = { type = it }, labelText = "Tip odeće", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LoginTextField(value = size, onValueChange = { size = it }, labelText = "Veličina", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LoginTextField(value = price, onValueChange = { price = it }, labelText = "Cena", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LoginTextField(value = storeName, onValueChange = { storeName = it }, labelText = "Naziv radnje", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LoginTextField(value = description, onValueChange = { description = it }, labelText = "Opis", modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    cameraBitmap != null -> {
                        uploadBitmapAndSave(userId, cameraBitmap!!, type, size, price, storeName, description, context, navController)
                    }
                    imageUri != null -> {
                        uploadUriAndSave(userId, imageUri!!, type, size, price, storeName, description, context, navController)
                    }
                    else -> Toast.makeText(context, "Molimo dodajte sliku", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Dodaj na mapu", color = Color.White)
        }
    }
}

fun uploadUriAndSave(
    userId: String, uri: Uri,
    type: String, size: String, price: String,
    storeName: String, description: String,
    context: Context, navController: NavController
) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imgRef = storageRef.child("clothesImages/${System.currentTimeMillis()}.jpg")

    imgRef.putFile(uri).continueWithTask { task ->
        if (!task.isSuccessful) task.exception?.let { throw it }
        imgRef.downloadUrl
    }.addOnSuccessListener { downloadUri ->
        save(userId, downloadUri, type, size, price, storeName, description, context, navController)
    }
}

fun uploadBitmapAndSave(
    userId: String, bitmap: Bitmap,
    type: String, size: String,
    price: String, storeName: String, description: String,
    context: Context, navController: NavController
) {
    val tempFile = File.createTempFile("cloth", ".jpg", context.cacheDir)
    FileOutputStream(tempFile).use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }
    val uri = Uri.fromFile(tempFile)
    uploadUriAndSave(userId, uri, type, size, price, storeName, description, context, navController)
}

@SuppressLint("MissingPermission")
fun save(
    userId: String,
    downloadUri: Uri,
    type: String,
    size: String,
    price: String,
    storeName: String,
    description: String,
    context: Context,
    navController: NavController
) {
    val fused = LocationServices.getFusedLocationProviderClient(context)
    fused.lastLocation.addOnSuccessListener { location ->
        if (location == null) {
            Toast.makeText(context, "Nema lokacije", Toast.LENGTH_LONG).show()
            return@addOnSuccessListener
        }

        val doc = hashMapOf(
            "authorId" to userId,
            "photoUrl" to downloadUri.toString(),
            "type" to type,
            "size" to size,
            "price" to price,
            "storeName" to storeName,
            "description" to description,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "createdAt" to Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("clothes")
            .add(doc)
            .addOnSuccessListener {
                Toast.makeText(context, "Uspešno dodato!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
