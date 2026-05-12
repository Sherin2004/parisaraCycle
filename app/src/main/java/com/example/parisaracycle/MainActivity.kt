package com.example.parisaracycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parisaracycle.ui.screens.*
import com.example.parisaracycle.ui.theme.ParisaraCycleTheme
import com.example.parisaracycle.ui.viewmodel.MapViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParisaraCycleTheme {
                var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

                if (currentUser == null) {
                    AuthScreen(onAuthSuccess = {
                        currentUser = FirebaseAuth.getInstance().currentUser
                    })
                } else {
                    MainApp(onLogout = { currentUser = null })
                }
            }
        }
    }
}

@Composable
fun MainApp(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val mapViewModel: MapViewModel = viewModel()
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Map", "Stats", "Buddies", "Profile")
    val icons = listOf(Icons.Default.Map, Icons.Default.Public, Icons.Default.Group, Icons.Default.AccountCircle)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            when (index) {
                                0 -> navController.navigate("map")
                                1 -> navController.navigate("stats")
                                2 -> navController.navigate("buddies")
                                3 -> navController.navigate("profile")
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("map") { 
                MapScreen(mapViewModel = mapViewModel) 
            }
            composable("stats") { EcoStatsScreen() }
            composable("buddies") { 
                BuddyScreen(
                    onJoinRide = { location ->
                        mapViewModel.setTargetLocation(location)
                        selectedItem = 0 // Update bottom bar selection
                        navController.navigate("map") {
                            popUpTo("map") { inclusive = true }
                        }
                    }
                ) 
            }
            composable("profile") { ProfileScreen(onLogout = onLogout) }
        }
    }
}
