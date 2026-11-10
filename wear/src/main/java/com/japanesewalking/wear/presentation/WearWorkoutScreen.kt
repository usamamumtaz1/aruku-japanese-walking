package com.japanesewalking.wear.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.japanesewalking.wear.datalayer.WearCommandSender
import com.japanesewalking.wear.datalayer.WearDataListenerService
import com.japanesewalking.wear.datalayer.WearWorkoutManager

// Japanese Walking Signature Palettes
val ToriiRed = Color(0xFFE53935)
val ZenTeal = Color(0xFF00897B)
val DeepCharcoal = Color(0xFF121212)
val PureBlack = Color(0xFF000000)
val SoftWhite = Color(0xFFF5F5F5)
val AmberGold = Color(0xFFFFB300)

@Composable
fun WearWorkoutScreen() {
    val context = LocalContext.current
    val workoutState by WearWorkoutManager.workoutState.collectAsState()

    val isRunning = workoutState.status == "RUNNING"
    val isPaused = workoutState.status == "PAUSED"
    val isActive = isRunning || isPaused

    val isFast = workoutState.intervalType == "FAST"
    val phaseColor by animateColorAsState(
        targetValue = if (isFast) ToriiRed else ZenTeal,
        label = "phaseColor"
    )

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        if (!isActive) {
            // Idle Screen - Ready to Walk
            IdleWorkoutView(
                onStartClick = {
                    WearWorkoutManager.startWorkout(context)
                }
            )
        } else {
            // Active Workout Circular Dashboard
            ActiveWorkoutView(
                isFast = isFast,
                isPaused = isPaused,
                remainingSeconds = workoutState.remainingSeconds,
                totalIntervalSeconds = workoutState.intervalTotalSeconds,
                completedSets = workoutState.completedSets,
                distanceMeters = workoutState.distanceMeters,
                phaseColor = phaseColor,
                onPauseResumeClick = {
                    if (isRunning) {
                        WearWorkoutManager.pauseWorkout(context)
                    } else {
                        WearWorkoutManager.resumeWorkout(context)
                    }
                },
                onSkipClick = {
                    WearWorkoutManager.skipInterval(context)
                },
                onStopClick = {
                    WearWorkoutManager.stopWorkout(context)
                }
            )
        }
    }
}

@Composable
fun IdleWorkoutView(onStartClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "歩",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ToriiRed
            )
            Text(
                text = "ARUKU",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = SoftWhite
            )
            Text(
                text = "30-Min Japanese Walk",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ToriiRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = "START WALK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ActiveWorkoutView(
    isFast: Boolean,
    isPaused: Boolean,
    remainingSeconds: Int,
    totalIntervalSeconds: Int,
    completedSets: Int,
    distanceMeters: Double,
    phaseColor: Color,
    onPauseResumeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val progress = if (totalIntervalSeconds > 0) {
        (totalIntervalSeconds - remainingSeconds).toFloat() / totalIntervalSeconds.toFloat()
    } else 0f

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val distanceKm = String.format("%.2f km", distanceMeters / 1000.0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Progress Ring
        CircularProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
            indicatorColor = if (isPaused) AmberGold else phaseColor,
            trackColor = Color(0xFF222222),
            strokeWidth = 6.dp
        )

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Interval Badge Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPaused) AmberGold.copy(alpha = 0.2f) else phaseColor.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPaused) "PAUSED" else if (isFast) "⚡ BRISK (70%)" else "🌿 RECOVERY",
                    color = if (isPaused) AmberGold else phaseColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Big Countdown Timer
            Text(
                text = timeFormatted,
                fontSize = 38.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                textAlign = TextAlign.Center
            )

            // Sets & Distance Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Set ${completedSets + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
                Text(
                    text = " • ",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = distanceKm,
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wrist Control Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume
                Button(
                    onClick = onPauseResumeClick,
                    modifier = Modifier.size(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isPaused) ToriiRed else Color(0xFF2C2C2E),
                        contentColor = Color.White
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isPaused) "▶" else "❚❚",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Skip Interval
                Button(
                    onClick = onSkipClick,
                    modifier = Modifier.size(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2C2C2E),
                        contentColor = Color.White
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = "⏭",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Finish Workout
                Button(
                    onClick = onStopClick,
                    modifier = Modifier.size(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2C2C2E),
                        contentColor = Color(0xFFFF5252)
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = "✕",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
