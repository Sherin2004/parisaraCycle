package com.example.parisaracycle.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parisaracycle.data.BuddyRepository
import com.example.parisaracycle.data.EcoStatsManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val ecoStatsManager = EcoStatsManager(application)
    private val buddyRepository = BuddyRepository()
    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val dbUrl = "https://parisaracycle-9f3ec-default-rtdb.firebaseio.com/"

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation ?: return
            val currentPos = LatLng(lastLocation.latitude, lastLocation.longitude)
            _currentLocation.value = currentPos
            updateLocationInFirebase(currentPos)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val user = auth.currentUser ?: return
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationInFirebase(currentPos: LatLng) {
        val user = auth.currentUser ?: return
        
        FirebaseDatabase.getInstance(dbUrl).getReference("buddies").child(user.uid).child("name")
            .get().addOnSuccessListener { snapshot ->
                val name = snapshot.getValue(String::class.java) ?: user.email ?: "Cyclist"
                buddyRepository.updateMyLocation(user.uid, name, currentPos)
            }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
