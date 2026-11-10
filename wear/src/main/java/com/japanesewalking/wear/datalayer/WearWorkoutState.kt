package com.japanesewalking.wear.datalayer

data class WearWorkoutState(
    val status: String = "IDLE", // IDLE, RUNNING, PAUSED
    val intervalType: String = "FAST", // FAST, NORMAL
    val remainingSeconds: Int = 180,
    val intervalTotalSeconds: Int = 180,
    val totalElapsedSeconds: Int = 0,
    val completedSets: Int = 0,
    val distanceMeters: Double = 0.0,
    val speedKmh: Double = 0.0
)
