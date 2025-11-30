package com.example.todoapp.ui.screens

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(0) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var usageStatsPermissionGranted by remember { mutableStateOf(checkUsageStatsPermission(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }

    val pages = listOf(
        OnboardingPage(
            title = "Welcome to StudyBuddy! 🧠",
            description = "Your AI-powered study companion that helps you build better learning habits and stay focused.",
            icon = Icons.Default.School,
            gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
        ),
        OnboardingPage(
            title = "Set Goals & Track Progress 🎯",
            description = "Create learning goals, track your daily study time, and build streaks to stay motivated.",
            icon = Icons.Default.Flag,
            gradientColors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
        ),
        OnboardingPage(
            title = "AI Study Coach 🤖",
            description = "Get personalized advice, study tips, and motivation from your AI assistant powered by Google Gemini.",
            icon = Icons.Default.Psychology,
            gradientColors = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
        ),
        OnboardingPage(
            title = "Stay Focused 📵",
            description = "We'll gently remind you when you've been on your phone too long and haven't studied yet.",
            icon = Icons.Default.PhoneAndroid,
            gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
        ),
        OnboardingPage(
            title = "Enable Notifications 🔔",
            description = "Allow notifications to receive study reminders and motivational nudges throughout the day.",
            icon = Icons.Default.Notifications,
            gradientColors = listOf(Color(0xFFfa709a), Color(0xFFfee140))
        ),
        OnboardingPage(
            title = "Phone Usage Tracking 📊",
            description = "Grant usage access to help us remind you when it's time to study instead of scrolling!",
            icon = Icons.Default.DataUsage,
            gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = pages[currentPage].gradientColors
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentPage < pages.size - 1) {
                    TextButton(onClick = onComplete) {
                        Text("Skip", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                }
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated Icon
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pages[page].icon,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = pages[page].title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = pages[page].description,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    // Permission buttons for specific pages
                    if (page == 4) { // Notifications page
                        Spacer(modifier = Modifier.height(24.dp))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = pages[page].gradientColors[0]
                                ),
                                enabled = !notificationPermissionGranted
                            ) {
                                Icon(
                                    if (notificationPermissionGranted) Icons.Default.Check else Icons.Default.Notifications,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (notificationPermissionGranted) "Permission Granted!" else "Enable Notifications")
                            }
                        } else {
                            Text(
                                "✅ Notifications enabled by default",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (page == 5) { // Usage stats page
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = pages[page].gradientColors[0]
                            ),
                            enabled = !usageStatsPermissionGranted
                        ) {
                            Icon(
                                if (usageStatsPermissionGranted) Icons.Default.Check else Icons.Default.Settings,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (usageStatsPermissionGranted) "Permission Granted!" else "Open Settings")
                        }
                        
                        // Check permission when returning from settings
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(1000)
                                usageStatsPermissionGranted = checkUsageStatsPermission(context)
                            }
                        }
                    }
                }
            }

            // Bottom section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = { currentPage-- },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                            )
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = pages[currentPage].gradientColors[0]
                        )
                    ) {
                        Text(if (currentPage == pages.size - 1) "Get Started!" else "Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (currentPage == pages.size - 1) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
