package com.example.proba.ui.clothes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.proba.R
import com.example.proba.ui.components.LoginTextField
import com.example.proba.ui.components.PointsManager
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
    //var size by remember { mutableStateOf("") }
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

    // Launcher za permisiju
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch() // pokreni kameru posle odobrene permisije
        } else {
            Toast.makeText(context, "Dozvola za kameru je odbijena", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dodaj proizvod", style = MaterialTheme.typography.headlineSmall)
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

            Button(onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    cameraLauncher.launch()
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("Kamera")
            }
        }

        Spacer(Modifier.height(16.dp))

        LoginTextField(value = type, onValueChange = { type = it }, labelText = "Tip proizvoda", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        //LoginTextField(value = size, onValueChange = { size = it }, labelText = "Veličina", modifier = Modifier.fillMaxWidth())
        //Spacer(Modifier.height(8.dp))
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
                        uploadBitmapAndSave(userId, cameraBitmap!!, type, price, storeName, description, context, navController)
                    }
                    imageUri != null -> {
                        uploadUriAndSave(userId, imageUri!!, type, price, storeName, description, context, navController)
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
    type: String,
    price: String, storeName: String, description: String,
    context: Context, navController: NavController
) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imgRef = storageRef.child("clothesImages/${System.currentTimeMillis()}.jpg")

    imgRef.putFile(uri).continueWithTask { task ->
        if (!task.isSuccessful) task.exception?.let { throw it }
        imgRef.downloadUrl
    }.addOnSuccessListener { downloadUri ->
        save(
            userId, downloadUri, type,
            "dostupno", // status je sada uvek "dostupno"
            price, storeName, description,
            context, navController
        )
    }
}

fun uploadBitmapAndSave(
    userId: String, bitmap: Bitmap,
    type: String,
    price: String, storeName: String, description: String,
    context: Context, navController: NavController
) {
    val tempFile = File.createTempFile("cloth", ".jpg", context.cacheDir)
    FileOutputStream(tempFile).use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }
    val uri = Uri.fromFile(tempFile)
    uploadUriAndSave(userId, uri, type, price, storeName, description, context, navController)
}

@SuppressLint("MissingPermission")
fun save(
    userId: String,
    downloadUri: Uri,
    type: String,
    //size: String,
    status: String,
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
            //"size" to size,
            "price" to price,
            "storeName" to storeName,
            "description" to description,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "createdAt" to Timestamp.now(),
            "status" to status,
            "comments" to listOf<Map<String, Any>>(),
            "ratings" to listOf<Map<String, Any>>()
        )

        FirebaseFirestore.getInstance().collection("clothes")
            .add(doc)
            .addOnSuccessListener { docRef ->  // ovde promenjeno: documentReference -> docRef
                Toast.makeText(context, "Uspešno dodato!", Toast.LENGTH_SHORT).show()
                PointsManager.addPoints(userId, 10) {
                    Toast.makeText(context, "Dobio si 10 poena!", Toast.LENGTH_SHORT).show()
                }

                // BONUS POENI za kvalitet opisa
                val descriptionLengthThreshold = 30 // minimum 20 karaktera da bi dobio bonus
                if (description.trim().length >= descriptionLengthThreshold) {
                    PointsManager.addPoints(userId, 3) {
                        Toast.makeText(context, "Bonus 3 poena za kvalitetan opis!", Toast.LENGTH_SHORT).show()
                    }
                }

                docRef.collection("comments")
                docRef.collection("ratings")

                navController.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show()
            }

    }
}
