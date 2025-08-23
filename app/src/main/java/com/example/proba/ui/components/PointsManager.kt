package com.example.proba.ui.components

import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore

object PointsManager {

    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    fun addPoints(userId: String, points: Int, onComplete: (() -> Unit)? = null) {
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentPoints = snapshot.getLong("points") ?: 0
            transaction.update(userRef, "points", currentPoints + points)
        }.addOnSuccessListener {
            onComplete?.invoke()
        }
    }

    fun calculateRank(points: Int): String {
        return when {
            points < 100 -> "No rank"
            points in 100..199 -> "Bronze"
            points in 200..299 -> "Silver"
            points in 300..399 -> "Gold"
            points >= 400 -> "Trophy"
            else -> "No rank"
        }
    }


}
