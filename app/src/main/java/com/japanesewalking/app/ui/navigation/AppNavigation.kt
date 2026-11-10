package com.japanesewalking.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object InteractiveTour : Screen("interactive_tour")
    object TechniqueEducation : Screen("technique_education")
    object WorkoutSetup : Screen("workout_setup")
    object ActiveWorkout : Screen("active_workout")
    object WorkoutSummary : Screen("workout_summary")
    object History : Screen("history")
    object Settings : Screen("settings")
}
