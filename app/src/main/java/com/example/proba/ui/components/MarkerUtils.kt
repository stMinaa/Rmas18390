package com.example.proba.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun createCustomMarker(
    context: Context,
    @DrawableRes pinResId: Int,
    @DrawableRes iconResId: Int
): BitmapDescriptor {
    val pin = ContextCompat.getDrawable(context, pinResId)!!
    val icon = ContextCompat.getDrawable(context, iconResId)!!

    // 🔽 dimenzije pina – ovo je Google maps default ~110px visok
    val widthPx = 96
    val heightPx = 128

    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    pin.setBounds(0, 0, widthPx, heightPx)
    pin.draw(canvas)

    // ikonica koja ide u središnji "balonasti" deo pina
    val iconSize = (widthPx * 0.60).toInt()      // 60% širine pina
    val left = (widthPx - iconSize) / 2
    val top = (heightPx * 0.15).toInt()          // pomeramo malo na gore tako da je u okruglom delu
    icon.setBounds(left, top, left + iconSize, top + iconSize)
    icon.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun createBlinkingCustomMarker(
    context: Context,
    @DrawableRes pinResId: Int,
    @DrawableRes iconResId: Int,
    @ColorInt colorFilter: Int? = null
): BitmapDescriptor {
    val pin = ContextCompat.getDrawable(context, pinResId)!!.mutate()
    val icon = ContextCompat.getDrawable(context, iconResId)!!.mutate()

    colorFilter?.let {
        val filter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_ATOP)
        pin.colorFilter = filter
        icon.colorFilter = filter
    }

    val widthPx = 96
    val heightPx = 128

    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    pin.setBounds(0, 0, widthPx, heightPx)
    pin.draw(canvas)

    val iconSize = (widthPx * 0.60).toInt()
    val left = (widthPx - iconSize) / 2
    val top = (heightPx * 0.15).toInt()
    icon.setBounds(left, top, left + iconSize, top + iconSize)
    icon.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}



fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}