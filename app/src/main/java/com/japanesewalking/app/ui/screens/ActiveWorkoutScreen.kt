package com.japanesewalking.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japanesewalking.app.data.model.GoalType
import com.japanesewalking.app.data.model.IntervalType
import com.japanesewalking.app.data.model.SessionStatus
import com.japanesewalking.app.ui.theme.FastWalkCoral
import com.japanesewalking.app.ui.theme.ForestGreen
import com.japanesewalking.app.ui.theme.FrostedGlassCard
import com.japanesewalking.app.ui.theme.RecoveryTeal
import com.japanesewalking.app.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    onWorkoutFinished: () -> Unit
) {
    val state by viewModel.liveState.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }

    val isFast = state.currentInterval == IntervalType.FAST
    val activeColor by animateColorAsState(
        targetValue = if (isFast) FastWalkCoral else RecoveryTeal,
        animationSpec = tween(durationMillis = 500),
        label = "IntervalColor"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Japanese Walking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (state.goal.type == GoalType.OPEN_ENDED) "Free Walk" else "Target: ${state.goal.type.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEndDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "End Walk")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interval Status Banner with Frosted Glass
                FrostedGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    accentTint = activeColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(activeColor, CircleShape))
                        Text(
                            text = if (isFast) "⚡ BRISK WALK (FAST)" else "🌿 RECOVERY WALK (NORMAL)",
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            fontSize = 15.sp
                        )
                    }
                }

                // Interval Countdown Ring & Timer
                val progress = if (state.intervalTotalSeconds > 0) {
                    (state.intervalRemainingSeconds.toFloat() / state.intervalTotalSeconds.toFloat()).coerceIn(0f, 1f)
                } else 1f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(230.dp)
                        .padding(12.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 14.dp,
                        color = activeColor,
                        trackColor = activeColor.copy(alpha = 0.15f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(state.intervalRemainingSeconds),
                            fontSize = 50.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColor
                        )
                        Text(
                            text = "Remaining in interval",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set ${state.completedSets + 1}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Coach Pacing Instruction
                Text(
                    text = if (isFast) "Walk briskly at ~70% effort. Swing arms backward." else "Slow down, breathe deeply, and recover steady pace.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Metrics Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            label = "Distance",
                            value = "${String.format("%.2f", state.distanceMeters / 1000.0)} km",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Total Time",
                            value = formatTime(state.totalElapsedSeconds.toInt()),
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            label = "Calories",
                            value = "${state.caloriesKcal} kcal",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Heart Rate Zone",
                            value = "~${state.estimatedHeartRateBpm} bpm",
                            icon = Icons.Default.Favorite,
                            iconColor = Color(0xFFE53935),
                            subtitle = if (isFast) "Zone 2 (Aerobic)" else "Zone 1 (Warm)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Controls: Pause / Resume & Finish
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                FilledIconButton(
                    onClick = {
                        if (state.status == SessionStatus.RUNNING) {
                            viewModel.pauseWorkout()
                        } else {
                            viewModel.resumeWorkout()
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.status == SessionStatus.RUNNING) MaterialTheme.colorScheme.surfaceVariant else ForestGreen
                    )
                ) {
                    Icon(
                        imageVector = if (state.status == SessionStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause/Resume",
                        modifier = Modifier.size(36.dp),
                        tint = if (state.status == SessionStatus.RUNNING) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                }

                // Finish Workout Button
                FilledIconButton(
                    onClick = { showEndDialog = true },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = FastWalkCoral
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Workout",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("Finish Walking Session?") },
            text = { Text("Are you ready to complete this session and view your summary breakdown?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEndDialog = false
                        viewModel.stopWorkout()
                        onWorkoutFinished()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Finish & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Keep Walking")
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = ForestGreen,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
