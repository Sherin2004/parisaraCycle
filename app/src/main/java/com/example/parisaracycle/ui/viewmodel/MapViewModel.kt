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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
        _error.value = null
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
        _error.value = null
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
            _isLoading.value = true
            _error.value = null
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
                } else {
                    _error.value = "Location not found"
                }
            } catch (e: Exception) {
                _error.value = "Geocoder error: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchRoute(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val points = withContext(Dispatchers.IO) {
                try {
                    val originStr = "${origin.latitude},${origin.longitude}"
                    val destStr = "${destination.latitude},${destination.longitude}"
                    
                    // Try Bicycling mode
                    var result = fetchAndDecode(originStr, destStr, "bicycling")
                    
                    // Fallback to Walking if bicycling is not available
                    if (result.isEmpty()) {
                        result = fetchAndDecode(originStr, destStr, "walking")
                    }
                    
                    // Fallback to Driving as last resort
                    if (result.isEmpty()) {
                        result = fetchAndDecode(originStr, destStr, "driving")
                    }
                    
                    result
                } catch (e: Exception) {
                    null
                }
            }
            
            if (points == null) {
                _error.value = "Network or API Error"
            } else if (points.isEmpty()) {
                _error.value = "No path found between these locations"
            } else {
                _routePoints.value = points
                _error.value = null
            }
            _isLoading.value = false
        }
    }

    private fun fetchAndDecode(originStr: String, destStr: String, mode: String): List<LatLng> {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$originStr" +
                    "&destination=$destStr" +
                    "&mode=$mode" +
                    "&key=$apiKey"
            
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 10000
            connection.connectTimeout = 15000
            
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
                // If the specific mode fails, we return empty so the caller can try a fallback
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection?.disconnect()
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
