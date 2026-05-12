package com.example.parisaracycle.data

import com.example.parisaracycle.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BuddyRepository {
    private val database = FirebaseDatabase.getInstance("https://parisaracycle-9f3ec-default-rtdb.firebaseio.com/")
    private val buddiesRef = database.getReference("buddies")
    private val hazardsRef = database.getReference("hazards")
    private val pitStopsRef = database.getReference("pitstops")

    fun getNearbyBuddies(): Flow<List<Buddy>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val buddies = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: ""
                    val name = child.child("name").getValue(String::class.java) ?: "Anonymous"
                    val lat = child.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                    Buddy(id, name, LatLng(lat, lng))
                }
                trySend(buddies)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        buddiesRef.addValueEventListener(listener)
        awaitClose { buddiesRef.removeEventListener(listener) }
    }

    fun updateMyLocation(userId: String, name: String, location: LatLng) {
        val data = mapOf(
            "name" to name,
            "lat" to location.latitude,
            "lng" to location.longitude,
            "lastUpdated" to System.currentTimeMillis()
        )
        buddiesRef.child(userId).setValue(data)
    }

    fun reportHazard(hazard: DangerZone) {
        val data = mapOf(
            "lat" to hazard.position.latitude,
            "lng" to hazard.position.longitude,
            "type" to hazard.type.name,
            "description" to hazard.description,
            "timestamp" to System.currentTimeMillis()
        )
        hazardsRef.push().setValue(data)
    }

    fun addPitStop(pitStop: PitStop) {
        val data = mapOf(
            "lat" to pitStop.position.latitude,
            "lng" to pitStop.position.longitude,
            "type" to pitStop.type.name,
            "name" to pitStop.name,
            "timestamp" to System.currentTimeMillis()
        )
        pitStopsRef.push().setValue(data)
    }

    fun getHazards(): Flow<List<DangerZone>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hazards = snapshot.children.mapNotNull { child ->
                    val lat = child.child("lat").getValue(Double::class.java) ?: return@mapNotNull null
                    val lng = child.child("lng").getValue(Double::class.java) ?: return@mapNotNull null
                    val typeStr = child.child("type").getValue(String::class.java) ?: "OTHER"
                    DangerZone(
                        id = child.key ?: "",
                        position = LatLng(lat, lng),
                        type = DangerType.valueOf(typeStr)
                    )
                }
                trySend(hazards)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        hazardsRef.addValueEventListener(listener)
        awaitClose { hazardsRef.removeEventListener(listener) }
    }

    fun getPitStops(): Flow<List<PitStop>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stops = snapshot.children.mapNotNull { child ->
                    val lat = child.child("lat").getValue(Double::class.java) ?: return@mapNotNull null
                    val lng = child.child("lng").getValue(Double::class.java) ?: return@mapNotNull null
                    val typeStr = child.child("type").getValue(String::class.java) ?: "REPAIR"
                    val name = child.child("name").getValue(String::class.java) ?: "Pit Stop"
                    PitStop(
                        id = child.key ?: "",
                        position = LatLng(lat, lng),
                        name = name,
                        type = PitStopType.valueOf(typeStr)
                    )
                }
                trySend(stops)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        pitStopsRef.addValueEventListener(listener)
        awaitClose { pitStopsRef.removeEventListener(listener) }
    }
}
