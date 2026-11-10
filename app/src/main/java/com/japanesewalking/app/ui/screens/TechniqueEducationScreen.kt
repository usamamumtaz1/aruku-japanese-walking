package com.japanesewalking.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japanesewalking.app.ui.theme.FastWalkCoral
import com.japanesewalking.app.ui.theme.ForestGreen
import com.japanesewalking.app.ui.theme.RecoveryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueEducationScreen(
    onBackClick: () -> Unit,
    onStartWalkClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Japanese Technique", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onStartWalkClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Try a Japanese Walking Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Science Intro Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Interval Walking Training (IWT)",
                        style = MaterialTheme.typography.titleLarge,
                        color = ForestGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Developed by Dr. Hiroshi Nose and Dr. Shizue Masuki at Shinshu University Graduate School of Medicine in Nagano, Japan. Based on over two decades of clinical sports science trials.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }

            // The 3-Minute Rule Card
            Text(
                text = "How the 3-Minute Cycle Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fast Walk Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FastWalkCoral.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(FastWalkCoral, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3m", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Brisk Pace", fontWeight = FontWeight.Bold, color = FastWalkCoral)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "~70% peak effort. Walking fast enough that full conversation feels breathless.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Normal Walk Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RecoveryTeal.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(RecoveryTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3m", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Normal Pace", fontWeight = FontWeight.Bold, color = RecoveryTeal)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "~40% effort. Relaxed stroll for recovery and steady breathing.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Proven Benefits Section
            Text(
                text = "Clinically Proven Benefits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            BenefitItem(
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFE53935),
                title = "Cardiovascular & Blood Pressure",
                description = "Studies in Japan showed a drop of 9–10 mmHg in systolic blood pressure among hypertensive participants."
            )

            BenefitItem(
                icon = Icons.Default.FitnessCenter,
                iconColor = ForestGreen,
                title = "Thigh & Core Strength (+13%)",
                description = "Alternating intensity builds knee extension and flexion power, preserving joint mobility and preventing sarcopenia."
            )

            BenefitItem(
                icon = Icons.Default.Speed,
                iconColor = Color(0xFF1E88E5),
                title = "Aerobic Capacity Boost (VO2 Max)",
                description = "4x more effective at increasing physical endurance than continuous steady-state walking."
            )

            BenefitItem(
                icon = Icons.Default.Timer,
                iconColor = Color(0xFFFB8C00),
                title = "Time-Efficient (30 Minutes)",
                description = "Just 5 sets (30 minutes total) 4 days a week yields greater metabolic improvements than 10,000 continuous slow steps."
            )

            // Posture & Form Tips
            Text(
                text = "Posture & Form Instructions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PostureBullet(number = "1", text = "Gaze 25 meters forward, keeping your spine straight and chin level.")
                    PostureBullet(number = "2", text = "Bend elbows at 90 degrees and swing them rhythmically backwards.")
                    PostureBullet(number = "3", text = "Land on your heels and roll smoothly forward onto your toes.")
                    PostureBullet(number = "4", text = "Take slightly longer strides during the 3-minute fast interval.")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PostureBullet(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(ForestGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
