package com.example.parisaracycle.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parisaracycle.data.BuddyRepository
import com.example.parisaracycle.model.DangerZone
import com.example.parisaracycle.model.PitStop
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MapViewModel : ViewModel() {
    private val repository = BuddyRepository()

    private val _targetLocation = MutableStateFlow<LatLng?>(null)
    val targetLocation: StateFlow<LatLng?> = _targetLocation

    private val _destination = MutableStateFlow<LatLng?>(null)
    val destination: StateFlow<LatLng?> = _destination

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    val hazards: StateFlow<List<DangerZone>> = repository.getHazards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pitStops: StateFlow<List<PitStop>> = repository.getPitStops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val apiKey = "AIzaSyDVV3XkWY4t49ljEyKoskDLvD4JAQ88SU0"

    fun setTargetLocation(location: LatLng) {
        _targetLocation.value = location
    }

    fun clearTargetLocation() {
        _targetLocation.value = null
    }

    fun setDestination(location: LatLng?) {
        _destination.value = location
        if (location == null) {
            _routePoints.value = emptyList()
        }
    }

    fun startNavigation() {
        if (_destination.value != null) {
            _isNavigating.value = true
        }
    }

    fun stopNavigation() {
        _isNavigating.value = false
        _destination.value = null
        _searchQuery.value = ""
        _routePoints.value = emptyList()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun reportHazard(hazard: DangerZone) {
        repository.reportHazard(hazard)
    }

    fun addPitStop(pitStop: PitStop) {
        repository.addPitStop(pitStop)
    }

    fun searchLocation(context: Context, query: String, origin: LatLng?) {
        viewModelScope.launch {
            val geocoder = Geocoder(context)
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    setDestination(latLng)
                    setTargetLocation(latLng)
                    
                    if (origin != null) {
                        fetchRoute(origin, latLng)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun fetchRoute(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            val points = withContext(Dispatchers.IO) {
                try {
                    val originStr = "${origin.latitude},${origin.longitude}"
                    val destStr = "${destination.latitude},${destination.longitude}"
                    val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=$originStr" +
                            "&destination=$destStr" +
                            "&mode=bicycling" +
                            "&key=$apiKey"
                    
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    val response = connection.inputStream.bufferedReader().readText()
                    
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status")
                    
                    if (status == "OK") {
                        val routes = jsonResponse.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val polyPoints = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline")
                                .getString("points")
                            decodePolyline(polyPoints)
                        } else {
                            emptyList()
                        }
                    } else {
                        fetchFallbackRoute(originStr, destStr)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            if (points.isNotEmpty()) {
                _routePoints.value = points
            }
        }
    }

    private suspend fun fetchFallbackRoute(originStr: String, destStr: String): List<LatLng> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=$originStr" +
                        "&destination=$destStr" +
                        "&mode=driving" +
                        "&key=$apiKey"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                if (jsonResponse.optString("status") == "OK") {
                    val routes = jsonResponse.getJSONArray("routes")
                    val polyPoints = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")
                    decodePolyline(polyPoints)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}
