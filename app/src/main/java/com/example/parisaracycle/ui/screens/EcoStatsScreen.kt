package com.example.parisaracycle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parisaracycle.data.EcoStatsManager
import kotlinx.coroutines.launch

@Composable
fun EcoStatsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ecoStatsManager = remember { EcoStatsManager(context) }
    
    val totalDistance by ecoStatsManager.totalDistanceFlow.collectAsState(initial = 0.0)
    val monthlyDistance by ecoStatsManager.monthlyDistanceFlow.collectAsState(initial = 0.0)
    
    val co2SavedTotal = ecoStatsManager.calculateCO2(totalDistance)
    val co2SavedMonth = ecoStatsManager.calculateCO2(monthlyDistance)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Eco Impact",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF2E7D32)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Lifetime Stats
        StatCard(
            title = "Lifetime Impact",
            distance = totalDistance,
            co2 = co2SavedTotal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        // Monthly Stats (Requirement 6: Must show Monthly Total)
        StatCard(
            title = "Monthly Progress",
            distance = monthlyDistance,
            co2 = co2SavedMonth,
            containerColor = Color(0xFFE8F5E9)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { 
                scope.launch {
                    ecoStatsManager.addDistance(2.0) 
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text("Simulate 2.0km Commute")
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Every kilometer cycled saves 120g of CO2 compared to a typical car journey.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    distance: Double,
    co2: Double,
    containerColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Distance", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${String.format("%.1f", distance)} km",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CO2 Saved", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${String.format("%.1f", co2)} g",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}
