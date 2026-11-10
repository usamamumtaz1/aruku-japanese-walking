package com.japanesewalking.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japanesewalking.app.ui.theme.FastWalkCoral
import com.japanesewalking.app.ui.theme.ForestGreen
import com.japanesewalking.app.ui.theme.FrostedGlassCard
import com.japanesewalking.app.ui.theme.RecoveryTeal
import com.japanesewalking.app.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    viewModel: WorkoutViewModel,
    onDoneClick: () -> Unit
) {
    val session by viewModel.summarySession.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Done & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Celebration Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(ForestGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Text("Session Completed!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "You successfully completed your Japanese interval walking session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Primary Metrics Frosted Glass Card
            FrostedGlassCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        SummaryStat(
                            title = "Total Time",
                            value = formatTime(session?.durationSeconds?.toInt() ?: 0)
                        )
                        SummaryStat(
                            title = "Distance",
                            value = "${String.format("%.2f", (session?.distanceMeters ?: 0.0) / 1000.0)} km"
                        )
                    }
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        SummaryStat(
                            title = "Sets Finished",
                            value = "${session?.completedSets ?: 0} sets"
                        )
                        SummaryStat(
                            title = "Calories",
                            value = "${session?.caloriesKcal ?: 0} kcal"
                        )
                    }
                }
            }

            // Interval Breakdown
            Text("Interval Effort Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val fastSec = session?.fastDurationSeconds ?: 0L
            val normalSec = session?.normalDurationSeconds ?: 0L
            val totalSec = (fastSec + normalSec).coerceAtLeast(1L).toFloat()
            val fastFraction = (fastSec.toFloat() / totalSec).coerceIn(0f, 1f)
            val normalFraction = (normalSec.toFloat() / totalSec).coerceIn(0f, 1f)

            FrostedGlassCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Split visual bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(fastFraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(FastWalkCoral)
                        )
                        Box(
                            modifier = Modifier
                                .weight(normalFraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(RecoveryTeal)
                        )
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(FastWalkCoral, CircleShape))
                            Text("Fast: ${formatTime(session?.fastDurationSeconds?.toInt() ?: 0)} (${(fastFraction * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(RecoveryTeal, CircleShape))
                            Text("Normal: ${formatTime(session?.normalDurationSeconds?.toInt() ?: 0)} (${(normalFraction * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Health Benefit Highlight
            FrostedGlassCard(
                shape = RoundedCornerShape(18.dp),
                accentTint = ForestGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = ForestGreen)
                    Text(
                        text = "Great job! Alternating fast and slow intervals activates both aerobic capacity and leg muscle power much more than a constant walk.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryStat(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
