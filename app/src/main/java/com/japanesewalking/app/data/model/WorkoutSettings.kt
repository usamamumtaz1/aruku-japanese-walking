package com.japanesewalking.app.data.model

data class WorkoutSettings(
    val fastIntervalSeconds: Int = 180, // 3 minutes standard
    val normalIntervalSeconds: Int = 180, // 3 minutes standard
    val isVoiceEnabled: Boolean = true,
    val isChimeEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val audioDuckingEnabled: Boolean = true,
    val periodicDistanceUpdates: Boolean = true, // Announce every 1 km / 0.5 km
    val periodicDistanceIntervalMeters: Double = 1000.0
)
