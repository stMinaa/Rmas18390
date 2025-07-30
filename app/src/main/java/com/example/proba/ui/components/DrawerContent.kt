package com.example.proba.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DrawerContent(onProfileClick: () -> Unit) {
    ModalDrawerSheet {
        Text(
            "Meni",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profil") },
            label = { Text("Profil", color = MaterialTheme.colorScheme.onBackground) },
            selected = false,
            onClick = { onProfileClick() }
        )
    }
}
