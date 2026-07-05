package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // timestamp
    val vehicleNumber: String,
    val driverName: String,
    val startingOdometer: Double,
    val endingOdometer: Double,
    val dieselFilled: Double, // in liters (can be 0 if no refuel)
    val dieselCost: Double, // cost of diesel filled in this entry
    val purpose: String,
    val fromLocation: String,
    val toLocation: String,
    val notes: String
) {
    val distance: Double
        get() = (endingOdometer - startingOdometer).coerceAtLeast(0.0)

    val mileage: Double
        get() = if (dieselFilled > 0 && distance > 0) distance / dieselFilled else 0.0
}
