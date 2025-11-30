package com.example.todoapp.ui.viewmodels

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.R
import com.example.todoapp.data.local.DailyProgressEntity
import com.example.todoapp.data.local.GoalEntity
import com.example.todoapp.data.local.TimerSessionEntity
import com.example.todoapp.data.repository.DailyProgressRepository
import com.example.todoapp.data.repository.GoalRepository
import com.example.todoapp.data.repository.TimerSessionRepository
import com.example.todoapp.util.ProgressCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimerMode {
    STOPWATCH, COUNTDOWN
}

enum class TimerState {
    IDLE, RUNNING, PAUSED, COMPLETED
}

data class TimerUiState(
    val mode: TimerMode = TimerMode.STOPWATCH,
    val state: TimerState = TimerState.IDLE,
    val elapsedSeconds: Long = 0L,
    val countdownTotalSeconds: Long = 25 * 60L, // Default 25 min (Pomodoro)
    val remainingSeconds: Long = 25 * 60L,
    val goalTitle: String = "",
    val dailyTargetMinutes: Int = 60,
    val todayMinutesDone: Int = 0,
    val progressToTarget: Float = 0f,
    val showCompletionDialog: Boolean = false,
    val showGoalMissedAlert: Boolean = false,
    val currentStreak: Int = 0,
    val sessionStartTime: Long = 0L
)

class TimerViewModel(
    private val dailyProgressRepository: DailyProgressRepository,
    private val goalRepository: GoalRepository,
    private val timerSessionRepository: TimerSessionRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    // Legacy support
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null
    private var currentGoalId: Long? = null
    private var currentGoal: GoalEntity? = null

    init {
        createNotificationChannel()
    }

    fun initializeTimer(goalId: Long) {
        currentGoalId = goalId
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId)
            currentGoal = goal
            
            if (goal != null) {
                val today = getStartOfDay()
                val todayMinutes = timerSessionRepository.getTotalMinutesForGoalToday(goalId, today)
                val progress = todayMinutes.toFloat() / goal.dailyTargetMinutes.toFloat()
                
                _uiState.value = _uiState.value.copy(
                    goalTitle = goal.title,
                    dailyTargetMinutes = goal.dailyTargetMinutes,
                    todayMinutesDone = todayMinutes,
                    progressToTarget = progress.coerceIn(0f, 1f),
                    remainingSeconds = _uiState.value.countdownTotalSeconds
                )
            }
        }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_uiState.value.state == TimerState.IDLE) {
            _uiState.value = _uiState.value.copy(mode = mode)
        }
    }

    fun setCountdownDuration(minutes: Int) {
        if (_uiState.value.state == TimerState.IDLE) {
            val seconds = minutes * 60L
            _uiState.value = _uiState.value.copy(
                countdownTotalSeconds = seconds,
                remainingSeconds = seconds
            )
        }
    }

    fun startTimer() {
        val goalId = currentGoalId ?: return
        if (_uiState.value.state == TimerState.RUNNING) return
        
        val isNewSession = _uiState.value.state == TimerState.IDLE
        
        _uiState.value = _uiState.value.copy(
            state = TimerState.RUNNING,
            sessionStartTime = if (isNewSession) System.currentTimeMillis() else _uiState.value.sessionStartTime
        )
        _isRunning.value = true

        timerJob = viewModelScope.launch {
            while (_uiState.value.state == TimerState.RUNNING) {
                delay(1000)
                
                when (_uiState.value.mode) {
                    TimerMode.STOPWATCH -> {
                        val newElapsed = _uiState.value.elapsedSeconds + 1
                        _uiState.value = _uiState.value.copy(elapsedSeconds = newElapsed)
                        _elapsedSeconds.value = newElapsed
                        
                        // Update progress
                        updateTodayProgress()
                    }
                    TimerMode.COUNTDOWN -> {
                        val newRemaining = _uiState.value.remainingSeconds - 1
                        val newElapsed = _uiState.value.elapsedSeconds + 1
                        
                        if (newRemaining <= 0) {
                            // Countdown completed
                            _uiState.value = _uiState.value.copy(
                                remainingSeconds = 0,
                                elapsedSeconds = newElapsed,
                                state = TimerState.COMPLETED,
                                showCompletionDialog = true
                            )
                            _elapsedSeconds.value = newElapsed
                            onTimerCompleted()
                            break
                        } else {
                            _uiState.value = _uiState.value.copy(
                                remainingSeconds = newRemaining,
                                elapsedSeconds = newElapsed
                            )
                            _elapsedSeconds.value = newElapsed
                            updateTodayProgress()
                        }
                    }
                }
            }
        }
    }

    // Legacy support method
    fun startTimer(goalId: Long) {
        initializeTimer(goalId)
        viewModelScope.launch {
            delay(100) // Allow initialization to complete
            startTimer()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
        _isRunning.value = false
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            state = TimerState.IDLE,
            elapsedSeconds = 0,
            remainingSeconds = _uiState.value.countdownTotalSeconds,
            sessionStartTime = 0
        )
        _elapsedSeconds.value = 0
        _isRunning.value = false
    }

    fun stopTimer() {
        pauseTimer()
        saveSession()
        resetTimer()
    }

    private fun updateTodayProgress() {
        val goal = currentGoal ?: return
        val elapsedMinutes = (_uiState.value.elapsedSeconds / 60).toInt()
        val totalToday = _uiState.value.todayMinutesDone + elapsedMinutes
        val progress = totalToday.toFloat() / goal.dailyTargetMinutes.toFloat()
        
        _uiState.value = _uiState.value.copy(
            progressToTarget = progress.coerceIn(0f, 1f)
        )
    }

    private fun onTimerCompleted() {
        // Send notification
        sendCompletionNotification()
        
        // Vibrate
        vibrate()
        
        // Save session
        saveSession()
    }

    private fun saveSession() {
        val goalId = currentGoalId ?: return
        val seconds = _uiState.value.elapsedSeconds
        if (seconds < 60) return // Ignore sessions less than 1 minute

        val minutes = (seconds / 60).toInt()
        val today = getStartOfDay()
        val wasCompleted = _uiState.value.mode == TimerMode.COUNTDOWN && 
                          _uiState.value.state == TimerState.COMPLETED

        viewModelScope.launch {
            // Save timer session
            val session = TimerSessionEntity(
                goalId = goalId,
                startTime = _uiState.value.sessionStartTime,
                endTime = System.currentTimeMillis(),
                durationMinutes = minutes,
                mode = _uiState.value.mode.name.lowercase(),
                wasCompleted = wasCompleted,
                date = today
            )
            timerSessionRepository.insertSession(session)
            
            // Update daily progress
            val existingProgressList = dailyProgressRepository.getProgressByGoal(goalId).first()
            val todayProgress = existingProgressList.find { it.date == today }

            val newMinutes = (todayProgress?.minutesDone ?: 0) + minutes
            
            val goal = goalRepository.getGoalById(goalId) ?: return@launch
            val wasTargetMet = ProgressCalculator.checkDailyTarget(goal, newMinutes)

            val progressEntity = todayProgress?.copy(
                minutesDone = newMinutes,
                wasTargetMet = wasTargetMet
            ) ?: DailyProgressEntity(
                goalId = goalId,
                date = today,
                minutesDone = newMinutes,
                wasTargetMet = wasTargetMet
            )

            dailyProgressRepository.insertProgress(progressEntity)
            
            // Update UI state with new totals
            _uiState.value = _uiState.value.copy(
                todayMinutesDone = newMinutes,
                progressToTarget = (newMinutes.toFloat() / goal.dailyTargetMinutes).coerceIn(0f, 1f)
            )
        }
    }

    fun dismissCompletionDialog() {
        _uiState.value = _uiState.value.copy(showCompletionDialog = false)
    }

    fun dismissGoalMissedAlert() {
        _uiState.value = _uiState.value.copy(showGoalMissedAlert = false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for timer completion"
                enableVibration(true)
            }
            
            val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendCompletionNotification() {
        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(application, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎉 Session Complete!")
            .setContentText("Great job! You completed ${_uiState.value.elapsedSeconds / 60} minutes of focused work.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 200), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 200), -1)
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
