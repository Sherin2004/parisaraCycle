package com.example.parisaracycle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.parisaracycle.model.DangerZone
import com.example.parisaracycle.model.DangerType
import com.example.parisaracycle.model.PitStop
import com.example.parisaracycle.model.PitStopType
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parisaracycle.data.BuddyRepository
import com.example.parisaracycle.ui.viewmodel.LocationViewModel
import com.example.parisaracycle.ui.viewmodel.MapViewModel
import com.google.firebase.auth.FirebaseAuth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    locationViewModel: LocationViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val defaultPos = LatLng(12.9716, 77.5946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 14f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationViewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationViewModel.stopLocationUpdates()
        }
    }

    val targetLocation by mapViewModel.targetLocation.collectAsState()
    val destination by mapViewModel.destination.collectAsState()
    val isNavigating by mapViewModel.isNavigating.collectAsState()
    val searchQuery by mapViewModel.searchQuery.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val routePoints by mapViewModel.routePoints.collectAsState()
    
    val hazardsList by mapViewModel.hazards.collectAsState()
    val pitStopsList by mapViewModel.pitStops.collectAsState()

    LaunchedEffect(targetLocation) {
        targetLocation?.let { latLng ->
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 16f)
            )
            mapViewModel.clearTargetLocation()
        }
    }

    // Auto-center camera if navigating
    LaunchedEffect(currentLocation, isNavigating) {
        if (isNavigating && currentLocation != null) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                        .target(currentLocation!!)
                        .zoom(18f)
                        .bearing(0f)
                        .tilt(45f)
                        .build()
                )
            )
        }
    }

    // Re-fetch route if destination exists but route is empty
    LaunchedEffect(currentLocation, destination) {
        if (currentLocation != null && destination != null && routePoints.isEmpty()) {
            mapViewModel.fetchRoute(currentLocation!!, destination!!)
        }
    }

    val repository = remember { BuddyRepository() }
    val buddies by repository.getNearbyBuddies().collectAsState(initial = emptyList())
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var isMapLoaded by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinLocation by remember { mutableStateOf<LatLng?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = true,
                isBuildingEnabled = true
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapLoaded = { isMapLoaded = true },
            onMapLongClick = { latLng ->
                if (!isNavigating) {
                    pinLocation = latLng
                    showPinDialog = true
                }
            }
        ) {
            // Buddies
            buddies.forEach { buddy ->
                if (buddy.id != currentUserId) {
                    Marker(
                        state = rememberMarkerState(position = buddy.position),
                        title = buddy.name,
                        snippet = "Active Buddy",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }

            // Danger Zones (from Firebase)
            hazardsList.forEach { zone ->
                val hue = when (zone.type) {
                    DangerType.POTHOLE -> BitmapDescriptorFactory.HUE_RED
                    DangerType.BLOCKAGE -> BitmapDescriptorFactory.HUE_ORANGE
                    DangerType.DANGEROUS_INTERSECTION -> BitmapDescriptorFactory.HUE_VIOLET
                    DangerType.OTHER -> BitmapDescriptorFactory.HUE_MAGENTA
                }
                Marker(
                    state = rememberMarkerState(position = zone.position),
                    title = zone.type.name.replace("_", " "),
                    snippet = "Hazard reported",
                    icon = BitmapDescriptorFactory.defaultMarker(hue)
                )
            }

            // Pit Stops (from Firebase)
            pitStopsList.forEach { stop ->
                val hue = when (stop.type) {
                    PitStopType.REPAIR -> BitmapDescriptorFactory.HUE_GREEN
                    PitStopType.WATER -> BitmapDescriptorFactory.HUE_CYAN
                    PitStopType.PARKING -> BitmapDescriptorFactory.HUE_BLUE
                    PitStopType.AIR_PUMP -> BitmapDescriptorFactory.HUE_YELLOW
                    PitStopType.CHARGING -> BitmapDescriptorFactory.HUE_ROSE
                }
                Marker(
                    state = rememberMarkerState(position = stop.position),
                    title = stop.name,
                    snippet = stop.type.name,
                    icon = BitmapDescriptorFactory.defaultMarker(hue)
                )
            }

            // Destination Marker
            destination?.let { dest ->
                Marker(
                    state = rememberMarkerState(position = dest),
                    title = "Destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )

                // Route Polyline
                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF4285F4),
                        width = 15f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                }
            }
        }

        // Top UI
        if (!isNavigating) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Surface(
                    modifier = Modifier.padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Parisara-Cycle", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    }
                }

                // Search Bar
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.98f),
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { mapViewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search destination...", fontSize = 16.sp, color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                mapViewModel.searchLocation(context, searchQuery, currentLocation)
                                focusManager.clearFocus()
                            })
                        )
                    }
                }
            }
        } else {
             // Navigation Info Overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Navigating...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Follow the cycling path",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(
                        onClick = { mapViewModel.stopNavigation() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Start Ride Card
        if (destination != null && !isNavigating) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 12.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Ready to start?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                    if (routePoints.isEmpty()) {
                        Text("Calculating path...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        Text("Route found!", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { mapViewModel.startNavigation() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF06292)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Ride", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { mapViewModel.setDestination(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }

        if (showPinDialog && pinLocation != null) {
            PinReportDialog(
                onDismiss = { showPinDialog = false },
                onConfirm = { type, category ->
                    if (category == "HAZARD") {
                        mapViewModel.reportHazard(DangerZone(
                            id = "",
                            position = pinLocation!!,
                            type = type as DangerType
                        ))
                    } else {
                        mapViewModel.addPitStop(PitStop(
                            id = "",
                            position = pinLocation!!,
                            name = type.toString().replace("_", " "),
                            type = type as PitStopType
                        ))
                    }
                    showPinDialog = false
                }
            )
        }
    }
}

@Composable
fun PinReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (Any, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("HAZARD") }
    var selectedType by remember { mutableStateOf<Any?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin Hazard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // Category Tabs
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { selectedCategory = "HAZARD"; selectedType = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == "HAZARD") Color(0xFFFF5252) else Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("HAZARD", color = if (selectedCategory == "HAZARD") Color.White else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { selectedCategory = "PIT STOP"; selectedType = null },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == "PIT STOP") Color(0xFF448AFF) else Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("PIT STOP", color = if (selectedCategory == "PIT STOP") Color.White else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Grid of options
                val options = if (selectedCategory == "HAZARD") {
                    listOf(
                        "Pothole" to DangerType.POTHOLE,
                        "Blockage" to DangerType.BLOCKAGE,
                        "Dangerous intersection" to DangerType.DANGEROUS_INTERSECTION,
                        "Other" to DangerType.OTHER
                    )
                } else {
                    listOf(
                        "Repair" to PitStopType.REPAIR,
                        "Water" to PitStopType.WATER,
                        "Parking" to PitStopType.PARKING,
                        "Air pump" to PitStopType.AIR_PUMP,
                        "Charging" to PitStopType.CHARGING
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(options) { (label, type) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { selectedType = type },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedType == type) {
                                if (selectedCategory == "HAZARD") Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
                            } else Color(0xFFF8F8F8),
                            border = if (selectedType == type) {
                                androidx.compose.foundation.BorderStroke(2.dp, if (selectedCategory == "HAZARD") Color(0xFFFF5252) else Color(0xFF448AFF))
                            } else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                        ) {
                            Box(modifier = Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedType == type) {
                                        if (selectedCategory == "HAZARD") Color(0xFFD32F2F) else Color(0xFF1976D2)
                                    } else Color.DarkGray
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👇", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Click on map to select location", fontSize = 12.sp, color = Color(0xFFE64A19), fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { selectedType?.let { onConfirm(it, selectedCategory) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF06292),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(26.dp),
                    enabled = selectedType != null,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Confirm Report", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}