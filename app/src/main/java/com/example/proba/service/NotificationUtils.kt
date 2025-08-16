package com.example.proba.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.proba.MainActivity
import com.example.proba.R

object NotificationUtils {
    private const val CHANNEL_ID = "location_channel"
    private const val CHANNEL_NAME = "VintageShopper: Lokacija"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Koristi se za servis praćenja lokacije"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun getNotification(context: Context, content: String, pinId: String? = null): Notification {
        Log.d("Notifikacija", "Pozvana je funkcija za pravljenje notifikacije.")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("highlightPinId", pinId) // ovo prosleđuje koji pin treba da zasvetli
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VintageShopper")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    fun getNotificationWithIntent(
        context: Context,
        content: String,
        clothesId: String? = null
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            clothesId?.let {
                putExtra("navigateTo", "clothesDetail")
                putExtra("clothesId", it)
                putExtra("highlightPinId", it) // Ako želiš i da se označi pin kad se vratiš na mapu
            }
        }

        val requestCode = clothesId?.hashCode() ?: 0
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            pendingFlags
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VintageShopper")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun getNotificationForClothes(context: Context, content: String, clothesId: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("clothesId", clothesId) // šaljemo ID odeće
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            clothesId.hashCode(), // unikatan requestCode po odeći
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VintageShopper")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_purple_pin) // tvoja ljubičasta ikonica
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // klikom nestaje notifikacija
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }


    fun safeNotify(context: Context, notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationUtils", "POST_NOTIFICATIONS permission not granted - can't show notification")
                return
            }
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }


}
