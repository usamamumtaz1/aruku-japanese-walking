package com.japanesewalking.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japanesewalking.app.data.model.GoalType
import com.japanesewalking.app.ui.theme.FastWalkCoral
import com.japanesewalking.app.ui.theme.ForestGreen
import com.japanesewalking.app.ui.theme.FrostedGlassCard
import com.japanesewalking.app.ui.theme.RecoveryTeal
import com.japanesewalking.app.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onStartWorkoutClick: () -> Unit,
    onEducationClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val totalCount by viewModel.totalWorkoutsCount.collectAsState()
    val totalSec by viewModel.totalDurationSeconds.collectAsState()
    val totalDist by viewModel.totalDistanceMeters.collectAsState()
    val latestSession by viewModel.latestSession.collectAsState()

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Ohayo! Good morning ☀️"
        in 12..17 -> "Konnichiwa! Afternoon 🌿"
        else -> "Konbanwa! Evening 🌙"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Japanese Red Emblem with '歩'
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("歩", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Column {
                            Text("Japanese Walking", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Interval Walking Training (IWT)", style = MaterialTheme.typography.labelSmall, color = ForestGreen)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onEducationClick) {
                        Icon(Icons.Default.School, contentDescription = "Learn Technique")
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Friendly Time Greeting
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            // Hero Expressive Card: 30-min Standard Workout
            FrostedGlassCard(
                accentTint = ForestGreen,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    "Dr. Hiroshi Nose Protocol",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                "30-Min Interval Walk",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "3 min fast • 3 min moderate\n5 sets • Headphone coach ready",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onStartWorkoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = ForestGreen
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Walking Session", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }

            // Weekly Routine Target Widget (4 Days/Week Habit)
            FrostedGlassCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Weekly Habit Tracker", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Target: 4 sessions / week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "$totalCount / 4 done",
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Weekdays pill indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        days.forEachIndexed { index, day ->
                            val isCompleted = index < totalCount
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCompleted) ForestGreen else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        day,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Workout Modes Grid
            Text("Choose Your Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickModeCard(
                    title = "Express",
                    subtitle = "18 min • 3 sets",
                    icon = Icons.Default.Timer,
                    badgeColor = FastWalkCoral,
                    onClick = {
                        viewModel.updateGoalType(GoalType.TIME_BOUND)
                        viewModel.updateTargetSeconds(18 * 60)
                        onStartWorkoutClick()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickModeCard(
                    title = "Standard",
                    subtitle = "30 min • 5 sets",
                    icon = Icons.Default.FitnessCenter,
                    badgeColor = ForestGreen,
                    onClick = {
                        viewModel.updateGoalType(GoalType.TIME_BOUND)
                        viewModel.updateTargetSeconds(30 * 60)
                        onStartWorkoutClick()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickModeCard(
                    title = "Free Walk",
                    subtitle = "Open-ended",
                    icon = Icons.Default.DirectionsWalk,
                    badgeColor = RecoveryTeal,
                    onClick = {
                        viewModel.updateGoalType(GoalType.OPEN_ENDED)
                        onStartWorkoutClick()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Lifetime Progress Row
            Text("Your Lifetime Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(
                    label = "Sessions",
                    value = "$totalCount",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Distance",
                    value = "${String.format("%.1f", (totalDist ?: 0.0) / 1000.0)} km",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Time Active",
                    value = "${(totalSec ?: 0L) / 60} min",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Daily Technique Form Tip Card
            FrostedGlassCard(
                accentTint = FastWalkCoral,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(FastWalkCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Form Tip of the Day", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FastWalkCoral)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Drive with your elbows bent at 90°. A rhythmic backward arm swing increases your fast stride length by 15% naturally!",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Interactive Why It Works Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Why Japanese Walking Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onEducationClick) {
                    Text("Learn More")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScienceBenefitCard(
                    badge = "Cardio Health",
                    title = "Drops Blood Pressure by 9-10 mmHg",
                    desc = "Proven in Dr. Nose’s Shinshu University medical studies over 5 months of interval training.",
                    color = Color(0xFFD32F2F)
                )
                ScienceBenefitCard(
                    badge = "Muscle Power",
                    title = "+13% Increase in Thigh Strength",
                    desc = "Brisk intervals recruit powerful fast-twitch muscle fibers, protecting aging knee joints.",
                    color = ForestGreen
                )
                ScienceBenefitCard(
                    badge = "Longevity",
                    title = "4x Greater Aerobic Fitness (VO2 Max)",
                    desc = "Far outperforms 10,000 continuous slow steps for metabolic stamina and health.",
                    color = Color(0xFF1976D2)
                )
            }

            // Latest Completed Session Card (if exists)
            if (latestSession != null) {
                Text("Latest Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                FrostedGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dateFormat = SimpleDateFormat("EEEE, MMM d • h:mm a", Locale.getDefault())
                    val dateStr = dateFormat.format(Date(latestSession!!.timestamp))

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${String.format("%.2f", latestSession!!.distanceMeters / 1000.0)} km",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${latestSession!!.completedSets} sets completed",
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreen
                            )
                            Text(
                                "${latestSession!!.durationSeconds / 60} min",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(18.dp),
        accentTint = badgeColor,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScienceBenefitCard(
    badge: String,
    title: String,
    desc: String,
    color: Color
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(18.dp),
        accentTint = color,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Text(badge, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
