package com.japanesewalking.app.data.model

enum class SessionStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class LiveWorkoutState(
    val status: SessionStatus = SessionStatus.IDLE,
    val currentInterval: IntervalType = IntervalType.FAST,
    val intervalRemainingSeconds: Int = 180,
    val intervalTotalSeconds: Int = 180,
    val intervalElapsedSeconds: Int = 0,
    val totalElapsedSeconds: Long = 0,
    val fastElapsedSeconds: Long = 0,
    val normalElapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val completedSets: Int = 0,
    val caloriesKcal: Int = 0,
    val estimatedHeartRateBpm: Int = 115, // Calculated effort estimate based on speed
    val goal: WorkoutGoal = WorkoutGoal(),
    val isGoalReached: Boolean = false
)
