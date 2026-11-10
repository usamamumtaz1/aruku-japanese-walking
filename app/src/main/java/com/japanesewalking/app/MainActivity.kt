package com.japanesewalking.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.japanesewalking.app.data.model.SessionStatus
import com.japanesewalking.app.ui.navigation.Screen
import com.japanesewalking.app.ui.screens.*
import com.japanesewalking.app.ui.theme.JapaneseWalkingTheme
import com.japanesewalking.app.ui.theme.getAmbientAppBackgroundBrush
import com.japanesewalking.app.ui.viewmodel.DarkThemeConfig
import com.japanesewalking.app.ui.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {

    private val workoutViewModel: WorkoutViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Gracefully continues regardless of choice
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleWatchAutoStart(intent)

        // Minimal permission: Only prompt notification on Android 13+ for pocket lockscreen controls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Pocket Tracking: Prompt to ignore battery optimization so screen-off intervals never suspend
        try {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(batteryIntent)
            }
        } catch (e: Exception) {
            // Graceful fallback for devices that restrict direct intent
        }

        setContent {
            val darkThemeConfig by workoutViewModel.darkThemeConfig.collectAsState()
            val isDark = when (darkThemeConfig) {
                DarkThemeConfig.SYSTEM -> isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }

            JapaneseWalkingTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getAmbientAppBackgroundBrush(isDark))
                    ) {
                        AppNavigationHost(viewModel = workoutViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWatchAutoStart(intent)
    }

    private fun handleWatchAutoStart(intent: Intent?) {
        if (intent?.action == "com.japanesewalking.app.AUTO_START_WORKOUT") {
            workoutViewModel.startWorkout()
        }
    }
}

@Composable
fun AppNavigationHost(viewModel: WorkoutViewModel) {
    val navController = rememberNavController()
    val liveState by viewModel.liveState.collectAsState()
    val isTourCompleted by viewModel.isTourCompleted.collectAsState()

    // If workout is active and not on ActiveWorkout screen, auto-route to live coach
    LaunchedEffect(liveState.status) {
        if ((liveState.status == SessionStatus.RUNNING || liveState.status == SessionStatus.PAUSED) &&
            navController.currentDestination?.route != Screen.ActiveWorkout.route
        ) {
            navController.navigate(Screen.ActiveWorkout.route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isTourCompleted) Screen.Home.route else Screen.InteractiveTour.route
    ) {
        composable(Screen.InteractiveTour.route) {
            InteractiveTourScreen(
                onComplete = {
                    viewModel.completeTour()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.InteractiveTour.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onStartWorkoutClick = { navController.navigate(Screen.WorkoutSetup.route) },
                onEducationClick = { navController.navigate(Screen.TechniqueEducation.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.TechniqueEducation.route) {
            TechniqueEducationScreen(
                onBackClick = { navController.popBackStack() },
                onStartWalkClick = {
                    navController.navigate(Screen.WorkoutSetup.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.WorkoutSetup.route) {
            WorkoutSetupScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onStartWorkoutClick = {
                    navController.navigate(Screen.ActiveWorkout.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.ActiveWorkout.route) {
            ActiveWorkoutScreen(
                viewModel = viewModel,
                onWorkoutFinished = {
                    navController.navigate(Screen.WorkoutSummary.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.WorkoutSummary.route) {
            WorkoutSummaryScreen(
                viewModel = viewModel,
                onDoneClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onReplayTourClick = {
                    navController.navigate(Screen.InteractiveTour.route)
                }
            )
        }
    }
}
