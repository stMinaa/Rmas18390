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
            points < 50 -> "No rank"
            points < 100 -> "Bronze"
            points < 200 -> "Silver"
            points < 500 -> "Gold"
            else -> "Trophy"
        }
    }
}
