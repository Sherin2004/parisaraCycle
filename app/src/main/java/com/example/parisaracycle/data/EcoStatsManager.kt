package com.example.parisaracycle.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

val Context.dataStore by preferencesDataStore(name = "eco_stats")

class EcoStatsManager(private val context: Context) {
    private val TOTAL_DISTANCE_KEY = doublePreferencesKey("total_distance_km")
    
    // Key for current month: e.g., "dist_2024_05"
    private fun getMonthlyKey() = doublePreferencesKey("dist_${SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())}")

    val totalDistanceFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[TOTAL_DISTANCE_KEY] ?: 0.0
        }

    val monthlyDistanceFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[getMonthlyKey()] ?: 0.0
        }

    suspend fun addDistance(distanceKm: Double) {
        context.dataStore.edit { preferences ->
            // Update lifetime total
            val currentTotal = preferences[TOTAL_DISTANCE_KEY] ?: 0.0
            preferences[TOTAL_DISTANCE_KEY] = currentTotal + distanceKm
            
            // Update monthly total
            val monthKey = getMonthlyKey()
            val currentMonthly = preferences[monthKey] ?: 0.0
            preferences[monthKey] = currentMonthly + distanceKm
        }
    }

    fun calculateCO2(distanceKm: Double): Double {
        return distanceKm * 120.0 // 1km = 120g CO2 savings
    }
}
