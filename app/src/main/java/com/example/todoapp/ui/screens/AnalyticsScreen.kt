package com.example.todoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.ui.AppViewModelProvider
import com.example.todoapp.ui.theme.*
import com.example.todoapp.ui.viewmodels.AnalyticsFilter
import com.example.todoapp.ui.viewmodels.AnalyticsViewModel
import com.example.todoapp.util.ActivityHeatmapGenerator
import com.example.todoapp.util.AiAnalyticsEngine
import com.example.todoapp.util.AnalyticsUtils
import com.example.todoapp.util.DailyDataPoint
import com.example.todoapp.util.GoalProgressData
import com.example.todoapp.util.GoalProjectionEngine
import com.example.todoapp.util.UsageStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Theme Colors
private val GradientPurple = Color(0xFF9C27B0)
private val GradientBlue = Color(0xFF2196F3)
private val GradientTeal = Color(0xFF009688)
private val GradientStart = Color(0xFF7B1FA2)
private val GradientEnd = Color(0xFF00BCD4)
private val CardBackground = Color(0xFFF8F9FF)

// Background gradient (matching other screens)
private val BackgroundGradientStart = Color(0xFFF5F7FA)
private val BackgroundGradientEnd = Color(0xFFE8EDF5)

// Status colors
private val StatusGreen = Color(0xFF4CAF50)
private val StatusYellow = Color(0xFFFFC107)
private val StatusRed = Color(0xFFE53935)
private val StatusOrange = Color(0xFFFF9800)

// AI Analytics colors
private val InsightPositive = Color(0xFF4CAF50)
private val InsightNeutral = Color(0xFF2196F3)
private val InsightWarning = Color(0xFFFF9800)
private val InsightCritical = Color(0xFFE53935)
private val InsightAchievement = Color(0xFF9C27B0)
private val PredictionHigh = Color(0xFF4CAF50)
private val PredictionMedium = Color(0xFF2196F3)
private val PredictionLow = Color(0xFFFF9800)
private val SuggestionUrgent = Color(0xFFE53935)
private val SuggestionHigh = Color(0xFFFF9800)
private val SuggestionMedium = Color(0xFF2196F3)
private val SuggestionLow = Color(0xFF4CAF50)

// GitHub-style Heatmap colors (green gradient)
private val HeatmapEmpty = Color(0xFFEBEDF0)
private val HeatmapLevel1 = Color(0xFFC6E48B)
private val HeatmapLevel2 = Color(0xFF7BC96F)
private val HeatmapLevel3 = Color(0xFF239A3B)
private val HeatmapLevel4 = Color(0xFF196127)

// Projection status colors
private val ProjectionOnTrack = Color(0xFF4CAF50)
private val ProjectionSlightDelay = Color(0xFFFFC107)
private val ProjectionBehind = Color(0xFFE53935)
private val ProjectionCompleted = Color(0xFF9C27B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    
    // Initialize theme manager
    LaunchedEffect(Unit) {
        AnalyticsThemeManager.initialize(context)
    }
    
    // Theme state
    val currentThemeType by AnalyticsThemeManager.currentThemeType.collectAsState()
    val analyticsTheme by AnalyticsThemeManager.currentTheme.collectAsState()
    var showThemeSelector by remember { mutableStateOf(false) }
    
    val currentFilter by viewModel.filter.collectAsState()
    val goalProgressList by viewModel.goalProgressDataList.collectAsState()
    val studyChartData by viewModel.studyChartData.collectAsState()
    val phoneUsageChartData by viewModel.phoneUsageChartData.collectAsState()
    val todayPhoneUsage by viewModel.todayPhoneUsage.collectAsState()
    val phoneUsageStatus by viewModel.phoneUsageStatus.collectAsState()
    val totalStudyMinutes by viewModel.totalStudyMinutes.collectAsState()
    val avgStudyMinutes by viewModel.avgStudyMinutes.collectAsState()
    val overallTargetMet by viewModel.overallTargetMetPercentage.collectAsState()
    val maxStreak by viewModel.maxStreak.collectAsState()
    val aiReport by viewModel.aiAnalyticsReport.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val heatmapView by viewModel.heatmapView.collectAsState()
    val goalProjections by viewModel.goalProjections.collectAsState()
    val projectionSummary by viewModel.projectionSummary.collectAsState()
    val selectedProjection by viewModel.selectedProjection.collectAsState()
    
    // Wrap with theme provider
    AnalyticsThemeProvider(theme = analyticsTheme) {
        ThemedAnalyticsScreenContent(
            analyticsTheme = analyticsTheme,
            currentThemeType = currentThemeType,
            showThemeSelector = showThemeSelector,
            onShowThemeSelector = { showThemeSelector = it },
            currentFilter = currentFilter,
            onFilterSelected = { viewModel.setFilter(it) },
            goalProgressList = goalProgressList,
            studyChartData = studyChartData,
            phoneUsageChartData = phoneUsageChartData,
            todayPhoneUsage = todayPhoneUsage,
            phoneUsageStatus = phoneUsageStatus,
            totalStudyMinutes = totalStudyMinutes,
            avgStudyMinutes = avgStudyMinutes,
            overallTargetMet = overallTargetMet,
            maxStreak = maxStreak,
            aiReport = aiReport,
            heatmapData = heatmapData,
            heatmapView = heatmapView,
            onHeatmapViewChanged = { viewModel.setHeatmapView(it) },
            goalProjections = goalProjections,
            projectionSummary = projectionSummary,
            selectedProjection = selectedProjection,
            onSelectProjection = { viewModel.selectProjectionGoal(it) }
        )
    }
}

@Composable
private fun ThemedAnalyticsScreenContent(
    analyticsTheme: AnalyticsTheme,
    currentThemeType: AnalyticsThemeType,
    showThemeSelector: Boolean,
    onShowThemeSelector: (Boolean) -> Unit,
    currentFilter: AnalyticsFilter,
    onFilterSelected: (AnalyticsFilter) -> Unit,
    goalProgressList: List<GoalProgressData>,
    studyChartData: List<DailyDataPoint>,
    phoneUsageChartData: List<DailyDataPoint>,
    todayPhoneUsage: Int,
    phoneUsageStatus: UsageStatus,
    totalStudyMinutes: Int,
    avgStudyMinutes: Int,
    overallTargetMet: Int,
    maxStreak: Int,
    aiReport: AiAnalyticsEngine.AnalyticsReport?,
    heatmapData: ActivityHeatmapGenerator.HeatmapData?,
    heatmapView: ActivityHeatmapGenerator.HeatmapView,
    onHeatmapViewChanged: (ActivityHeatmapGenerator.HeatmapView) -> Unit,
    goalProjections: List<GoalProjectionEngine.GoalProjection>,
    projectionSummary: GoalProjectionEngine.ProjectionSummary?,
    selectedProjection: GoalProjectionEngine.GoalProjection?,
    onSelectProjection: (Long?) -> Unit
) {
    // Use standard Material Theme background like other screens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = analyticsTheme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(analyticsTheme.spacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = analyticsTheme.spacing.lg)
        ) {
            // Header with AI Score and Theme Picker
            item {
                ThemedAnalyticsHeader(
                    aiReport = aiReport,
                    theme = analyticsTheme,
                    onThemeClick = { onShowThemeSelector(true) }
                )
            }

            // Quick Stats Row
            item {
                ThemedQuickStatsRow(
                    totalMinutes = totalStudyMinutes,
                    avgMinutes = avgStudyMinutes,
                    targetMet = overallTargetMet,
                    maxStreak = maxStreak,
                    theme = analyticsTheme
                )
            }

            // Filter Chips
            item {
                ThemedFilterChipsRow(
                    currentFilter = currentFilter,
                    onFilterSelected = onFilterSelected,
                    theme = analyticsTheme
                )
            }

            // GitHub-style Activity Heatmap
            item {
                ThemedSectionHeader(
                    emoji = "📅",
                    title = "Activity Heatmap",
                    subtitle = "Your daily activity at a glance",
                    theme = analyticsTheme
                )
            }

            item {
                ThemedActivityHeatmapCard(
                    heatmapData = heatmapData,
                    currentView = heatmapView,
                    onViewChanged = onHeatmapViewChanged,
                    theme = analyticsTheme
                )
            }

            // Goal Projection Engine Section
            if (goalProjections.isNotEmpty()) {
                item {
                    ThemedSectionHeader(
                        emoji = "🎯",
                        title = "Goal Projections",
                        subtitle = "Track progress and forecast completion",
                        theme = analyticsTheme
                    )
                }

                // Projection Summary Card
                if (projectionSummary != null) {
                    item {
                        ThemedProjectionSummaryCard(
                            summary = projectionSummary,
                            theme = analyticsTheme
                        )
                    }
                }

                // Goal Selector (if multiple goals)
                if (goalProjections.size > 1) {
                    item {
                        ThemedGoalProjectionSelector(
                            projections = goalProjections,
                            selectedProjection = selectedProjection,
                            onGoalSelected = onSelectProjection,
                            theme = analyticsTheme
                        )
                    }
                }

                // Selected Goal Projection Details
                if (selectedProjection != null) {
                    item {
                        ThemedGoalProjectionCard(
                            projection = selectedProjection,
                            theme = analyticsTheme
                        )
                    }

                    item {
                        ThemedProjectionForecastChart(
                            projection = selectedProjection,
                            theme = analyticsTheme
                        )
                    }

                    item {
                        ThemedGoalConeAnimation(
                            projection = selectedProjection,
                            theme = analyticsTheme
                        )
                    }

                    item {
                        ThemedProjectionRecommendationCard(
                            projection = selectedProjection,
                            theme = analyticsTheme
                        )
                    }
                }
            }

            // AI Insights Section
            if (aiReport != null && aiReport.insights.isNotEmpty()) {
                item {
                    ThemedSectionHeader(
                        emoji = "🧠",
                        title = "AI Insights",
                        subtitle = "Personalized analysis of your activity",
                        theme = analyticsTheme
                    )
                }

                item {
                    ThemedAiInsightsSection(
                        insights = aiReport.insights,
                        theme = analyticsTheme
                    )
                }
            }

            // AI Predictions Section
            if (aiReport != null && aiReport.predictions.isNotEmpty()) {
                item {
                    ThemedSectionHeader(
                        emoji = "🔮",
                        title = "Predictions",
                        subtitle = "What the data tells us about your future",
                        theme = analyticsTheme
                    )
                }

                item {
                    ThemedAiPredictionsSection(
                        predictions = aiReport.predictions,
                        theme = analyticsTheme
                    )
                }
            }

            // AI Suggestions Section
            if (aiReport != null && aiReport.suggestions.isNotEmpty()) {
                item {
                    ThemedSectionHeader(
                        emoji = "💡",
                        title = "Suggestions",
                        subtitle = "Recommendations to boost your productivity",
                        theme = analyticsTheme
                    )
                }

                item {
                    ThemedAiSuggestionsSection(
                        suggestions = aiReport.suggestions,
                        theme = analyticsTheme
                    )
                }
            }

            // Overall Goal Progress Section
            item {
                ThemedSectionHeader(
                    emoji = "🎯",
                    title = "Goal Progress",
                    subtitle = "Your active goals",
                    theme = analyticsTheme
                )
            }

            if (goalProgressList.isEmpty()) {
                item {
                    ThemedEmptyState(
                        emoji = "🎯",
                        title = "No active goals",
                        subtitle = "Create a goal to start tracking your progress!",
                        theme = analyticsTheme
                    )
                }
            } else {
                items(goalProgressList) { goalData ->
                    ThemedGoalProgressCard(
                        goalData = goalData,
                        theme = analyticsTheme
                    )
                }
            }

            // Daily Study Progress Graph
            item {
                ThemedSectionHeader(
                    emoji = "📚",
                    title = "Study Time",
                    subtitle = "Minutes studied each day",
                    theme = analyticsTheme
                )
            }

            item {
                ThemedStudyTimeChart(
                    data = studyChartData,
                    filter = currentFilter,
                    theme = analyticsTheme
                )
            }

            // Phone Usage Graph
            item {
                ThemedSectionHeader(
                    emoji = "📱",
                    title = "Phone Usage",
                    subtitle = "Screen time tracking",
                    theme = analyticsTheme
                )
            }

            item {
                ThemedPhoneUsageChart(
                    data = phoneUsageChartData,
                    todayUsage = todayPhoneUsage,
                    status = phoneUsageStatus,
                    filter = currentFilter,
                    theme = analyticsTheme
                )
            }

            // Streak Visualization
            item {
                ThemedSectionHeader(
                    emoji = "🔥",
                    title = "Your Streaks",
                    subtitle = "Consistency tracking",
                    theme = analyticsTheme
                )
            }

            if (goalProgressList.isEmpty()) {
                item {
                    ThemedEmptyState(
                        emoji = "🔥",
                        title = "No streaks yet",
                        subtitle = "Complete daily goals to build streaks!",
                        theme = analyticsTheme
                    )
                }
            } else {
                items(goalProgressList) { goalData ->
                    ThemedStreakVisualizationCard(
                        goalData = goalData,
                        theme = analyticsTheme
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
        
        // Theme Selector Dialog
        if (showThemeSelector) {
            ThemeSelectorDialog(
                currentTheme = currentThemeType,
                onThemeSelected = { 
                    AnalyticsThemeManager.setTheme(it)
                    onShowThemeSelector(false)
                },
                onDismiss = { onShowThemeSelector(false) },
                theme = analyticsTheme
            )
        }
    }
}

// ==================== AI ANALYTICS HEADER ====================

@Composable
fun AnalyticsHeader(aiReport: AiAnalyticsEngine.AnalyticsReport?) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedScore by animateFloatAsState(
        targetValue = if (animationPlayed) (aiReport?.overallScore?.toFloat() ?: 0f) else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )

    LaunchedEffect(aiReport) {
        animationPlayed = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GradientPurple.copy(alpha = 0.1f),
                            GradientBlue.copy(alpha = 0.1f),
                            GradientTeal.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📊",
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = aiReport?.summary ?: "Track your progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // AI Score Circle
                if (aiReport != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        AiScoreCircle(
                            score = animatedScore.roundToInt(),
                            modifier = Modifier.size(80.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${animatedScore.roundToInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = getScoreColor(animatedScore.roundToInt())
                            )
                            Text(
                                text = "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiScoreCircle(score: Int, modifier: Modifier = Modifier) {
    val color = getScoreColor(score)
    
    Canvas(modifier = modifier) {
        val strokeWidth = 8f
        val radius = (size.minDimension - strokeWidth) / 2

        // Background arc
        drawCircle(
            color = Color(0xFFE8EAF6),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * (score / 100f),
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth)
        )
    }
}

fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> InsightPositive
        score >= 60 -> InsightNeutral
        score >= 40 -> InsightWarning
        else -> InsightCritical
    }
}

// ==================== AI SECTIONS ====================

@Composable
fun AiSectionHeader(icon: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AiInsightsSection(insights: List<AiAnalyticsEngine.AiInsight>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(insights) { insight ->
            AnimatedInsightCard(insight = insight)
        }
    }
}

@Composable
fun AnimatedInsightCard(insight: AiAnalyticsEngine.AiInsight) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(insight.id) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInHorizontally(
            animationSpec = tween(500),
            initialOffsetX = { it / 2 }
        )
    ) {
        InsightCard(insight = insight)
    }
}

@Composable
fun InsightCard(insight: AiAnalyticsEngine.AiInsight) {
    val backgroundColor = when (insight.type) {
        AiAnalyticsEngine.InsightType.POSITIVE -> InsightPositive
        AiAnalyticsEngine.InsightType.NEUTRAL -> InsightNeutral
        AiAnalyticsEngine.InsightType.WARNING -> InsightWarning
        AiAnalyticsEngine.InsightType.CRITICAL -> InsightCritical
        AiAnalyticsEngine.InsightType.ACHIEVEMENT -> InsightAchievement
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with icon and type indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(backgroundColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(insight.icon, fontSize = 22.sp)
                }
                
                // Trend indicator if available
                insight.trend?.let { trend ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (trend >= 0) InsightPositive.copy(alpha = 0.1f)
                                else InsightCritical.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (trend >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (trend >= 0) InsightPositive else InsightCritical
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (trend >= 0) "+" else ""}${trend.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (trend >= 0) InsightPositive else InsightCritical
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Metric if available
            if (insight.metric != null && insight.metricValue != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(backgroundColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insight.metric!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = insight.metricValue!!,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = backgroundColor
                    )
                }
            }
        }
    }
}

@Composable
fun AiPredictionsSection(predictions: List<AiAnalyticsEngine.AiPrediction>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        predictions.forEach { prediction ->
            AnimatedPredictionCard(prediction = prediction)
        }
    }
}

@Composable
fun AnimatedPredictionCard(prediction: AiAnalyticsEngine.AiPrediction) {
    var visible by remember { mutableStateOf(false) }
    var animatedProbability by remember { mutableStateOf(0f) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) prediction.probability / 100f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "predictionProgress"
    )
    
    LaunchedEffect(prediction.id) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + expandVertically(
            animationSpec = tween(500)
        )
    ) {
        PredictionCard(prediction = prediction, animatedProgress = animatedProgress)
    }
}

@Composable
fun PredictionCard(prediction: AiAnalyticsEngine.AiPrediction, animatedProgress: Float) {
    val confidenceColor = when (prediction.confidence) {
        AiAnalyticsEngine.PredictionConfidence.VERY_HIGH -> PredictionHigh
        AiAnalyticsEngine.PredictionConfidence.HIGH -> PredictionHigh
        AiAnalyticsEngine.PredictionConfidence.MEDIUM -> PredictionMedium
        AiAnalyticsEngine.PredictionConfidence.LOW -> PredictionLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(confidenceColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(prediction.icon, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prediction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = prediction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Probability bar
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE8EAF6))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(confidenceColor.copy(alpha = 0.7f), confidenceColor)
                                    )
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${prediction.probability}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = confidenceColor
                    )
                }

                // Timeframe
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = prediction.timeframe,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Confidence badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(confidenceColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = prediction.confidence.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = confidenceColor
                )
            }
        }
    }
}

@Composable
fun AiSuggestionsSection(suggestions: List<AiAnalyticsEngine.AiSuggestion>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        suggestions.forEach { suggestion ->
            AnimatedSuggestionCard(suggestion = suggestion)
        }
    }
}

@Composable
fun AnimatedSuggestionCard(suggestion: AiAnalyticsEngine.AiSuggestion) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(suggestion.id) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
            animationSpec = tween(500),
            initialOffsetY = { it / 2 }
        )
    ) {
        SuggestionCard(suggestion = suggestion)
    }
}

@Composable
fun SuggestionCard(suggestion: AiAnalyticsEngine.AiSuggestion) {
    val priorityColor = when (suggestion.priority) {
        AiAnalyticsEngine.SuggestionPriority.URGENT -> SuggestionUrgent
        AiAnalyticsEngine.SuggestionPriority.HIGH -> SuggestionHigh
        AiAnalyticsEngine.SuggestionPriority.MEDIUM -> SuggestionMedium
        AiAnalyticsEngine.SuggestionPriority.LOW -> SuggestionLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(priorityColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(suggestion.icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action button
            FilledTonalButton(
                onClick = { /* Implement action */ },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = priorityColor.copy(alpha = 0.15f),
                    contentColor = priorityColor
                )
            ) {
                Text(
                    text = suggestion.actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==================== EXISTING COMPONENTS ====================

@Composable
fun QuickStatsRow(
    totalMinutes: Int,
    avgMinutes: Int,
    targetMet: Int,
    maxStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "⏱️",
            value = AnalyticsUtils.formatMinutes(totalMinutes),
            label = "Total",
            color = GradientPurple
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "📊",
            value = "${avgMinutes}m",
            label = "Daily Avg",
            color = GradientBlue
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "🎯",
            value = "$targetMet%",
            label = "Goals Met",
            color = GradientTeal
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = "🔥",
            value = "$maxStreak",
            label = "Best Streak",
            color = StatusOrange
        )
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    currentFilter: AnalyticsFilter,
    onFilterSelected: (AnalyticsFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsFilter.entries.forEach { filter ->
            val isSelected = currentFilter == filter

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        filter.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GradientBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateCard(icon: String, title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GoalProgressCard(goalData: GoalProgressData) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) goalData.overallProgress else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    LaunchedEffect(goalData.goal.id) {
        animationPlayed = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = GradientPurple.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressWithGradient(
                    progress = animatedProgress,
                    modifier = Modifier.size(80.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GradientPurple
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Goal Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goalData.goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${goalData.daysRemaining} days left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${goalData.goal.dailyTargetMinutes} min/day target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Streak Badge
            if (goalData.currentStreak > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    StatusOrange.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text("🔥", fontSize = 24.sp)
                    Text(
                        text = "${goalData.currentStreak}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusOrange
                    )
                }
            }
        }
    }
}

@Composable
fun CircularProgressWithGradient(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 10f
        val radius = (size.minDimension - strokeWidth) / 2

        // Background arc
        drawCircle(
            color = Color(0xFFE8EAF6),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc with gradient
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(GradientPurple, GradientBlue, GradientTeal, GradientPurple)
            ),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth)
        )
    }
}

@Composable
fun StudyTimeChart(
    data: List<DailyDataPoint>,
    filter: AnalyticsFilter
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "chartAnimation"
    )

    LaunchedEffect(data) {
        animationPlayed = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (data.isEmpty() || data.all { it.value == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No study data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start a timer session!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Selected value tooltip
                selectedIndex?.let { index ->
                    if (index < data.size) {
                        val point = data[index]
                        Card(
                            modifier = Modifier.padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = GradientBlue.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${AnalyticsUtils.getDayLabel(point.date)}: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${point.value} minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GradientBlue
                                )
                            }
                        }
                    }
                }

                val displayData = when (filter) {
                    AnalyticsFilter.WEEK -> data.takeLast(7)
                    AnalyticsFilter.MONTH -> data.takeLast(30)
                    AnalyticsFilter.QUARTER -> data.takeLast(90)
                }

                val maxValue = displayData.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val barWidth = size.width.toFloat() / displayData.size
                                val index = (offset.x / barWidth).toInt().coerceIn(0, displayData.size - 1)
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = displayData.size
                        val spacing = 4.dp.toPx()
                        val totalSpacing = spacing * (barCount + 1)
                        val barWidth = (size.width - totalSpacing) / barCount
                        val maxBarHeight = size.height - 30.dp.toPx()

                        displayData.forEachIndexed { index, point ->
                            val barHeight = (point.value.toFloat() / maxValue) * maxBarHeight * animatedProgress
                            val x = spacing + index * (barWidth + spacing)
                            val y = size.height - barHeight - 25.dp.toPx()

                            val isSelected = selectedIndex == index

                            // Bar with gradient
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isSelected)
                                        listOf(GradientPurple, GradientBlue)
                                    else
                                        listOf(GradientBlue.copy(alpha = 0.8f), GradientTeal.copy(alpha = 0.6f))
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            // Dot on top
                            if (barHeight > 0) {
                                drawCircle(
                                    color = if (isSelected) GradientPurple else GradientBlue,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = Offset(x + barWidth / 2, y)
                                )
                            }
                        }
                    }
                }

                // X-axis labels (show only for week view)
                if (filter == AnalyticsFilter.WEEK && displayData.size <= 7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        displayData.forEach { point ->
                            Text(
                                text = AnalyticsUtils.getDayLabel(point.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneUsageChart(
    data: List<DailyDataPoint>,
    todayUsage: Int,
    status: UsageStatus,
    filter: AnalyticsFilter
) {
    val statusColor = when (status) {
        UsageStatus.GOOD -> StatusGreen
        UsageStatus.WARNING -> StatusYellow
        UsageStatus.DANGER -> StatusRed
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "phoneChartAnimation"
    )

    LaunchedEffect(data) {
        animationPlayed = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Today's usage indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📱", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Today: ${todayUsage} min",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                text = "Target: < 30 min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (status) {
                            UsageStatus.GOOD -> Text("✅", fontSize = 18.sp)
                            UsageStatus.WARNING -> Text("⚠️", fontSize = 18.sp)
                            UsageStatus.DANGER -> Text("🚫", fontSize = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty() || data.all { it.value == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 32.sp)
                        Text(
                            "No usage data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val displayData = when (filter) {
                    AnalyticsFilter.WEEK -> data.takeLast(7)
                    AnalyticsFilter.MONTH -> data.takeLast(30)
                    AnalyticsFilter.QUARTER -> data.takeLast(90)
                }

                val maxValue = displayData.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

                // Line chart with color coding
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    if (displayData.size < 2) return@Canvas

                    val pointSpacing = size.width / (displayData.size - 1).coerceAtLeast(1)
                    val maxHeight = size.height - 20.dp.toPx()

                    // Draw trendline
                    val path = Path()
                    displayData.forEachIndexed { index, point ->
                        val x = index * pointSpacing
                        val y = size.height - (point.value.toFloat() / maxValue) * maxHeight * animatedProgress - 10.dp.toPx()

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(StatusGreen, StatusYellow, StatusRed)
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw points with color based on value
                    displayData.forEachIndexed { index, point ->
                        val x = index * pointSpacing
                        val y = size.height - (point.value.toFloat() / maxValue) * maxHeight * animatedProgress - 10.dp.toPx()

                        val pointColor = when {
                            point.value < 30 -> StatusGreen
                            point.value < 60 -> StatusYellow
                            else -> StatusRed
                        }

                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = pointColor,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakVisualizationCard(goalData: GoalProgressData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fire badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                StatusOrange.copy(alpha = 0.3f),
                                StatusOrange.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goalData.goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Current Streak: ${goalData.currentStreak} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (goalData.currentStreak > 0) StatusOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (goalData.currentStreak > 0) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Sparkline
            SparklineChart(
                data = goalData.sparklineData,
                modifier = Modifier
                    .width(80.dp)
                    .height(24.dp)
            )
        }
    }
}

@Composable
fun SparklineChart(
    data: List<Boolean>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val dotCount = data.size
        if (dotCount <= 1) return@Canvas
        
        val spacing = size.width / (dotCount - 1).coerceAtLeast(1)
        val dotRadius = 4.dp.toPx()

        data.forEachIndexed { index, met ->
            val x = index * spacing
            val y = size.height / 2

            // Connection line
            if (index > 0) {
                val prevX = (index - 1) * spacing
                val lineColor = if (met && data[index - 1]) StatusGreen.copy(alpha = 0.5f)
                else Color.Gray.copy(alpha = 0.3f)

                drawLine(
                    color = lineColor,
                    start = Offset(prevX, y),
                    end = Offset(x, y),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Dot
            drawCircle(
                color = if (met) StatusGreen else Color.LightGray,
                radius = dotRadius,
                center = Offset(x, y)
            )
        }
    }
}

// ==================== GITHUB-STYLE ACTIVITY HEATMAP ====================

@Composable
fun ActivityHeatmapCard(
    heatmapData: ActivityHeatmapGenerator.HeatmapData?,
    currentView: ActivityHeatmapGenerator.HeatmapView,
    onViewChanged: (ActivityHeatmapGenerator.HeatmapView) -> Unit
) {
    var selectedCell by remember { mutableStateOf<ActivityHeatmapGenerator.HeatmapCell?>(null) }
    var showTooltip by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // View Type Tabs
            HeatmapViewTabs(
                currentView = currentView,
                onViewChanged = onViewChanged
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Streak Info
            if (heatmapData != null && heatmapData.currentStreak > 0) {
                AnimatedStreakBanner(
                    currentStreak = heatmapData.currentStreak,
                    longestStreak = heatmapData.longestStreak
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Period Label
            if (heatmapData != null) {
                Text(
                    text = "${heatmapData.monthLabel} ${heatmapData.yearLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Heatmap Grid
            if (heatmapData != null) {
                GitHubStyleHeatmapGrid(
                    heatmapData = heatmapData,
                    onCellTapped = { cell ->
                        selectedCell = cell
                        showTooltip = true
                    }
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GradientPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            HeatmapLegend()
            
            // Stats Row
            if (heatmapData != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HeatmapStatsRow(
                    totalStudyMinutes = heatmapData.totalStudyMinutes,
                    totalActiveDays = heatmapData.totalActiveDays,
                    weekCount = heatmapData.weekCount
                )
            }
            
            // Gap Warnings
            if (heatmapData != null && heatmapData.gaps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                GapWarnings(gaps = heatmapData.gaps)
            }
        }
    }
    
    // Tooltip Dialog
    if (showTooltip && selectedCell != null) {
        HeatmapCellTooltipDialog(
            cell = selectedCell!!,
            onDismiss = { showTooltip = false }
        )
    }
}

@Composable
fun HeatmapViewTabs(
    currentView: ActivityHeatmapGenerator.HeatmapView,
    onViewChanged: (ActivityHeatmapGenerator.HeatmapView) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActivityHeatmapGenerator.HeatmapView.values().forEach { view ->
            val isSelected = currentView == view
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBackground"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) GradientPurple else Color.Gray,
                animationSpec = tween(200),
                label = "tabText"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .pointerInput(view) {
                        detectTapGestures { onViewChanged(view) }
                    }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (view) {
                        ActivityHeatmapGenerator.HeatmapView.WEEKLY -> "Week"
                        ActivityHeatmapGenerator.HeatmapView.MONTHLY -> "Month"
                        ActivityHeatmapGenerator.HeatmapView.YEARLY -> "Year"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun AnimatedStreakBanner(
    currentStreak: Int,
    longestStreak: Int
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedStreak by animateIntAsState(
        targetValue = if (animationPlayed) currentStreak else 0,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "streakAnimation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    val isOnFire = currentStreak >= 7
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF9800).copy(alpha = 0.15f),
                        Color(0xFFFF5722).copy(alpha = 0.15f)
                    )
                )
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔥",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$animatedStreak-day learning streak!",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
            if (longestStreak > currentStreak) {
                Text(
                    text = "Best: $longestStreak days",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800)
                )
            }
        }
        if (isOnFire) {
            Text(
                text = "ON FIRE!",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun GitHubStyleHeatmapGrid(
    heatmapData: ActivityHeatmapGenerator.HeatmapData,
    onCellTapped: (ActivityHeatmapGenerator.HeatmapCell) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    
    // Group cells by week
    val cellsByWeek = heatmapData.cells.groupBy { it.weekIndex }
    val weeks = cellsByWeek.keys.sorted()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Day labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Empty corner for week labels
            Spacer(modifier = Modifier.width(36.dp))
            
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            dayLabels.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Heatmap rows (weeks)
        weeks.forEach { weekIndex ->
            val weekCells = cellsByWeek[weekIndex] ?: emptyList()
            val firstCell = weekCells.minByOrNull { it.dayOfWeek }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Week label
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = firstCell?.let { dateFormat.format(Date(it.date)) } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
                
                // Cells for each day (1=Sunday to 7=Saturday, but we want Mon-Sun)
                // Map to Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7, Sun=1
                val dayOrder = listOf(2, 3, 4, 5, 6, 7, 1) // Monday first
                dayOrder.forEach { dayOfWeek ->
                    val cell = weekCells.find { it.dayOfWeek == dayOfWeek }
                    HeatmapCellItem(
                        cell = cell,
                        onTapped = { cell?.let { onCellTapped(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.HeatmapCellItem(
    cell: ActivityHeatmapGenerator.HeatmapCell?,
    onTapped: () -> Unit
) {
    val cellColor = when {
        cell == null -> Color.Transparent
        !cell.isInCurrentMonth -> HeatmapEmpty.copy(alpha = 0.3f)
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.NONE -> HeatmapEmpty
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.LOW -> HeatmapLevel1
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> HeatmapLevel2
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.HIGH -> HeatmapLevel3
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> HeatmapLevel4
        else -> HeatmapEmpty
    }
    
    // Highlight today
    val borderModifier = if (cell?.isToday == true) {
        Modifier.border(2.dp, GradientPurple, RoundedCornerShape(3.dp))
    } else {
        Modifier
    }
    
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(3.dp))
            .background(cellColor)
            .then(borderModifier)
            .then(
                if (cell != null) {
                    Modifier.pointerInput(cell) {
                        detectTapGestures { onTapped() }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Show dot for today
        if (cell?.isToday == true) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        listOf(HeatmapEmpty, HeatmapLevel1, HeatmapLevel2, HeatmapLevel3, HeatmapLevel4).forEach { color ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
fun HeatmapStatsRow(
    totalStudyMinutes: Int,
    totalActiveDays: Int,
    weekCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F9FF))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HeatmapStatItem(
            label = "Study Time",
            value = "${totalStudyMinutes / 60}h ${totalStudyMinutes % 60}m"
        )
        HeatmapStatItem(
            label = "Active Days",
            value = "$totalActiveDays"
        )
        HeatmapStatItem(
            label = "Weeks",
            value = "$weekCount"
        )
    }
}

@Composable
fun HeatmapStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GradientPurple
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun GapWarnings(gaps: List<ActivityHeatmapGenerator.GapInfo>) {
    val visibleGaps = gaps.take(2) // Show max 2 gap warnings
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleGaps.forEach { gap ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusRed.copy(alpha = 0.1f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${gap.dayCount}-day gap detected",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = StatusRed
                    )
                    Text(
                        text = gap.formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun HeatmapCellTooltipDialog(
    cell: ActivityHeatmapGenerator.HeatmapCell,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cell.formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (cell.isToday) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = GradientPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Study Time
                TooltipStatRow(
                    icon = "📚",
                    label = "Study Time",
                    value = "${cell.studyMinutes} min",
                    color = GradientPurple
                )
                
                // Phone Usage
                TooltipStatRow(
                    icon = "📱",
                    label = "Phone Usage",
                    value = "${cell.phoneMinutes} min",
                    color = GradientBlue
                )
                
                // Tasks Completed
                TooltipStatRow(
                    icon = "✅",
                    label = "Tasks Done",
                    value = "${cell.tasksCompleted}",
                    color = StatusGreen
                )
                
                // Activity Level
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    val (activityText, activityColor) = when (cell.activityLevel) {
                        ActivityHeatmapGenerator.ActivityLevel.NONE -> "No Activity" to Color.Gray
                        ActivityHeatmapGenerator.ActivityLevel.LOW -> "Low" to HeatmapLevel1
                        ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> "Moderate" to HeatmapLevel2
                        ActivityHeatmapGenerator.ActivityLevel.HIGH -> "Good" to HeatmapLevel3
                        ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> "Excellent" to HeatmapLevel4
                    }
                    
                    Text(
                        text = activityText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = activityColor
                    )
                }
                
                // Progress bar
                val progressValue = when (cell.activityLevel) {
                    ActivityHeatmapGenerator.ActivityLevel.NONE -> 0f
                    ActivityHeatmapGenerator.ActivityLevel.LOW -> 0.25f
                    ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> 0.5f
                    ActivityHeatmapGenerator.ActivityLevel.HIGH -> 0.75f
                    ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> 1f
                }
                
                LinearProgressIndicator(
                    progress = progressValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (cell.activityLevel) {
                        ActivityHeatmapGenerator.ActivityLevel.NONE -> Color.Gray
                        ActivityHeatmapGenerator.ActivityLevel.LOW -> HeatmapLevel1
                        ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> HeatmapLevel2
                        ActivityHeatmapGenerator.ActivityLevel.HIGH -> HeatmapLevel3
                        ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> HeatmapLevel4
                    },
                    trackColor = Color(0xFFE0E0E0)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = GradientPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun TooltipStatRow(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ==================== GOAL PROJECTION ENGINE UI ====================

@Composable
fun ProjectionSummaryCard(summary: GoalProjectionEngine.ProjectionSummary) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedScore by animateIntAsState(
        targetValue = if (animationPlayed) summary.overallHealthScore else 0,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "healthScore"
    )
    
    LaunchedEffect(Unit) { animationPlayed = true }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GradientPurple.copy(alpha = 0.08f),
                            GradientBlue.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Goals Health Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.totalGoals} active goals tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                // Animated circular score
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = animatedScore / 100f,
                        modifier = Modifier.size(60.dp),
                        strokeWidth = 6.dp,
                        color = when {
                            animatedScore >= 70 -> ProjectionOnTrack
                            animatedScore >= 40 -> ProjectionSlightDelay
                            else -> ProjectionBehind
                        },
                        trackColor = Color(0xFFE0E0E0)
                    )
                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            animatedScore >= 70 -> ProjectionOnTrack
                            animatedScore >= 40 -> ProjectionSlightDelay
                            else -> ProjectionBehind
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProjectionStatusBadge(
                    count = summary.onTrackCount,
                    label = "On Track",
                    color = ProjectionOnTrack
                )
                ProjectionStatusBadge(
                    count = summary.needsAttentionCount,
                    label = "Needs Attention",
                    color = ProjectionSlightDelay
                )
                ProjectionStatusBadge(
                    count = summary.behindScheduleCount,
                    label = "Behind",
                    color = ProjectionBehind
                )
                ProjectionStatusBadge(
                    count = summary.completedCount,
                    label = "Completed",
                    color = ProjectionCompleted
                )
            }
        }
    }
}

@Composable
fun ProjectionStatusBadge(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun GoalProjectionSelector(
    projections: List<GoalProjectionEngine.GoalProjection>,
    selectedProjection: GoalProjectionEngine.GoalProjection?,
    onGoalSelected: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(projections) { projection ->
            val isSelected = selectedProjection?.goal?.id == projection.goal.id
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) getStatusColor(projection.status) else Color.White,
                animationSpec = tween(200),
                label = "selectorBg"
            )
            val textColor = if (isSelected) Color.White else Color.DarkGray
            
            Card(
                modifier = Modifier
                    .pointerInput(projection.goal.id) {
                        detectTapGestures { onGoalSelected(projection.goal.id) }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = projection.goal.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${projection.percentCompleted.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalProjectionCard(projection: GoalProjectionEngine.GoalProjection) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) projection.percentCompleted else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val animatedTime by animateFloatAsState(
        targetValue = if (animationPlayed) projection.percentTimeElapsed else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "time"
    )
    
    LaunchedEffect(projection.goal.id) { animationPlayed = true }
    
    val statusColor = getStatusColor(projection.status)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = projection.goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${projection.daysRemaining} days remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                // Status Badge
                Text(
                    text = projection.statusBadge,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress vs Time Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Progress Circle
                ProjectionCircleMetric(
                    value = animatedProgress,
                    label = "Completed",
                    color = statusColor
                )
                
                // VS indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    val diff = projection.percentCompleted - projection.percentTimeElapsed
                    Text(
                        text = if (diff >= 0) "+${diff.roundToInt()}%" else "${diff.roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (diff >= 0) ProjectionOnTrack else ProjectionBehind
                    )
                }
                
                // Time Circle
                ProjectionCircleMetric(
                    value = animatedTime,
                    label = "Time Elapsed",
                    color = GradientBlue
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (projection.status) {
                        GoalProjectionEngine.GoalStatus.ON_TRACK -> "✅"
                        GoalProjectionEngine.GoalStatus.SLIGHT_DELAY -> "⚠️"
                        GoalProjectionEngine.GoalStatus.BEHIND_SCHEDULE -> "🚨"
                        GoalProjectionEngine.GoalStatus.COMPLETED -> "🎉"
                        GoalProjectionEngine.GoalStatus.NOT_STARTED -> "🚀"
                    },
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = projection.statusDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pace metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PaceMetricItem(
                    label = "Current Pace",
                    value = "${projection.currentPace.roundToInt()} min/day",
                    icon = "📊"
                )
                PaceMetricItem(
                    label = "Required Pace",
                    value = "${projection.requiredPace.roundToInt()} min/day",
                    icon = "🎯"
                )
                PaceMetricItem(
                    label = "Original Target",
                    value = "${projection.originalPace.roundToInt()} min/day",
                    icon = "📌"
                )
            }
        }
    }
}

@Composable
fun ProjectionCircleMetric(value: Float, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = value / 100f,
                modifier = Modifier.size(80.dp),
                strokeWidth = 8.dp,
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
            Text(
                text = "${value.roundToInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun PaceMetricItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProjectionForecastChart(projection: GoalProjectionEngine.GoalProjection) {
    val chartData = projection.chartData
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Forecast Chart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartLegendItem("Actual", GradientPurple)
                    ChartLegendItem("Expected", Color.Gray)
                    ChartLegendItem("Required", ProjectionSlightDelay)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                ForecastLineChart(chartData = chartData)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chart footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Day 0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "Goal: ${(chartData.goalTarget / 60).roundToInt()}h total",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = GradientPurple
                )
                Text(
                    text = "Day ${projection.daysTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ForecastLineChart(chartData: GoalProjectionEngine.ProjectionChartData) {
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(chartData) {
        animationProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxValue = chartData.maxValue
        val totalDays = chartData.expectedProgress.size - 1
        
        if (totalDays <= 0 || maxValue <= 0) return@Canvas
        
        val xStep = width / totalDays
        
        fun valueToY(value: Float) = height - (value / maxValue * height)
        fun dayToX(day: Int) = day * xStep
        
        // Draw goal target line (dashed)
        val targetY = valueToY(chartData.goalTarget)
        drawLine(
            color = GradientPurple.copy(alpha = 0.3f),
            start = Offset(0f, targetY),
            end = Offset(width, targetY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(10f, 10f)
            )
        )
        
        // Draw expected progress line (gray dashed)
        val expectedPath = Path()
        chartData.expectedProgress.forEachIndexed { index, point ->
            val x = dayToX(point.day)
            val y = valueToY(point.value)
            if (index == 0) expectedPath.moveTo(x, y)
            else expectedPath.lineTo(x, y)
        }
        drawPath(
            path = expectedPath,
            color = Color.Gray.copy(alpha = 0.5f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(8f, 8f)
                )
            )
        )
        
        // Draw required pace line (yellow/orange)
        if (chartData.requiredPace.isNotEmpty()) {
            val requiredPath = Path()
            chartData.requiredPace.forEachIndexed { index, point ->
                val x = dayToX(point.day)
                val y = valueToY(point.value)
                if (index == 0) requiredPath.moveTo(x, y)
                else requiredPath.lineTo(x, y)
            }
            drawPath(
                path = requiredPath,
                color = ProjectionSlightDelay,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Draw actual progress line (animated, solid purple)
        if (chartData.actualProgress.isNotEmpty()) {
            val actualPath = Path()
            val animatedCount = (chartData.actualProgress.size * animationProgress).toInt()
                .coerceAtLeast(1)
            
            chartData.actualProgress.take(animatedCount).forEachIndexed { index, point ->
                val x = dayToX(point.day)
                val y = valueToY(point.value)
                if (index == 0) actualPath.moveTo(x, y)
                else actualPath.lineTo(x, y)
            }
            
            // Gradient fill under actual line
            val fillPath = Path()
            chartData.actualProgress.take(animatedCount).forEachIndexed { index, point ->
                val x = dayToX(point.day)
                val y = valueToY(point.value)
                if (index == 0) {
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    fillPath.lineTo(x, y)
                }
            }
            if (animatedCount > 0) {
                val lastX = dayToX(chartData.actualProgress[animatedCount - 1].day)
                fillPath.lineTo(lastX, height)
                fillPath.close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientPurple.copy(alpha = 0.3f),
                        GradientPurple.copy(alpha = 0.05f)
                    )
                )
            )
            
            drawPath(
                path = actualPath,
                color = GradientPurple,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Current position dot
            if (animatedCount > 0) {
                val lastPoint = chartData.actualProgress[animatedCount - 1]
                val dotX = dayToX(lastPoint.day)
                val dotY = valueToY(lastPoint.value)
                
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = GradientPurple,
                    radius = 6.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
        
        // Draw projected progress line (dashed purple)
        if (chartData.projectedProgress.isNotEmpty() && animationProgress > 0.5f) {
            val projectedPath = Path()
            chartData.projectedProgress.forEachIndexed { index, point ->
                val x = dayToX(point.day)
                val y = valueToY(point.value.coerceAtMost(maxValue))
                if (index == 0) projectedPath.moveTo(x, y)
                else projectedPath.lineTo(x, y)
            }
            drawPath(
                path = projectedPath,
                color = GradientPurple.copy(alpha = 0.5f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(6f, 6f)
                    )
                )
            )
        }
    }
}

@Composable
fun GoalConeAnimation(projection: GoalProjectionEngine.GoalProjection) {
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(projection.goal.id) {
        animationProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }
    
    val statusColor = getStatusColor(projection.status)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆 Goal Cone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Visual progress tracker",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Animated cone visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val coneHeight = size.height * 0.85f
                    val topY = size.height * 0.1f
                    val bottomY = topY + coneHeight
                    val maxWidth = size.width * 0.7f
                    
                    // Draw cone outline (goal zone)
                    val conePath = Path().apply {
                        moveTo(centerX, topY) // Top point
                        lineTo(centerX - maxWidth / 2, bottomY) // Bottom left
                        lineTo(centerX + maxWidth / 2, bottomY) // Bottom right
                        close()
                    }
                    
                    drawPath(
                        path = conePath,
                        color = Color.Gray.copy(alpha = 0.1f)
                    )
                    
                    drawPath(
                        path = conePath,
                        color = Color.Gray.copy(alpha = 0.3f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Draw progress fill (animated)
                    val progress = (projection.percentCompleted / 100f * animationProgress).coerceIn(0f, 1f)
                    val fillHeight = coneHeight * progress
                    val fillBottomY = bottomY
                    val fillTopY = fillBottomY - fillHeight
                    
                    // Calculate width at the fill top position
                    val progressRatio = fillHeight / coneHeight
                    val fillTopWidth = maxWidth * (1 - progressRatio) // Wider at bottom, narrower at top
                    val fillBottomWidth = maxWidth
                    
                    val fillPath = Path().apply {
                        // Start from bottom left, go up, across top, back down
                        moveTo(centerX - fillBottomWidth / 2, fillBottomY)
                        lineTo(centerX - fillTopWidth / 2, fillTopY)
                        lineTo(centerX + fillTopWidth / 2, fillTopY)
                        lineTo(centerX + fillBottomWidth / 2, fillBottomY)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.8f),
                                statusColor.copy(alpha = 0.5f)
                            )
                        )
                    )
                    
                    // Draw markers
                    listOf(0.25f, 0.5f, 0.75f).forEach { marker ->
                        val markerY = bottomY - (coneHeight * marker)
                        val markerRatio = marker
                        val markerWidth = maxWidth * (1 - markerRatio)
                        
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(centerX - markerWidth / 2, markerY),
                            end = Offset(centerX + markerWidth / 2, markerY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // Draw progress indicator line
                    if (progress > 0) {
                        val indicatorY = fillTopY
                        val indicatorWidth = fillTopWidth
                        
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - indicatorWidth / 2 + 4.dp.toPx(), indicatorY),
                            end = Offset(centerX + indicatorWidth / 2 - 4.dp.toPx(), indicatorY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress percentage
            Text(
                text = "${(projection.percentCompleted * animationProgress).roundToInt()}% Complete",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            
            if (projection.willFinishOnTime) {
                Text(
                    text = "🎯 On track to finish on time!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProjectionOnTrack
                )
            } else if (projection.estimatedCompletionDate != null) {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                Text(
                    text = "📅 Est. completion: ${dateFormat.format(Date(projection.estimatedCompletionDate))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProjectionSlightDelay
                )
            }
        }
    }
}

@Composable
fun ProjectionRecommendationCard(projection: GoalProjectionEngine.GoalProjection) {
    val statusColor = getStatusColor(projection.status)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Recommendation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Main recommendation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Text(
                    text = projection.recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }
            
            // Pace adjustment if needed
            if (projection.paceAdjustment > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProjectionSlightDelay.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏱️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Adjustment Needed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${projection.originalPace.roundToInt()} min → ${projection.requiredPace.roundToInt()} min (+${projection.paceAdjustment} min)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "+${projection.paceAdjustment}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProjectionSlightDelay
                    )
                }
            }
            
            // Recent performance insight
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RecentPerformanceChip(
                    label = "Last 7 days",
                    value = "${projection.recentPerformance.last7DaysAverage.roundToInt()} min/day",
                    trend = projection.recentPerformance.trend
                )
                RecentPerformanceChip(
                    label = "Consistency",
                    value = "${projection.recentPerformance.consistencyScore.roundToInt()}%",
                    trend = null
                )
                RecentPerformanceChip(
                    label = "Confidence",
                    value = "${projection.confidenceLevel.roundToInt()}%",
                    trend = null
                )
            }
        }
    }
}

@Composable
fun RecentPerformanceChip(
    label: String,
    value: String,
    trend: GoalProjectionEngine.PerformanceTrend?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (trend != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (trend) {
                        GoalProjectionEngine.PerformanceTrend.IMPROVING -> "↑"
                        GoalProjectionEngine.PerformanceTrend.STABLE -> "→"
                        GoalProjectionEngine.PerformanceTrend.DECLINING -> "↓"
                    },
                    color = when (trend) {
                        GoalProjectionEngine.PerformanceTrend.IMPROVING -> ProjectionOnTrack
                        GoalProjectionEngine.PerformanceTrend.STABLE -> Color.Gray
                        GoalProjectionEngine.PerformanceTrend.DECLINING -> ProjectionBehind
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

private fun getStatusColor(status: GoalProjectionEngine.GoalStatus): Color {
    return when (status) {
        GoalProjectionEngine.GoalStatus.ON_TRACK -> ProjectionOnTrack
        GoalProjectionEngine.GoalStatus.SLIGHT_DELAY -> ProjectionSlightDelay
        GoalProjectionEngine.GoalStatus.BEHIND_SCHEDULE -> ProjectionBehind
        GoalProjectionEngine.GoalStatus.COMPLETED -> ProjectionCompleted
        GoalProjectionEngine.GoalStatus.NOT_STARTED -> Color.Gray
    }
}

// ==================== THEMED COMPONENTS ====================

@Composable
fun ThemedAnalyticsHeader(
    aiReport: AiAnalyticsEngine.AnalyticsReport?,
    theme: AnalyticsTheme,
    onThemeClick: () -> Unit
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedScore by animateFloatAsState(
        targetValue = if (animationPlayed) (aiReport?.overallScore?.toFloat() ?: 0f) else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )

    LaunchedEffect(aiReport) {
        animationPlayed = true
    }

    val cardShape = RoundedCornerShape(theme.shapes.cardCornerRadius)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(theme.elevation.card, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            theme.colors.accentPrimary.copy(alpha = 0.1f),
                            theme.colors.accentSecondary.copy(alpha = 0.1f),
                            theme.colors.accentTertiary.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(theme.spacing.cardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📊", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Analytics",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = theme.colors.textPrimary
                            )
                            Text(
                                text = "Your productivity insights",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.colors.textSecondary
                            )
                        }
                    }
                }

                // Score circle
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = animatedScore / 100f,
                        modifier = Modifier.size(56.dp),
                        color = theme.colors.accentPrimary,
                        trackColor = theme.colors.progressTrack,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "${animatedScore.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.colors.textPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Theme picker button
                IconButton(
                    onClick = onThemeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.colors.surfaceSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Change Theme",
                        tint = theme.colors.iconAccent
                    )
                }
            }
        }
    }
}

@Composable
fun ThemedQuickStatsRow(
    totalMinutes: Int,
    avgMinutes: Int,
    targetMet: Int,
    maxStreak: Int,
    theme: AnalyticsTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.sm)
    ) {
        ThemedQuickStatCard(
            emoji = "⏱️",
            value = "${totalMinutes / 60}h ${totalMinutes % 60}m",
            label = "Total",
            modifier = Modifier.weight(1f),
            theme = theme
        )
        ThemedQuickStatCard(
            emoji = "📈",
            value = "${avgMinutes}m",
            label = "Average",
            modifier = Modifier.weight(1f),
            theme = theme
        )
        ThemedQuickStatCard(
            emoji = "✅",
            value = "$targetMet%",
            label = "Target Met",
            modifier = Modifier.weight(1f),
            theme = theme
        )
        ThemedQuickStatCard(
            emoji = "🔥",
            value = "$maxStreak",
            label = "Best Streak",
            modifier = Modifier.weight(1f),
            theme = theme
        )
    }
}

@Composable
fun ThemedQuickStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    theme: AnalyticsTheme
) {
    val cardShape = RoundedCornerShape(theme.shapes.cardCornerRadius)
    
    Card(
        modifier = modifier.shadow(theme.elevation.low, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(theme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.colors.accentPrimary,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = theme.colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ThemedFilterChipsRow(
    currentFilter: AnalyticsFilter,
    onFilterSelected: (AnalyticsFilter) -> Unit,
    theme: AnalyticsTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.chipCornerRadius))
            .background(theme.colors.surfaceSecondary)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AnalyticsFilter.values().forEach { filter ->
            val isSelected = currentFilter == filter
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) theme.colors.cardBackground else Color.Transparent,
                animationSpec = tween(200),
                label = "filterBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) theme.colors.accentPrimary else theme.colors.textTertiary,
                animationSpec = tween(200),
                label = "filterText"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(theme.shapes.chipCornerRadius - 4.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFilterSelected(filter) }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ThemedActivityHeatmapCard(
    heatmapData: ActivityHeatmapGenerator.HeatmapData?,
    currentView: ActivityHeatmapGenerator.HeatmapView,
    onViewChanged: (ActivityHeatmapGenerator.HeatmapView) -> Unit,
    theme: AnalyticsTheme
) {
    val cardShape = RoundedCornerShape(theme.shapes.cardCornerRadius)
    var selectedCell by remember { mutableStateOf<ActivityHeatmapGenerator.HeatmapCell?>(null) }
    var showTooltip by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(theme.elevation.card, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = theme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(theme.spacing.cardPadding)
        ) {
            // View Type Tabs with theme
            ThemedHeatmapViewTabs(
                currentView = currentView,
                onViewChanged = onViewChanged,
                theme = theme
            )
            
            Spacer(modifier = Modifier.height(theme.spacing.lg))
            
            // Streak Info
            if (heatmapData != null && heatmapData.currentStreak > 0) {
                ThemedStreakBanner(
                    currentStreak = heatmapData.currentStreak,
                    longestStreak = heatmapData.longestStreak,
                    theme = theme
                )
                Spacer(modifier = Modifier.height(theme.spacing.md))
            }
            
            // Period Label
            if (heatmapData != null) {
                Text(
                    text = "${heatmapData.monthLabel} ${heatmapData.yearLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = theme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = theme.spacing.sm)
                )
            }
            
            // Heatmap Grid
            if (heatmapData != null) {
                ThemedHeatmapGrid(
                    heatmapData = heatmapData,
                    theme = theme,
                    onCellTapped = { cell ->
                        selectedCell = cell
                        showTooltip = true
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = theme.colors.accentPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(theme.spacing.md))
            
            // Legend
            ThemedHeatmapLegend(theme = theme)
            
            // Stats Row
            if (heatmapData != null) {
                Spacer(modifier = Modifier.height(theme.spacing.md))
                ThemedHeatmapStatsRow(
                    totalStudyMinutes = heatmapData.totalStudyMinutes,
                    totalActiveDays = heatmapData.totalActiveDays,
                    weekCount = heatmapData.weekCount,
                    theme = theme
                )
            }
        }
    }
    
    // Tooltip Dialog
    if (showTooltip && selectedCell != null) {
        ThemedHeatmapTooltipDialog(
            cell = selectedCell!!,
            onDismiss = { showTooltip = false },
            theme = theme
        )
    }
}

@Composable
fun ThemedHeatmapViewTabs(
    currentView: ActivityHeatmapGenerator.HeatmapView,
    onViewChanged: (ActivityHeatmapGenerator.HeatmapView) -> Unit,
    theme: AnalyticsTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.chipCornerRadius))
            .background(theme.colors.surfaceSecondary)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActivityHeatmapGenerator.HeatmapView.values().forEach { view ->
            val isSelected = currentView == view
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) theme.colors.cardBackground else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) theme.colors.accentPrimary else theme.colors.textTertiary,
                animationSpec = tween(200),
                label = "tabText"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(theme.shapes.chipCornerRadius - 4.dp))
                    .background(backgroundColor)
                    .pointerInput(view) {
                        detectTapGestures { onViewChanged(view) }
                    }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (view) {
                        ActivityHeatmapGenerator.HeatmapView.WEEKLY -> "Week"
                        ActivityHeatmapGenerator.HeatmapView.MONTHLY -> "Month"
                        ActivityHeatmapGenerator.HeatmapView.YEARLY -> "Year"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ThemedStreakBanner(
    currentStreak: Int,
    longestStreak: Int,
    theme: AnalyticsTheme
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedStreak by animateIntAsState(
        targetValue = if (animationPlayed) currentStreak else 0,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "streakAnimation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    val isOnFire = currentStreak >= 7
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cardCornerRadius / 2))
            .background(theme.colors.statusWarningLight)
            .padding(theme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🔥", fontSize = 24.sp)
        Spacer(modifier = Modifier.width(theme.spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$animatedStreak-day learning streak!",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.colors.statusWarning
            )
            if (longestStreak > currentStreak) {
                Text(
                    text = "Best: $longestStreak days",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.colors.statusWarning.copy(alpha = 0.8f)
                )
            }
        }
        if (isOnFire) {
            Text(
                text = "ON FIRE!",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.colors.statusError)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ThemedHeatmapGrid(
    heatmapData: ActivityHeatmapGenerator.HeatmapData,
    theme: AnalyticsTheme,
    onCellTapped: (ActivityHeatmapGenerator.HeatmapCell) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val cellsByWeek = heatmapData.cells.groupBy { it.weekIndex }
    val weeks = cellsByWeek.keys.sorted()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Day labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Spacer(modifier = Modifier.width(36.dp))
            
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            dayLabels.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.colors.textTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Heatmap rows (weeks)
        weeks.forEach { weekIndex ->
            val weekCells = cellsByWeek[weekIndex] ?: emptyList()
            val firstCell = weekCells.minByOrNull { it.dayOfWeek }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = firstCell?.let { dateFormat.format(Date(it.date)) } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.colors.textTertiary,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
                
                val dayOrder = listOf(2, 3, 4, 5, 6, 7, 1)
                dayOrder.forEach { dayOfWeek ->
                    val cell = weekCells.find { it.dayOfWeek == dayOfWeek }
                    ThemedHeatmapCellItem(
                        cell = cell,
                        theme = theme,
                        onTapped = { cell?.let { onCellTapped(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.ThemedHeatmapCellItem(
    cell: ActivityHeatmapGenerator.HeatmapCell?,
    theme: AnalyticsTheme,
    onTapped: () -> Unit
) {
    val cellColor = when {
        cell == null -> Color.Transparent
        !cell.isInCurrentMonth -> theme.colors.heatmapEmpty.copy(alpha = 0.3f)
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.NONE -> theme.colors.heatmapEmpty
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.LOW -> theme.colors.heatmapLevel1
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> theme.colors.heatmapLevel2
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.HIGH -> theme.colors.heatmapLevel3
        cell.activityLevel == ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> theme.colors.heatmapLevel4
        else -> theme.colors.heatmapEmpty
    }
    
    val borderModifier = if (cell?.isToday == true) {
        Modifier.border(2.dp, theme.colors.accentPrimary, RoundedCornerShape(theme.shapes.heatmapCellCornerRadius))
    } else {
        Modifier
    }
    
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(theme.shapes.heatmapCellCornerRadius))
            .background(cellColor)
            .then(borderModifier)
            .then(
                if (cell != null) {
                    Modifier.pointerInput(cell) {
                        detectTapGestures { onTapped() }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell?.isToday == true) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun ThemedHeatmapLegend(theme: AnalyticsTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = theme.colors.textTertiary,
            fontSize = 10.sp
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        listOf(
            theme.colors.heatmapEmpty,
            theme.colors.heatmapLevel1,
            theme.colors.heatmapLevel2,
            theme.colors.heatmapLevel3,
            theme.colors.heatmapLevel4
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = theme.colors.textTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ThemedHeatmapStatsRow(
    totalStudyMinutes: Int,
    totalActiveDays: Int,
    weekCount: Int,
    theme: AnalyticsTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cardCornerRadius / 2))
            .background(theme.colors.surfaceSecondary)
            .padding(theme.spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ThemedHeatmapStatItem(
            label = "Study Time",
            value = "${totalStudyMinutes / 60}h ${totalStudyMinutes % 60}m",
            theme = theme
        )
        ThemedHeatmapStatItem(
            label = "Active Days",
            value = "$totalActiveDays",
            theme = theme
        )
        ThemedHeatmapStatItem(
            label = "Weeks",
            value = "$weekCount",
            theme = theme
        )
    }
}

@Composable
fun ThemedHeatmapStatItem(
    label: String,
    value: String,
    theme: AnalyticsTheme
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = theme.colors.accentPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = theme.colors.textSecondary
        )
    }
}

@Composable
fun ThemedHeatmapTooltipDialog(
    cell: ActivityHeatmapGenerator.HeatmapCell,
    onDismiss: () -> Unit,
    theme: AnalyticsTheme
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.colors.cardBackground,
        shape = RoundedCornerShape(theme.shapes.dialogCornerRadius),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📅", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cell.formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.colors.textPrimary
                    )
                }
                if (cell.isToday) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.colors.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemedTooltipStatRow(
                    icon = "📚",
                    label = "Study Time",
                    value = "${cell.studyMinutes} min",
                    color = theme.colors.accentPrimary,
                    theme = theme
                )
                ThemedTooltipStatRow(
                    icon = "📱",
                    label = "Phone Usage",
                    value = "${cell.phoneMinutes} min",
                    color = theme.colors.accentSecondary,
                    theme = theme
                )
                ThemedTooltipStatRow(
                    icon = "✅",
                    label = "Tasks Done",
                    value = "${cell.tasksCompleted}",
                    color = theme.colors.statusSuccess,
                    theme = theme
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.colors.textSecondary
                    )
                    
                    val (activityText, activityColor) = when (cell.activityLevel) {
                        ActivityHeatmapGenerator.ActivityLevel.NONE -> "No Activity" to theme.colors.textTertiary
                        ActivityHeatmapGenerator.ActivityLevel.LOW -> "Low" to theme.colors.heatmapLevel1
                        ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> "Moderate" to theme.colors.heatmapLevel2
                        ActivityHeatmapGenerator.ActivityLevel.HIGH -> "Good" to theme.colors.heatmapLevel3
                        ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> "Excellent" to theme.colors.heatmapLevel4
                    }
                    
                    Text(
                        text = activityText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = activityColor
                    )
                }
                
                val progressValue = when (cell.activityLevel) {
                    ActivityHeatmapGenerator.ActivityLevel.NONE -> 0f
                    ActivityHeatmapGenerator.ActivityLevel.LOW -> 0.25f
                    ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> 0.5f
                    ActivityHeatmapGenerator.ActivityLevel.HIGH -> 0.75f
                    ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> 1f
                }
                
                LinearProgressIndicator(
                    progress = progressValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(theme.shapes.progressBarCornerRadius)),
                    color = when (cell.activityLevel) {
                        ActivityHeatmapGenerator.ActivityLevel.NONE -> theme.colors.textTertiary
                        ActivityHeatmapGenerator.ActivityLevel.LOW -> theme.colors.heatmapLevel1
                        ActivityHeatmapGenerator.ActivityLevel.MEDIUM -> theme.colors.heatmapLevel2
                        ActivityHeatmapGenerator.ActivityLevel.HIGH -> theme.colors.heatmapLevel3
                        ActivityHeatmapGenerator.ActivityLevel.VERY_HIGH -> theme.colors.heatmapLevel4
                    },
                    trackColor = theme.colors.progressTrack
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = theme.colors.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun ThemedTooltipStatRow(
    icon: String,
    label: String,
    value: String,
    color: Color,
    theme: AnalyticsTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = theme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Wrapper functions that call existing components with theme colors
@Composable
fun ThemedProjectionSummaryCard(
    summary: GoalProjectionEngine.ProjectionSummary,
    theme: AnalyticsTheme
) {
    ProjectionSummaryCard(summary = summary)
}

@Composable
fun ThemedGoalProjectionSelector(
    projections: List<GoalProjectionEngine.GoalProjection>,
    selectedProjection: GoalProjectionEngine.GoalProjection?,
    onGoalSelected: (Long?) -> Unit,
    theme: AnalyticsTheme
) {
    GoalProjectionSelector(
        projections = projections,
        selectedProjection = selectedProjection,
        onGoalSelected = onGoalSelected
    )
}

@Composable
fun ThemedGoalProjectionCard(
    projection: GoalProjectionEngine.GoalProjection,
    theme: AnalyticsTheme
) {
    GoalProjectionCard(projection = projection)
}

@Composable
fun ThemedProjectionForecastChart(
    projection: GoalProjectionEngine.GoalProjection,
    theme: AnalyticsTheme
) {
    ProjectionForecastChart(projection = projection)
}

@Composable
fun ThemedGoalConeAnimation(
    projection: GoalProjectionEngine.GoalProjection,
    theme: AnalyticsTheme
) {
    GoalConeAnimation(projection = projection)
}

@Composable
fun ThemedProjectionRecommendationCard(
    projection: GoalProjectionEngine.GoalProjection,
    theme: AnalyticsTheme
) {
    ProjectionRecommendationCard(projection = projection)
}

@Composable
fun ThemedAiInsightsSection(
    insights: List<AiAnalyticsEngine.AiInsight>,
    theme: AnalyticsTheme
) {
    AiInsightsSection(insights = insights)
}

@Composable
fun ThemedAiPredictionsSection(
    predictions: List<AiAnalyticsEngine.AiPrediction>,
    theme: AnalyticsTheme
) {
    AiPredictionsSection(predictions = predictions)
}

@Composable
fun ThemedAiSuggestionsSection(
    suggestions: List<AiAnalyticsEngine.AiSuggestion>,
    theme: AnalyticsTheme
) {
    AiSuggestionsSection(suggestions = suggestions)
}

@Composable
fun ThemedGoalProgressCard(
    goalData: GoalProgressData,
    theme: AnalyticsTheme
) {
    GoalProgressCard(goalData = goalData)
}

@Composable
fun ThemedStudyTimeChart(
    data: List<DailyDataPoint>,
    filter: AnalyticsFilter,
    theme: AnalyticsTheme
) {
    StudyTimeChart(data = data, filter = filter)
}

@Composable
fun ThemedPhoneUsageChart(
    data: List<DailyDataPoint>,
    todayUsage: Int,
    status: UsageStatus,
    filter: AnalyticsFilter,
    theme: AnalyticsTheme
) {
    PhoneUsageChart(data = data, todayUsage = todayUsage, status = status, filter = filter)
}

@Composable
fun ThemedStreakVisualizationCard(
    goalData: GoalProgressData,
    theme: AnalyticsTheme
) {
    StreakVisualizationCard(goalData = goalData)
}

// Theme Selector Dialog
@Composable
fun ThemeSelectorDialog(
    currentTheme: AnalyticsThemeType,
    onThemeSelected: (AnalyticsThemeType) -> Unit,
    onDismiss: () -> Unit,
    theme: AnalyticsTheme
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.colors.cardBackground,
        shape = RoundedCornerShape(theme.shapes.dialogCornerRadius),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dashboard Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose a theme that matches your style",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.colors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnalyticsThemeType.values().forEach { themeType ->
                    val themeInfo = AnalyticsThemeManager.getThemeInfo(themeType)
                    val isSelected = currentTheme == themeType
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(theme.shapes.cardCornerRadius))
                            .background(
                                if (isSelected) theme.colors.accentPrimary.copy(alpha = 0.15f)
                                else theme.colors.surfaceSecondary
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) theme.colors.accentPrimary else Color.Transparent,
                                shape = RoundedCornerShape(theme.shapes.cardCornerRadius)
                            )
                            .clickable { onThemeSelected(themeType) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Theme color preview
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            themeInfo.previewColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = themeInfo.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.colors.textPrimary
                            )
                            Text(
                                text = themeInfo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.colors.textSecondary
                            )
                        }
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = theme.colors.accentPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = theme.colors.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

