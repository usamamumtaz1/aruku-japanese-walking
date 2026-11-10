package com.japanesewalking.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japanesewalking.app.ui.theme.*

data class TourStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val scientificBenefit: String,
    val icon: ImageVector,
    val accentColor: Color
)

private val TOUR_STEPS = listOf(
    TourStep(
        title = "The 3-Minute Japanese Interval Method",
        subtitle = "Pioneered at Shinshu University",
        description = "Alternate 3 minutes of brisk walking (high aerobic intensity) with 3 minutes of gentle walking (recovery) for 5 cycles (30 minutes total).",
        scientificBenefit = "Clinically proven to drop blood pressure, increase thigh muscle strength by 13%, and boost VO2 max.",
        icon = Icons.Default.DirectionsWalk,
        accentColor = ForestGreen
    ),
    TourStep(
        title = "Hands-Free Voice Coach in Your Pocket",
        subtitle = "Audio Guidance with Music Ducking",
        description = "Put your phone in your pocket! Spoken announcements alert you at every interval switch, 5-second countdown, and halfway mark.",
        scientificBenefit = "Background pocket service with audio ducking smoothly lowers Spotify and podcasts while the coach speaks.",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        accentColor = FastWalkCoral
    ),
    TourStep(
        title = "Real-Time Pace & Calorie Tracking",
        subtitle = "Scientific Precision on Every Walk",
        description = "Track cadence, live distance, pace, and estimated calorie burn in high contrast, whether walking under direct sun or night skies.",
        scientificBenefit = "Built-in pocket wakelock and hardware alarms keep timing 100% accurate even when the screen is locked.",
        icon = Icons.Default.Speed,
        accentColor = RecoveryTeal
    ),
    TourStep(
        title = "Instant 2x2 Home Screen Widget",
        subtitle = "One-Tap Launch",
        description = "Place our frosted glass 2x2 widget directly on your Android home screen to start your daily 30-minute interval walk with a single tap.",
        scientificBenefit = "Zero friction between you and your daily health goal — no navigation or menu digging needed.",
        icon = Icons.Default.Widgets,
        accentColor = Color(0xFFD97706)
    )
)

@Composable
fun InteractiveTourScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = TOUR_STEPS[currentStep]
    val isLast = currentStep == TOUR_STEPS.size - 1

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TOUR_STEPS.indices.forEach { index ->
                        val isCurrent = index == currentStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isCurrent) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) step.accentColor
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // Skip Button
                TextButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                ) {
                    Text("Skip", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area with Animated Transition
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "TourSlideTransition"
            ) { stepIdx ->
                val currentTourStep = TOUR_STEPS[stepIdx]

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Icon with Ambient Halo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        currentTourStep.accentColor.copy(alpha = 0.25f),
                                        currentTourStep.accentColor.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(currentTourStep.accentColor)
                        ) {
                            Icon(
                                imageVector = currentTourStep.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Subtitle / Scientific Tag
                    Surface(
                        color = currentTourStep.accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = currentTourStep.subtitle.uppercase(),
                            color = currentTourStep.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Title
                    Text(
                        text = currentTourStep.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description Card
                    FrostedGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentTourStep.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )

                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = currentTourStep.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = currentTourStep.scientificBenefit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Controls (Back / Next / Get Started)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (isLast) {
                            onComplete()
                        } else {
                            currentStep += 1
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLast) ForestGreen else step.accentColor
                    ),
                    modifier = Modifier
                        .weight(if (currentStep > 0) 2f else 1f)
                        .height(54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isLast) "Get Started" else "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
