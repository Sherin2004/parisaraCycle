package com.example.parisaracycle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parisaracycle.data.BuddyRepository
import com.example.parisaracycle.model.Buddy
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.foundation.shape.CircleShape
import com.google.android.gms.maps.model.LatLng

@Composable
fun BuddyScreen(onJoinRide: (LatLng) -> Unit = {}) {
    val repository = remember { BuddyRepository() }
    val buddiesRaw by repository.getNearbyBuddies().collectAsState(initial = emptyList())
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // Process buddies to put "Me" on top
    val buddies = remember(buddiesRaw, currentUserId) {
        val sortedList = buddiesRaw.sortedWith(compareByDescending { it.id == currentUserId })
        sortedList.map { buddy ->
            if (buddy.id == currentUserId) {
                buddy.copy(name = "Me (You)")
            } else {
                buddy
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Cycling Buddies Nearby",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Connect with students on your route for safety.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (buddies.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Searching for buddies nearby...", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(buddies) { buddy ->
                    BuddyItem(
                        buddy = buddy, 
                        isMe = buddy.id == currentUserId,
                        onJoinRide = onJoinRide
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Safety Tip",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Cycling in groups reduces accidents and makes you more visible to heavy vehicles.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyItem(buddy: Buddy, isMe: Boolean, onJoinRide: (LatLng) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onJoinRide(buddy.position) }, // Entire card is now clickable
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMe) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isMe) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buddy.name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = if (isMe) "Click to see your location on map" else "Active Now", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isMe) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isMe) {
                Button(
                    onClick = { 
                        onJoinRide(buddy.position) 
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Join Ride", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("YOU", modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}
