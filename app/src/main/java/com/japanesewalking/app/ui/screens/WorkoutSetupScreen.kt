package com.japanesewalking.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japanesewalking.app.data.model.GoalType
import com.japanesewalking.app.ui.theme.FastWalkCoral
import com.japanesewalking.app.ui.theme.ForestGreen
import com.japanesewalking.app.ui.theme.FrostedGlassCard
import com.japanesewalking.app.ui.theme.RecoveryTeal
import com.japanesewalking.app.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSetupScreen(
    viewModel: WorkoutViewModel,
    onBackClick: () -> Unit,
    onStartWorkoutClick: () -> Unit
) {
    val configuredGoal by viewModel.configuredGoal.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Configure Walking Session", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Button(
                    onClick = {
                        viewModel.startWorkout()
                        onStartWorkoutClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Walking Session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Headphone Guidance Banner with Frosted Glass
            FrostedGlassCard(
                accentTint = ForestGreen,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ForestGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Headphones Recommended", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Audio coach will notify you when to speed up, ducking smoothly over music or podcasts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Goal Selection Type
            Text("Select Session Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalTypeTab(
                    title = "Time",
                    isSelected = configuredGoal.type == GoalType.TIME_BOUND,
                    onClick = { viewModel.updateGoalType(GoalType.TIME_BOUND) },
                    modifier = Modifier.weight(1f)
                )
                GoalTypeTab(
                    title = "Distance",
                    isSelected = configuredGoal.type == GoalType.DISTANCE_BOUND,
                    onClick = { viewModel.updateGoalType(GoalType.DISTANCE_BOUND) },
                    modifier = Modifier.weight(1f)
                )
                GoalTypeTab(
                    title = "Open-Ended",
                    isSelected = configuredGoal.type == GoalType.OPEN_ENDED,
                    onClick = { viewModel.updateGoalType(GoalType.OPEN_ENDED) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Goal Options based on selection
            when (configuredGoal.type) {
                GoalType.TIME_BOUND -> {
                    TimeGoalSelector(
                        selectedSeconds = configuredGoal.targetSeconds,
                        onSelectSeconds = { viewModel.updateTargetSeconds(it) }
                    )
                }
                GoalType.DISTANCE_BOUND -> {
                    DistanceGoalSelector(
                        selectedMeters = configuredGoal.targetMeters,
                        onSelectMeters = { viewModel.updateTargetDistanceMeters(it) }
                    )
                }
                GoalType.OPEN_ENDED -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Walk as long as you'd like. The audio coach will keep alternating intervals and counting your sets until you tap Finish.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Divider()

            // Interval Configuration Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pacing Interval Scheme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Japanese standard protocol", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { showIntervalDialog = true }) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Customize")
                }
            }

            FrostedGlassCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).background(FastWalkCoral, CircleShape))
                            Text("Fast Interval (Brisk):", fontWeight = FontWeight.SemiBold)
                        }
                        Text("${settings.fastIntervalSeconds / 60} min", fontWeight = FontWeight.Bold, color = FastWalkCoral)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).background(RecoveryTeal, CircleShape))
                            Text("Recovery Interval (Normal):", fontWeight = FontWeight.SemiBold)
                        }
                        Text("${settings.normalIntervalSeconds / 60} min", fontWeight = FontWeight.Bold, color = RecoveryTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showIntervalDialog) {
        IntervalCustomizerDialog(
            currentFastSec = settings.fastIntervalSeconds,
            currentNormalSec = settings.normalIntervalSeconds,
            onDismiss = { showIntervalDialog = false },
            onSave = { fastSec, normalSec ->
                viewModel.updateIntervalDurations(fastSec, normalSec)
                showIntervalDialog = false
            }
        )
    }
}

@Composable
private fun GoalTypeTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TimeGoalSelector(
    selectedSeconds: Int,
    onSelectSeconds: (Int) -> Unit
) {
    val presets = listOf(
        18 * 60 to "18 min (3 sets)",
        30 * 60 to "30 min (5 sets)",
        42 * 60 to "42 min (7 sets)",
        60 * 60 to "60 min (10 sets)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (sec, label) ->
            val isSelected = selectedSeconds == sec
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) ForestGreen else MaterialTheme.colorScheme.outlineVariant),
                color = if (isSelected) ForestGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSeconds(sec) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceGoalSelector(
    selectedMeters: Double,
    onSelectMeters: (Double) -> Unit
) {
    val presets = listOf(
        2000.0 to "2.0 km (~20 min)",
        3000.0 to "3.0 km (~30 min)",
        5000.0 to "5.0 km (~50 min)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (meters, label) ->
            val isSelected = selectedMeters == meters
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) ForestGreen else MaterialTheme.colorScheme.outlineVariant),
                color = if (isSelected) ForestGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMeters(meters) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalCustomizerDialog(
    currentFastSec: Int,
    currentNormalSec: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var fastMinutes by remember { mutableStateOf(currentFastSec / 60) }
    var normalMinutes by remember { mutableStateOf(currentNormalSec / 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Interval Durations") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Standard Japanese walking uses 3 minutes fast and 3 minutes normal. You can adapt this for your fitness level:",
                    style = MaterialTheme.typography.bodySmall
                )

                // Fast Walk Minutes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fast Walk (Brisk):")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { if (fastMinutes > 1) fastMinutes-- }) { Text("-") }
                        Text("$fastMinutes min", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = { if (fastMinutes < 10) fastMinutes++ }) { Text("+") }
                    }
                }

                // Normal Walk Minutes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Normal Walk:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { if (normalMinutes > 1) normalMinutes-- }) { Text("-") }
                        Text("$normalMinutes min", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = { if (normalMinutes < 10) normalMinutes++ }) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(fastMinutes * 60, normalMinutes * 60) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
