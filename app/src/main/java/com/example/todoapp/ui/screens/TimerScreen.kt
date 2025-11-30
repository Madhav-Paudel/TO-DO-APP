package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.viewmodels.TimerMode
import com.example.todoapp.ui.viewmodels.TimerState
import com.example.todoapp.ui.viewmodels.TimerUiState
import com.example.todoapp.ui.viewmodels.TimerViewModel
import com.example.todoapp.util.MotivationalQuotes
import kotlin.math.cos
import kotlin.math.sin

// Calm color palette
private val CalmBlue = Color(0xFF5B9BD5)
private val CalmPurple = Color(0xFF7C4DFF)
private val CalmTeal = Color(0xFF00BCD4)
private val CalmGreen = Color(0xFF4CAF50)
private val CalmOrange = Color(0xFFFF9800)
private val SoftShadow = Color(0x20000000)
private val BackgroundGradientStart = Color(0xFFF5F7FA)
private val BackgroundGradientEnd = Color(0xFFE8EDF5)
private val DarkBackgroundStart = Color(0xFF1A1A2E)
private val DarkBackgroundEnd = Color(0xFF16213E)

@Composable
fun TimerScreen(
    goalId: Long,
    onStop: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val studyTip = remember { MotivationalQuotes.getRandomTip() }
    
    var showModeSelector by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    
    // Initialize timer with goal
    LaunchedEffect(goalId) {
        viewModel.initializeTimer(goalId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            TopBar(
                goalTitle = uiState.goalTitle,
                onClose = {
                    if (uiState.state != TimerState.IDLE) {
                        viewModel.stopTimer()
                    }
                    onStop()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mode Toggle
            ModeToggle(
                currentMode = uiState.mode,
                isEnabled = uiState.state == TimerState.IDLE,
                onModeChange = { viewModel.setTimerMode(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Daily Goal Progress
            DailyGoalIndicator(
                targetMinutes = uiState.dailyTargetMinutes,
                completedMinutes = uiState.todayMinutesDone + (uiState.elapsedSeconds / 60).toInt(),
                progress = uiState.progressToTarget
            )
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Main Timer Circle
            TimerCircle(
                uiState = uiState,
                onDurationClick = {
                    if (uiState.state == TimerState.IDLE && uiState.mode == TimerMode.COUNTDOWN) {
                        showDurationPicker = true
                    }
                }
            )
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Quick Duration Presets (Countdown mode only)
            if (uiState.mode == TimerMode.COUNTDOWN && uiState.state == TimerState.IDLE) {
                DurationPresets(
                    selectedMinutes = (uiState.countdownTotalSeconds / 60).toInt(),
                    onSelect = { viewModel.setCountdownDuration(it) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Study Tip Card
            StudyTipCard(tip = studyTip)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Control Buttons
            ControlButtons(
                state = uiState.state,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.resetTimer() },
                onStop = {
                    viewModel.stopTimer()
                    onStop()
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Completion Dialog
    if (uiState.showCompletionDialog) {
        CompletionDialog(
            minutes = (uiState.elapsedSeconds / 60).toInt(),
            onDismiss = { viewModel.dismissCompletionDialog() },
            onContinue = {
                viewModel.dismissCompletionDialog()
                viewModel.resetTimer()
            },
            onFinish = {
                viewModel.dismissCompletionDialog()
                viewModel.resetTimer()
                onStop()
            }
        )
    }
    
    // Duration Picker Dialog
    if (showDurationPicker) {
        DurationPickerDialog(
            currentMinutes = (uiState.countdownTotalSeconds / 60).toInt(),
            onSelect = { 
                viewModel.setCountdownDuration(it)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
    }
}

@Composable
private fun TopBar(
    goalTitle: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Focus Session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )
            if (goalTitle.isNotEmpty()) {
                Text(
                    text = goalTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF718096)
                )
            }
        }
        
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF718096)
            )
        }
    }
}

@Composable
private fun ModeToggle(
    currentMode: TimerMode,
    isEnabled: Boolean,
    onModeChange: (TimerMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModeButton(
                icon = Icons.Outlined.Timer,
                label = "Stopwatch",
                isSelected = currentMode == TimerMode.STOPWATCH,
                isEnabled = isEnabled,
                onClick = { onModeChange(TimerMode.STOPWATCH) },
                modifier = Modifier.weight(1f)
            )
            
            ModeButton(
                icon = Icons.Outlined.HourglassEmpty,
                label = "Countdown",
                isSelected = currentMode == TimerMode.COUNTDOWN,
                isEnabled = isEnabled,
                onClick = { onModeChange(TimerMode.COUNTDOWN) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CalmPurple else Color.Transparent,
        label = "bg_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF718096),
        label = "content_color"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = isEnabled) { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DailyGoalIndicator(
    targetMinutes: Int,
    completedMinutes: Int,
    progress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = CalmPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily Goal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D3748)
                    )
                }
                
                Text(
                    text = "$completedMinutes / $targetMinutes min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress >= 1f) CalmGreen else CalmPurple
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(500),
                    label = "progress"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (progress >= 1f) {
                                Brush.horizontalGradient(listOf(CalmGreen, Color(0xFF81C784)))
                            } else {
                                Brush.horizontalGradient(listOf(CalmPurple, CalmBlue))
                            }
                        )
                )
            }
            
            if (progress >= 1f) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CalmGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Daily goal achieved! 🎉",
                        style = MaterialTheme.typography.bodySmall,
                        color = CalmGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerCircle(
    uiState: TimerUiState,
    onDurationClick: () -> Unit
) {
    val isRunning = uiState.state == TimerState.RUNNING
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "timer_anim")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val tickRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .scale(if (isRunning) pulseScale else 1f)
            .clickable(enabled = uiState.mode == TimerMode.COUNTDOWN && uiState.state == TimerState.IDLE) {
                onDurationClick()
            }
    ) {
        // Outer glow (when running)
        if (isRunning) {
            Canvas(modifier = Modifier.size(300.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CalmPurple.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2
                )
            }
        }
        
        // Main timer ring
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Background ring
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress calculation
            val progress = when (uiState.mode) {
                TimerMode.STOPWATCH -> {
                    // Full circle every 25 minutes for Pomodoro visualization
                    (uiState.elapsedSeconds % 1500) / 1500f
                }
                TimerMode.COUNTDOWN -> {
                    if (uiState.countdownTotalSeconds > 0) {
                        1f - (uiState.remainingSeconds.toFloat() / uiState.countdownTotalSeconds)
                    } else 0f
                }
            }
            
            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        CalmPurple,
                        CalmBlue,
                        CalmTeal,
                        CalmPurple
                    )
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )
            
            // Tick marks
            val tickCount = 60
            for (i in 0 until tickCount) {
                val angle = Math.toRadians((i * 6 - 90).toDouble())
                val isLargeTick = i % 5 == 0
                val tickLength = if (isLargeTick) 12.dp.toPx() else 6.dp.toPx()
                val tickWidth = if (isLargeTick) 2.dp.toPx() else 1.dp.toPx()
                
                val innerRadius = radius - strokeWidth / 2 - 8.dp.toPx()
                val outerRadius = innerRadius - tickLength
                
                val startX = center.x + innerRadius * cos(angle).toFloat()
                val startY = center.y + innerRadius * sin(angle).toFloat()
                val endX = center.x + outerRadius * cos(angle).toFloat()
                val endY = center.y + outerRadius * sin(angle).toFloat()
                
                drawLine(
                    color = Color(0xFFCBD5E0),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }
            
            // Moving second hand (when running)
            if (isRunning) {
                rotate(tickRotation, center) {
                    drawLine(
                        color = CalmOrange,
                        start = center,
                        end = Offset(center.x, center.y - radius + strokeWidth + 20.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Hand center dot
                    drawCircle(
                        color = CalmOrange,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }
            }
        }
        
        // Timer display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time display
            val displayTime = when (uiState.mode) {
                TimerMode.STOPWATCH -> formatTime(uiState.elapsedSeconds)
                TimerMode.COUNTDOWN -> formatTime(uiState.remainingSeconds)
            }
            
            Text(
                text = displayTime,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF2D3748),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Status text
            val statusText = when {
                uiState.state == TimerState.IDLE && uiState.mode == TimerMode.COUNTDOWN -> "Tap to set duration"
                uiState.state == TimerState.IDLE -> "Ready to start"
                uiState.state == TimerState.RUNNING -> "Stay focused! 🎯"
                uiState.state == TimerState.PAUSED -> "Paused ⏸️"
                uiState.state == TimerState.COMPLETED -> "Completed! 🎉"
                else -> ""
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF718096)
            )
            
            // Minutes indicator
            if (uiState.state == TimerState.RUNNING || uiState.state == TimerState.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.elapsedSeconds / 60} min studied",
                    style = MaterialTheme.typography.bodySmall,
                    color = CalmPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DurationPresets(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    val presets = listOf(15, 25, 30, 45, 60)
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { minutes ->
            val isSelected = selectedMinutes == minutes
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) CalmPurple else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CalmPurple else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(minutes) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFF718096)
                )
            }
        }
    }
}

@Composable
private fun StudyTipCard(tip: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CalmPurple.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = CalmPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4A5568)
            )
        }
    }
}

@Composable
private fun ControlButtons(
    state: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        AnimatedVisibility(
            visible = state != TimerState.IDLE,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = onReset,
                containerColor = Color.White,
                contentColor = Color(0xFF718096),
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Main Play/Pause button
        val buttonScale by animateFloatAsState(
            targetValue = if (state == TimerState.RUNNING) 1.1f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "button_scale"
        )
        
        FloatingActionButton(
            onClick = {
                when (state) {
                    TimerState.IDLE, TimerState.PAUSED -> onStart()
                    TimerState.RUNNING -> onPause()
                    TimerState.COMPLETED -> onReset()
                }
            },
            containerColor = when (state) {
                TimerState.RUNNING -> CalmOrange
                else -> CalmPurple
            },
            contentColor = Color.White,
            modifier = Modifier
                .size(80.dp)
                .scale(buttonScale)
                .shadow(8.dp, CircleShape)
        ) {
            Icon(
                imageVector = when (state) {
                    TimerState.RUNNING -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when (state) {
                    TimerState.RUNNING -> "Pause"
                    else -> "Start"
                },
                modifier = Modifier.size(36.dp)
            )
        }
        
        // Stop button
        AnimatedVisibility(
            visible = state != TimerState.IDLE,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = onStop,
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFE53935),
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CompletionDialog(
    minutes: Int,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit
) {
    val celebration = remember { MotivationalQuotes.getRandomCelebration() }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated emoji
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "emoji_scale"
                )
                
                Text(
                    text = "🎉",
                    fontSize = 72.sp,
                    modifier = Modifier.scale(scale)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Session Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You focused for $minutes minutes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF718096)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = celebration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CalmPurple,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onContinue,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CalmPurple
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue")
                    }
                    
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalmPurple
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish")
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationPickerDialog(
    currentMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(currentMinutes) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Duration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Duration slider
                Text(
                    text = "$selectedMinutes minutes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light,
                    color = CalmPurple
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.toInt() },
                    valueRange = 5f..120f,
                    steps = 22, // 5-minute intervals
                    colors = SliderDefaults.colors(
                        thumbColor = CalmPurple,
                        activeTrackColor = CalmPurple
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Quick presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 45, 60, 90).forEach { mins ->
                        AssistChip(
                            onClick = { selectedMinutes = mins },
                            label = { Text("${mins}m") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedMinutes == mins) CalmPurple.copy(alpha = 0.1f) else Color.Transparent
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSelect(selectedMinutes) },
                        colors = ButtonDefaults.buttonColors(containerColor = CalmPurple)
                    ) {
                        Text("Set")
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
