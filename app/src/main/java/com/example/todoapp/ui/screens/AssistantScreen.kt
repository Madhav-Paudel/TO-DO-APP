package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.data.model.ActionTaken
import com.example.todoapp.data.model.ActionType
import com.example.todoapp.data.model.ChatMessage
import com.example.todoapp.data.model.ChatSender
import com.example.todoapp.ui.viewmodels.AssistantViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Color palette
private val UserBubbleColor = Color(0xFF2196F3)
private val UserBubbleGradientStart = Color(0xFF42A5F5)
private val UserBubbleGradientEnd = Color(0xFF1976D2)
private val AiBubbleGradientStart = Color(0xFF7C4DFF)
private val AiBubbleGradientEnd = Color(0xFF536DFE)
private val SystemBubbleColor = Color(0xFF4CAF50)
private val AccentPurple = Color(0xFF7C4DFF)
private val AccentBlue = Color(0xFF2196F3)
private val HeaderGradientStart = Color(0xFF667eea)
private val HeaderGradientEnd = Color(0xFF764ba2)
private val ChipBackground = Color(0xFFF3E5F5)
private val ChipText = Color(0xFF7C4DFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel = viewModel()) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with gradient
        ChatHeader()
        
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
            // Welcome message if empty
            if (chatHistory.isEmpty()) {
                item {
                    WelcomeSection(onSuggestionClick = { viewModel.sendUserMessage(it) })
                }
            }
            
            items(chatHistory) { message ->
                ModernChatBubble(message)
            }
            
            // Typing indicator when loading
            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }
        
        // Quick suggestions (only show when chat is not empty and not loading)
        if (chatHistory.isNotEmpty() && !isLoading) {
            QuickSuggestionChips(onChipClick = { viewModel.sendUserMessage(it) })
        }
        
        // Input bar
        ModernInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.text.isNotBlank()) {
                    viewModel.sendUserMessage(inputText.text)
                    inputText = TextFieldValue("")
                }
            },
            isLoading = isLoading
        )
    }
}

@Composable
private fun ChatHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(HeaderGradientStart, HeaderGradientEnd)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
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
            
            Column {
                Text(
                    text = "AI Assistant (Text Only)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ask me anything about your goals & habits",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
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
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large AI icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Hi there! 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "I'm here to help you achieve your goals.\nTry one of these to get started:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        
        // Suggestion cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuggestionCard(
                icon = Icons.Outlined.CalendarMonth,
                text = "Plan my week",
                color = Color(0xFF4CAF50),
                onClick = { onSuggestionClick("Plan my week") }
            )
            SuggestionCard(
                icon = Icons.Outlined.Flag,
                text = "Create a new learning goal",
                color = Color(0xFF2196F3),
                onClick = { onSuggestionClick("Create a new learning goal") }
            )
            SuggestionCard(
                icon = Icons.Outlined.Checklist,
                text = "Break my goal into tasks",
                color = Color(0xFFFF9800),
                onClick = { onSuggestionClick("Break my goal into tasks") }
            )
            SuggestionCard(
                icon = Icons.Outlined.Schedule,
                text = "Recommend a daily routine",
                color = Color(0xFF9C27B0),
                onClick = { onSuggestionClick("Recommend a daily routine") }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModernChatBubble(message: ChatMessage) {
    val isUser = message.sender == ChatSender.USER
    val isSystem = message.sender == ChatSender.SYSTEM
    val isAi = message.sender == ChatSender.ASSISTANT
    
    val alignment = when (message.sender) {
        ChatSender.USER -> Alignment.End
        ChatSender.ASSISTANT -> Alignment.Start
        ChatSender.SYSTEM -> Alignment.Start
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // AI Avatar (left side for AI/System)
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSystem) Modifier.background(SystemBubbleColor.copy(alpha = 0.2f))
                            else Modifier.background(
                                brush = Brush.linearGradient(
                                    listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSystem) Icons.Default.Settings else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isSystem) SystemBubbleColor else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Message Bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .then(
                        if (isUser) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    colors = listOf(UserBubbleGradientStart, UserBubbleGradientEnd)
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                        } else if (isAi) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AiBubbleGradientStart.copy(alpha = 0.15f),
                                        AiBubbleGradientEnd.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 20.dp
                                )
                            )
                        } else {
                            Modifier.background(
                                color = SystemBubbleColor.copy(alpha = 0.12f),
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
                        color = if (isUser) Color.White 
                               else MaterialTheme.colorScheme.onBackground,
                        lineHeight = 22.sp
                    )
                    
                    // Timestamp
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(alpha = 0.7f)
                               else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 6.dp)
                    )
                }
            }
            
            // User Avatar (right side)
            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(UserBubbleColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = UserBubbleColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // Action Taken Card
        if (message.action != null && message.action.type != ActionType.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            ActionTakenCard(action = message.action)
        }
    }
}

@Composable
private fun ActionTakenCard(action: ActionTaken) {
    var expanded by remember { mutableStateOf(false) }
    
    val actionInfo = when (action.type) {
        ActionType.GOAL_CREATED -> Triple("Goal Created", Icons.Default.Flag, Color(0xFF4CAF50))
        ActionType.TASK_CREATED -> Triple("Task Created", Icons.Default.CheckCircle, Color(0xFF2196F3))
        ActionType.GOAL_DELETED -> Triple("Goal Deleted", Icons.Default.Delete, Color(0xFFE53935))
        ActionType.TASK_DELETED -> Triple("Task Deleted", Icons.Default.Delete, Color(0xFFE53935))
        ActionType.TASK_COMPLETED -> Triple("Task Completed", Icons.Default.Done, Color(0xFF4CAF50))
        ActionType.LIST_SHOWN -> Triple("Data Retrieved", Icons.Default.List, Color(0xFF9C27B0))
        ActionType.NONE -> Triple("", Icons.Default.Info, Color.Gray)
    }
    
    Card(
        modifier = Modifier
            .padding(start = 40.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = actionInfo.third.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    actionInfo.second,
                    contentDescription = null,
                    tint = actionInfo.third,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionInfo.first,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = actionInfo.third
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = actionInfo.third.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = actionInfo.third.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Item: ${action.itemName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    if (action.details.isNotBlank()) {
                        Text(
                            text = "Details: ${action.details}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Typing dots
        Box(
            modifier = Modifier
                .background(
                    color = AiBubbleGradientStart.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            TypingDots()
        }
    }
}

@Composable
private fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val animatedY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 600
                        0f at 0
                        -6f at 200
                        0f at 400
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "dot_$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(y = animatedY.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AiBubbleGradientStart)
            )
        }
    }
}

@Composable
private fun QuickSuggestionChips(onChipClick: (String) -> Unit) {
    val suggestions = listOf(
        "Plan my week",
        "Create a new learning goal",
        "Break my goal into tasks",
        "Recommend a daily routine"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onChipClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium,
                        color = ChipText
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = ChipBackground
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = AccentPurple.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ModernInputBar(
    inputText: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    val hasText = inputText.text.isNotBlank()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp),
                placeholder = {
                    Text(
                        "Ask your AI coach...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !isLoading,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Chat,
                        contentDescription = null,
                        tint = AccentPurple.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Send button
            val buttonColor by animateColorAsState(
                targetValue = if (hasText && !isLoading) AccentPurple else Color.Gray.copy(alpha = 0.4f),
                label = "button_color"
            )
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasText && !isLoading) {
                            Brush.linearGradient(
                                listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
                            )
                        }
                    )
                    .clickable(enabled = hasText && !isLoading) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
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

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
