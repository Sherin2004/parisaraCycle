package com.example.parisaracycle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parisaracycle.model.DangerZone
import com.example.parisaracycle.model.DangerType
import com.example.parisaracycle.model.PitStop
import com.example.parisaracycle.model.PitStopType
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.compose.MapEffect
// CRITICAL: Ensure this exact import is present


@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapScreen() {
    val defaultPos = LatLng(12.9716, 77.5946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 14f)
    }

    var isMapLoaded by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinLocation by remember { mutableStateOf<LatLng?>(null) }

    val dangerZones = remember { mutableStateListOf<DangerZone>() }
    val pitStops = remember {
        mutableStateListOf(
            PitStop("p1", LatLng(12.9750, 77.5940), "Repair Shop", PitStopType.REPAIR_SHOP),
            PitStop("p2", LatLng(12.9700, 77.6000), "Water Point", PitStopType.WATER_POINT)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false,
                isBuildingEnabled = true
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true
            ),
            onMapLoaded = { isMapLoaded = true },
            onMapLongClick = { latLng ->
                pinLocation = latLng
                showPinDialog = true
            }
        ) {
            // Danger Zones
            dangerZones.forEach { zone ->
                Marker(
                    state = MarkerState(position = zone.position),
                    title = "Hazard: ${zone.type.name}",
                    snippet = "Pinned by community"
                )
            }

            // Pit Stops
            pitStops.forEach { stop ->
                Marker(
                    state = MarkerState(position = stop.position),
                    title = stop.name,
                    snippet = stop.type.name
                )
            }
        }

        if (!isMapLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Loading Maps...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AddLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Long-press to pin hazards",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    if (showPinDialog && pinLocation != null) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Report Hazard") },
            text = { Text("Mark this location as dangerous for cyclists?") },
            confirmButton = {
                Button(onClick = {
                    dangerZones.add(DangerZone(
                        id = System.currentTimeMillis().toString(),
                        position = pinLocation!!,
                        type = DangerType.BLOCKAGE
                    ))
                    showPinDialog = false
                }) {
                    Text("Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}