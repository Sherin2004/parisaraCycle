package com.example.parisaracycle.data

import com.example.parisaracycle.model.Buddy
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BuddyRepository {
    private val database = FirebaseDatabase.getInstance()
    private val buddiesRef = database.getReference("buddies")

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
}
