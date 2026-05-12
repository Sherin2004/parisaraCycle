package com.example.parisaracycle.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parisaracycle.data.BuddyRepository
import com.example.parisaracycle.data.EcoStatsManager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.util.*

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val ecoStatsManager = EcoStatsManager(application)
    private val buddyRepository = BuddyRepository()
    
    // Mock user ID for demo
    private val userId = UUID.randomUUID().toString()
    private val userName = "Cyclist_${userId.take(4)}"

    fun simulateRide(distanceKm: Double, currentPos: LatLng) {
        viewModelScope.launch {
            // Update local eco stats
            ecoStatsManager.addDistance(distanceKm)
            
            // Broadcast location to Firebase Buddy System
            buddyRepository.updateMyLocation(userId, userName, currentPos)
        }
    }
}
