package com.japanesewalking.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val distanceMeters: Double,
    val fastDurationSeconds: Long,
    val normalDurationSeconds: Long,
    val completedSets: Int,
    val caloriesKcal: Int,
    val averageSpeedKmh: Double,
    val goalType: String,
    val targetDurationSeconds: Int = 0,
    val targetDistanceMeters: Double = 0.0
)
