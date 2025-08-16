package com.example.proba.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val firestore = FirebaseFirestore.getInstance()

    private var clothesListener: ListenerRegistration? = null

    private val notifiedItems = mutableMapOf<String, Long>()
    private val NOTIFICATION_COOLDOWN_MS = 5 * 60 * 1000L // 5 minuta cooldown

    private var lastKnownLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        NotificationUtils.createNotificationChannel(this)
        startForeground(1, NotificationUtils.getNotification(this, "Praćenje lokacije aktivno"))

        startLocationUpdates()
        startClothesListener()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30_000L
        ).setMinUpdateIntervalMillis(30_000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    lastKnownLocation = location
                    saveLocation(location)
                    Log.d("LocationService", "Lokacija: ${location.latitude}, ${location.longitude}")
                    // Ne zovemo checkNearbyItems jer koristimo snapshot listener
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } else {
            Log.e("LocationService", "Lokacijska dozvola NIJE odobrena")
            stopSelf()
        }
    }

    private fun startClothesListener() {
        clothesListener?.remove() // ukloni prethodni ako postoji

        clothesListener = firestore.collection("clothes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("LocationService", "Greška u snapshot listeneru: ", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && lastKnownLocation != null) {
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                    // Ovde ide cela kolekcija, ne samo promene
                    for (doc in snapshots.documents) {
                        val lat = doc.getDouble("latitude") ?: continue
                        val lng = doc.getDouble("longitude") ?: continue
                        val id = doc.id
                        val type = doc.getString("type") ?: "Artikal u blizini"
                        val description = doc.getString("description") ?: ""
                        val storeName = doc.getString("storeName") ?: "Radnja"
                        val price = doc.get("price")?.toString() ?: ""
                        val size = doc.getString("size") ?: ""
                        val authorId = doc.getString("authorId")

                        // preskoči ako je isti korisnik autor
                        if (authorId != null && authorId == currentUserId) {
                            continue
                        }

                        val itemLocation = Location("").apply {
                            latitude = lat
                            longitude = lng
                        }

                        if (isNearby(lastKnownLocation!!, itemLocation, 500f)) {
                            val lastNotifyTime = notifiedItems[id] ?: 0L
                            if (System.currentTimeMillis() - lastNotifyTime > NOTIFICATION_COOLDOWN_MS) {
                                notifiedItems[id] = System.currentTimeMillis()

                                val body = if (description.isNotBlank()) {
                                    "$type ($size) — $description\nCena: $price\n$storeName"
                                } else {
                                    "$type ($size)\nCena: $price\n$storeName"
                                }

                                val notification = NotificationUtils.getNotificationWithIntent(this, body, id)
                                NotificationUtils.safeNotify(this, id.hashCode(), notification)
                                Log.d("LocationService", "Notifikacija poslata za artikal $id")
                            } else {
                                Log.d("LocationService", "Artikal $id je već notifikovan (cooldown).")
                            }
                        }
                    }
                }
            }
    }


    private fun saveLocation(location: Location) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val data = hashMapOf(
            "userId" to userId,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to Timestamp.now()
        )
        firestore.collection("locations")
            .add(data)
            .addOnSuccessListener { Log.d("Firestore", "Lokacija sačuvana") }
            .addOnFailureListener { Log.e("Firestore", "Greška pri čuvanju", it) }
    }

    private fun isNearby(userLocation: Location, objectLocation: Location, radiusMeters: Float = 500f): Boolean {
        return userLocation.distanceTo(objectLocation) <= radiusMeters
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null

        clothesListener?.remove()
        clothesListener = null

        notifiedItems.clear()

        Log.d("LocationService", "Service ugašen")
    }
}
