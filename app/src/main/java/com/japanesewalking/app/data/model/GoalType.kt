package com.japanesewalking.app.data.model

enum class GoalType(val displayName: String) {
    TIME_BOUND("Time Target"),
    DISTANCE_BOUND("Distance Target"),
    OPEN_ENDED("Open-Ended Free Walk")
}

data class WorkoutGoal(
    val type: GoalType = GoalType.TIME_BOUND,
    val targetSeconds: Int = 30 * 60, // Default 30 min (5 cycles)
    val targetMeters: Double = 3000.0 // Default 3.0 km
)
