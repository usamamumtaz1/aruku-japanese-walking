package com.japanesewalking.app.data.model

enum class IntervalType(
    val title: String,
    val description: String,
    val targetEffortPercent: Int
) {
    FAST(
        title = "Fast Walk",
        description = "Brisk pace (~70% max effort). Conversation should be challenging.",
        targetEffortPercent = 70
    ),
    NORMAL(
        title = "Moderate Walk",
        description = "Relaxed recovery pace (~40% max effort). Deep breathing.",
        targetEffortPercent = 40
    )
}
