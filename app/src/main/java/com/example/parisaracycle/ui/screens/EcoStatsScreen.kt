package com.example.parisaracycle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoStatsScreen() {
    var distanceInput by remember { mutableStateOf("") }
    var isKm by remember { mutableStateOf(true) }

    val distance = distanceInput.toDoubleOrNull() ?: 0.0
    val distanceInKm = if (isKm) distance else distance * 1.60934

    // Calculations (Constants based on average mid-size car)
    val co2SavedGrams = distanceInKm * 210.0 // 210g per km
    val co2SavedKg = co2SavedGrams / 1000.0
    val treesEquivalent = co2SavedKg / 0.05 // Approx 1 tree absorbs 20kg CO2/year -> 0.05kg/day
    val fuelSavedLiters = distanceInKm / 12.0 // Assuming 12km/L average efficiency

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF8))
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2E7D32), Color(0xFF43A047))
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Text(
                    text = "Eco Impact",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Calculate your positive contribution",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Calculator Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Distance Calculator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = distanceInput,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) distanceInput = it },
                            label = { Text("Enter distance") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E7D32),
                                focusedLabelColor = Color(0xFF2E7D32)
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Unit Toggle
                        Surface(
                            onClick = { isKm = !isKm },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isKm) "KM" else "MILES",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "YOUR SAVINGS",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            // Result Cards Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ImpactCard(
                    modifier = Modifier.weight(1f),
                    title = "CO2 Saved",
                    value = if (co2SavedKg < 1.0) "${co2SavedGrams.roundToInt()}g" else "${String.format("%.2f", co2SavedKg)}kg",
                    icon = Icons.Default.CloudQueue,
                    color = Color(0xFF2196F3)
                )
                ImpactCard(
                    modifier = Modifier.weight(1f),
                    title = "Fuel Saved",
                    value = "${String.format("%.1f", fuelSavedLiters)} L",
                    icon = Icons.Default.LocalGasStation,
                    color = Color(0xFFFF9800)
                )
            }

            ImpactCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Nature Equivalent",
                value = "${treesEquivalent.roundToInt()} Tree-Days",
                subtitle = "CO2 absorption equivalent to one tree for this many days",
                icon = Icons.Default.Park,
                color = Color(0xFF4CAF50)
            )

            // Fun Fact
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFECB3).copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF57F17))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cycling 10km a day can save up to 750kg of CO2 per year!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5D4037)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ImpactCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontSize = 14.sp, color = Color.Gray)
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 11.sp, lineHeight = 14.sp, color = Color.Gray)
            }
        }
    }
}
