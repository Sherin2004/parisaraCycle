package com.example.parisaracycle.model

import com.google.android.gms.maps.model.LatLng

data class DangerZone(
    val id: String = "",
    val position: LatLng = LatLng(0.0, 0.0),
    val description: String = "",
    val type: DangerType = DangerType.POTHOLE
)

enum class DangerType {
    POTHOLE, DANGEROUS_INTERSECTION, BLOCKAGE
}

data class PitStop(
    val id: String = "",
    val position: LatLng = LatLng(0.0, 0.0),
    val name: String = "",
    val type: PitStopType = PitStopType.REPAIR_SHOP
)

enum class PitStopType {
    REPAIR_SHOP, WATER_POINT
}

data class Buddy(
    val id: String = "",
    val name: String = "",
    val position: LatLng = LatLng(0.0, 0.0),
    val lastUpdated: Long = 0L
)
