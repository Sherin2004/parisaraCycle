package com.example.parisaracycle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { onJoinRide(buddy.position) },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMe) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        border = if (isMe) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Box(
            modifier = Modifier.background(
                if (isMe) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color.White, Color.White)
                    )
                }
            )
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
                    modifier = Modifier.size(52.dp),
                    shadowElevation = if (isMe) 4.dp else 0.dp
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buddy.name, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.height(18.dp)
                            ) {
                                Text(
                                    "YOU", 
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isMe) "Tracking your location" else "Nearby • 2 min ago", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Gray
                    )
                }
                
                if (!isMe) {
                    IconButton(
                        onClick = { onJoinRide(buddy.position) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = "Join Ride")
                    }
                } else {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
