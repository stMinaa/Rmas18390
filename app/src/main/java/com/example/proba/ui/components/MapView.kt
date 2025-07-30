package com.example.proba.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapView(
    userLocation: LatLng,
    cameraPositionState: CameraPositionState,
    permissionsGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    if (permissionsGranted) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLocation, 15f)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = userLocation),
                title = "Tvoja lokacija"
            )
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text("Dozvoli pristup lokaciji")
            }
        }
    }
}
