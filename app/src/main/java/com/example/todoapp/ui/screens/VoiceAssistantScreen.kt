package com.example.todoapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.data.model.ActionTaken
import com.example.todoapp.data.model.ActionType
import com.example.todoapp.data.model.ChatMessage
import com.example.todoapp.data.model.ChatSender
import com.example.todoapp.ui.AppViewModelProvider
import com.example.todoapp.ui.viewmodels.VoiceAssistantViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern color palette
private val PrimaryPurple = Color(0xFF7C4DFF)
private val PrimaryPurpleLight = Color(0xFFB47CFF)
private val PrimaryBlue = Color(0xFF2196F3)
private val PrimaryTeal = Color(0xFF00BCD4)
private val GradientStart = Color(0xFF667eea)
private val GradientMiddle = Color(0xFF764ba2)
private val GradientEnd = Color(0xFFf093fb)
private val GlassWhite = Color(0x40FFFFFF)
private val GlassBorder = Color(0x30FFFFFF)
private val DarkBackground = Color(0xFF0D1117)
private val DarkSurface = Color(0xFF161B22)
private val DarkCard = Color(0xFF21262D)
private val UserBubbleStart = Color(0xFF667eea)
private val UserBubbleEnd = Color(0xFF764ba2)
private val AssistantBubbleStart = Color(0xFF1A1F2E)
private val AssistantBubbleEnd = Color(0xFF252D3D)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFF9800)
private val AccentRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    var hasRecordPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
        if (granted) {
            viewModel.initializeSpeechRecognizer()
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Initialize speech recognizer
    LaunchedEffect(Unit) {
        if (hasRecordPermission) {
            viewModel.initializeSpeechRecognizer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
    ) {
        // Animated background orbs
        AnimatedBackgroundOrbs()
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            VoiceAssistantHeader(
                ttsEnabled = uiState.ttsEnabled,
                onToggleTTS = { viewModel.toggleTTS(it) },
                isSpeaking = uiState.isSpeaking,
                onStopSpeaking = { viewModel.stopSpeaking() }
            )
            
            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Welcome section if empty
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeSection(
                            onSuggestionClick = { viewModel.sendMessage(it) }
                        )
                    }
                }
                
                items(uiState.messages) { message ->
                    GlassmorphicChatBubble(message)
                }
                
                // Typing indicator
                if (uiState.isLoading) {
                    item {
                        ModernTypingIndicator()
                    }
                }
            }
            
            // Quick suggestions
            if (uiState.messages.isNotEmpty() && !uiState.isLoading && !uiState.isListening) {
                QuickSuggestions(
                    onSuggestionClick = { viewModel.sendMessage(it) }
                )
            }
            
            // Input bar with mic button
            GlassInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                isLoading = uiState.isLoading,
                isListening = uiState.isListening,
                speechAmplitude = uiState.speechAmplitude,
                onMicClick = {
                    if (hasRecordPermission) {
                        if (uiState.isListening) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                hasRecordPermission = hasRecordPermission
            )
        }
        
        // Error snackbar
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            ) {
                Text(uiState.error ?: "")
            }
        }
    }
}

@Composable
private fun AnimatedBackgroundOrbs() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Purple orb
        Box(
            modifier = Modifier
                .offset(
                    x = (100 + kotlin.math.sin(Math.toRadians(offset1.toDouble())) * 50).dp,
                    y = (150 + kotlin.math.cos(Math.toRadians(offset1.toDouble())) * 30).dp
                )
                .size(200.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Blue orb
        Box(
            modifier = Modifier
                .offset(
                    x = (200 + kotlin.math.cos(Math.toRadians(offset2.toDouble())) * 40).dp,
                    y = (400 + kotlin.math.sin(Math.toRadians(offset2.toDouble())) * 50).dp
                )
                .size(180.dp)
                .blur(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun VoiceAssistantHeader(
    ttsEnabled: Boolean,
    onToggleTTS: (Boolean) -> Unit,
    isSpeaking: Boolean,
    onStopSpeaking: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val avatarGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface.copy(alpha = 0.95f),
                        DarkSurface.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated AI Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = avatarGlow
                        scaleY = avatarGlow
                    }
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = PrimaryPurple.copy(alpha = 0.5f),
                        spotColor = PrimaryPurple.copy(alpha = 0.5f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMiddle)
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nova",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your AI Productivity Coach",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // TTS toggle
            IconButton(
                onClick = {
                    if (isSpeaking) {
                        onStopSpeaking()
                    } else {
                        onToggleTTS(!ttsEnabled)
                    }
                }
            ) {
                Icon(
                    imageVector = when {
                        isSpeaking -> Icons.Default.VolumeOff
                        ttsEnabled -> Icons.Default.VolumeUp
                        else -> Icons.Outlined.VolumeOff
                    },
                    contentDescription = "Toggle TTS",
                    tint = if (ttsEnabled || isSpeaking) PrimaryPurple else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun WelcomeSection(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large animated avatar
        val infiniteTransition = rememberInfiniteTransition(label = "welcome")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = PrimaryPurple.copy(alpha = 0.4f),
                    spotColor = PrimaryPurple.copy(alpha = 0.4f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Hey there! 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = "I'm Nova, your personal productivity coach.\nHow can I help you today?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        
        // Suggestion cards with glassmorphism
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            GlassSuggestionCard(
                icon = Icons.Outlined.Flag,
                text = "Create a new goal",
                color = PrimaryPurple,
                onClick = { onSuggestionClick("Help me create a new goal") }
            )
            GlassSuggestionCard(
                icon = Icons.Outlined.Checklist,
                text = "Plan my tasks for today",
                color = PrimaryBlue,
                onClick = { onSuggestionClick("Help me plan my tasks for today") }
            )
            GlassSuggestionCard(
                icon = Icons.Outlined.TrendingUp,
                text = "How am I doing?",
                color = AccentGreen,
                onClick = { onSuggestionClick("How am I doing?") }
            )
            GlassSuggestionCard(
                icon = Icons.Outlined.Lightbulb,
                text = "Give me productivity tips",
                color = AccentOrange,
                onClick = { onSuggestionClick("Give me some productivity tips") }
            )
        }
    }
}

@Composable
private fun GlassSuggestionCard(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GlassmorphicChatBubble(message: ChatMessage) {
    val isUser = message.sender == ChatSender.USER
    val isSystem = message.sender == ChatSender.SYSTEM
    
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Assistant avatar
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = PrimaryPurple.copy(alpha = 0.3f)
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GradientStart, GradientMiddle)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSystem) Icons.Default.Settings else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            
            // Message bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .then(
                        if (isUser) {
                            Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 20.dp,
                                        bottomEnd = 4.dp
                                    ),
                                    ambientColor = PrimaryPurple.copy(alpha = 0.3f)
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(UserBubbleStart, UserBubbleEnd)
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 20.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        } else {
                            Modifier
                                .border(
                                    width = 1.dp,
                                    color = GlassBorder,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 20.dp,
                                        bottomEnd = 20.dp
                                    )
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            AssistantBubbleStart,
                                            AssistantBubbleEnd
                                        )
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 20.dp,
                                        bottomEnd = 20.dp
                                    )
                                )
                        }
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            
            // User avatar
            if (isUser) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = PrimaryBlue.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = PrimaryBlue.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Action card if present
        if (message.action != null && message.action.type != ActionType.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            ActionCard(
                action = message.action,
                modifier = Modifier.padding(start = if (!isUser) 46.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun ActionCard(action: ActionTaken, modifier: Modifier = Modifier) {
    val actionInfo = when (action.type) {
        ActionType.GOAL_CREATED -> Triple("Goal Created", Icons.Default.Flag, AccentGreen)
        ActionType.TASK_CREATED -> Triple("Task Created", Icons.Default.CheckCircle, PrimaryBlue)
        ActionType.GOAL_DELETED -> Triple("Goal Deleted", Icons.Default.Delete, AccentRed)
        ActionType.TASK_DELETED -> Triple("Task Deleted", Icons.Default.Delete, AccentRed)
        ActionType.TASK_COMPLETED -> Triple("Task Completed", Icons.Default.Done, AccentGreen)
        ActionType.LIST_SHOWN -> Triple("Data Retrieved", Icons.Default.List, PrimaryPurple)
        ActionType.NONE -> Triple("", Icons.Default.Info, Color.Gray)
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = actionInfo.third.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            actionInfo.third.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                actionInfo.second,
                contentDescription = null,
                tint = actionInfo.third,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = actionInfo.first,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = actionInfo.third
                )
                Text(
                    text = action.itemName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ModernTypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientMiddle)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Typing dots with glass effect
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AssistantBubbleStart, AssistantBubbleEnd)
                    ),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            AnimatedTypingDots()
        }
    }
}

@Composable
private fun AnimatedTypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 800
                        0f at 0
                        -8f at 200
                        0f at 400
                        0f at 800
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "dot_$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(y = animatedY.dp)
                    .size(10.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurpleLight)
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun QuickSuggestions(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "List my tasks",
        "How am I doing?",
        "Create a task",
        "Help"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.2f)
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = PrimaryPurple.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    isListening: Boolean,
    speechAmplitude: Float,
    onMicClick: () -> Unit,
    hasRecordPermission: Boolean
) {
    val hasText = inputText.isNotBlank()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.95f),
        shadowElevation = 16.dp
    ) {
        Column {
            // Listening indicator
            AnimatedVisibility(
                visible = isListening,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ListeningIndicator(amplitude = speechAmplitude)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text field with glass effect
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp),
                    placeholder = {
                        Text(
                            if (isListening) "Listening..." else "Message Nova...",
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PrimaryPurple
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    enabled = !isLoading && !isListening,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Mic button with animation
                AnimatedMicButton(
                    isListening = isListening,
                    amplitude = speechAmplitude,
                    onClick = onMicClick,
                    enabled = !isLoading,
                    hasPermission = hasRecordPermission
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button
                AnimatedVisibility(
                    visible = hasText,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = PrimaryPurple.copy(alpha = 0.3f)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GradientStart, GradientMiddle)
                                ),
                                shape = CircleShape
                            )
                            .clickable(
                                enabled = hasText && !isLoading,
                                onClick = onSend,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedMicButton(
    isListening: Boolean,
    amplitude: Float,
    onClick: () -> Unit,
    enabled: Boolean,
    hasPermission: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f + (amplitude * 0.3f) else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isListening) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(if (isListening) pulseScale else 1f)
            .shadow(
                elevation = if (isListening) 16.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isListening) AccentRed.copy(alpha = glowAlpha) else PrimaryPurple.copy(alpha = 0.2f),
                spotColor = if (isListening) AccentRed.copy(alpha = glowAlpha) else PrimaryPurple.copy(alpha = 0.2f)
            )
            .background(
                brush = if (isListening) {
                    Brush.linearGradient(
                        colors = listOf(AccentRed, AccentOrange)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(DarkCard, DarkCard)
                    )
                },
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isListening) AccentRed.copy(alpha = 0.5f) else GlassBorder,
                shape = CircleShape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isListening) "Stop listening" else "Start voice input",
            tint = if (isListening) Color.White else if (hasPermission) PrimaryPurple else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ListeningIndicator(amplitude: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentRed.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated waveform
            repeat(7) { index ->
                val infiniteTransition = rememberInfiniteTransition(label = "wave_$index")
                val height by infiniteTransition.animateFloat(
                    initialValue = 8f,
                    targetValue = 8f + (amplitude * 20f * (1f - kotlin.math.abs(index - 3) * 0.2f)),
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 200,
                            delayMillis = index * 50,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(4.dp)
                        .height(height.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(AccentRed, AccentOrange)
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Listening...",
                style = MaterialTheme.typography.labelMedium,
                color = AccentRed,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
