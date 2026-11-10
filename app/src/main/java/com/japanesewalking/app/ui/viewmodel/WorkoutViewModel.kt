package com.japanesewalking.app.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.japanesewalking.app.JapaneseWalkingApp
import com.japanesewalking.app.data.model.GoalType
import com.japanesewalking.app.data.model.LiveWorkoutState
import com.japanesewalking.app.data.model.SessionStatus
import com.japanesewalking.app.data.model.WorkoutGoal
import com.japanesewalking.app.data.model.WorkoutSession
import com.japanesewalking.app.data.model.WorkoutSettings
import com.japanesewalking.app.service.WorkoutForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DarkThemeConfig {
    SYSTEM,
    LIGHT,
    DARK
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as JapaneseWalkingApp).repository
    private val prefs = application.getSharedPreferences("japanese_walking_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOUR_COMPLETED = "key_tour_completed"
        private const val KEY_PROMO_REDEEMED = "key_promo_redeemed"
        private const val KEY_PROMO_CODE = "key_promo_code"
        private const val KEY_DARK_THEME = "key_dark_theme"

        // Setting keys
        const val KEY_FAST_INTERVAL_SEC = "key_fast_interval_sec"
        const val KEY_NORMAL_INTERVAL_SEC = "key_normal_interval_sec"
        const val KEY_VOICE_ENABLED = "key_voice_enabled"
        const val KEY_CHIME_ENABLED = "key_chime_enabled"
        const val KEY_VIBRATION_ENABLED = "key_vibration_enabled"
        const val KEY_AUDIO_DUCKING = "key_audio_ducking"
        const val KEY_PERIODIC_DISTANCE = "key_periodic_distance"
        const val KEY_PERIODIC_DISTANCE_METERS = "key_periodic_distance_meters"

        val VALID_PROMO_CODES = listOf(
            "EARLYBIRD",
            "LAUNCHVIP",
            "PRODUCTHUNT",
            "EPICAPPS",
            "VIP2026",
            "WALKVIP",
            "SHINSHU"
        )
    }

    // Interactive Tour State
    private val _isTourCompleted = MutableStateFlow(prefs.getBoolean(KEY_TOUR_COMPLETED, false))
    val isTourCompleted: StateFlow<Boolean> = _isTourCompleted.asStateFlow()

    fun completeTour() {
        prefs.edit().putBoolean(KEY_TOUR_COMPLETED, true).apply()
        _isTourCompleted.value = true
    }

    // Promo Code State
    private val _isPromoRedeemed = MutableStateFlow(prefs.getBoolean(KEY_PROMO_REDEEMED, false))
    val isPromoRedeemed: StateFlow<Boolean> = _isPromoRedeemed.asStateFlow()

    private val _redeemedPromoCode = MutableStateFlow(prefs.getString(KEY_PROMO_CODE, null))
    val redeemedPromoCode: StateFlow<String?> = _redeemedPromoCode.asStateFlow()

    fun redeemPromoCode(code: String): Pair<Boolean, String> {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isEmpty()) {
            return Pair(false, "Please enter a promo code.")
        }
        return if (VALID_PROMO_CODES.contains(cleanCode)) {
            prefs.edit()
                .putBoolean(KEY_PROMO_REDEEMED, true)
                .putString(KEY_PROMO_CODE, cleanCode)
                .apply()
            _isPromoRedeemed.value = true
            _redeemedPromoCode.value = cleanCode
            Pair(true, "Promo code \"$cleanCode\" applied! Full access unlocked 🎉")
        } else {
            Pair(false, "Invalid or expired promo code.")
        }
    }

    // Theme Configuration
    private val _darkThemeConfig = MutableStateFlow(
        try {
            DarkThemeConfig.valueOf(prefs.getString(KEY_DARK_THEME, DarkThemeConfig.SYSTEM.name) ?: DarkThemeConfig.SYSTEM.name)
        } catch (e: Exception) {
            DarkThemeConfig.SYSTEM
        }
    )
    val darkThemeConfig: StateFlow<DarkThemeConfig> = _darkThemeConfig.asStateFlow()

    fun setDarkThemeConfig(config: DarkThemeConfig) {
        prefs.edit().putString(KEY_DARK_THEME, config.name).apply()
        _darkThemeConfig.value = config
    }

    val allSessions: StateFlow<List<WorkoutSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestSession: StateFlow<WorkoutSession?> = repository.latestSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalWorkoutsCount: StateFlow<Int> = repository.totalSessionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDurationSeconds: StateFlow<Long?> = repository.totalDurationSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalDistanceMeters: StateFlow<Double?> = repository.totalDistanceMeters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // User Configurable Settings with persistent SharedPreferences backing
    private val _settings = MutableStateFlow(
        WorkoutSettings(
            fastIntervalSeconds = prefs.getInt(KEY_FAST_INTERVAL_SEC, 180),
            normalIntervalSeconds = prefs.getInt(KEY_NORMAL_INTERVAL_SEC, 180),
            isVoiceEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, true),
            isChimeEnabled = prefs.getBoolean(KEY_CHIME_ENABLED, true),
            isVibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            audioDuckingEnabled = prefs.getBoolean(KEY_AUDIO_DUCKING, true),
            periodicDistanceUpdates = prefs.getBoolean(KEY_PERIODIC_DISTANCE, true),
            periodicDistanceIntervalMeters = prefs.getFloat(KEY_PERIODIC_DISTANCE_METERS, 1000.0f).toDouble()
        )
    )
    val settings: StateFlow<WorkoutSettings> = _settings.asStateFlow()

    // Configurable Goal for next workout
    private val _configuredGoal = MutableStateFlow(WorkoutGoal())
    val configuredGoal: StateFlow<WorkoutGoal> = _configuredGoal.asStateFlow()

    // Live Workout State
    private val _liveState = MutableStateFlow(LiveWorkoutState())
    val liveState: StateFlow<LiveWorkoutState> = _liveState.asStateFlow()

    // Last completed session for Summary Screen
    private val _summarySession = MutableStateFlow<WorkoutSession?>(null)
    val summarySession: StateFlow<WorkoutSession?> = _summarySession.asStateFlow()

    private var workoutService: WorkoutForegroundService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? WorkoutForegroundService.LocalBinder
            workoutService = localBinder?.getService()
            isBound = true

            workoutService?.let { service ->
                viewModelScope.launch {
                    service.state.collect { state ->
                        _liveState.value = state
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            workoutService = null
            isBound = false
        }
    }

    init {
        bindToServiceIfRunning()
    }

    private fun bindToServiceIfRunning() {
        val context = getApplication<Application>()
        val intent = Intent(context, WorkoutForegroundService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun updateGoalType(goalType: GoalType) {
        _configuredGoal.value = _configuredGoal.value.copy(type = goalType)
    }

    fun updateTargetSeconds(seconds: Int) {
        _configuredGoal.value = _configuredGoal.value.copy(targetSeconds = seconds)
    }

    fun updateTargetDistanceMeters(meters: Double) {
        _configuredGoal.value = _configuredGoal.value.copy(targetMeters = meters)
    }

    fun updateIntervalDurations(fastSeconds: Int, normalSeconds: Int) {
        prefs.edit()
            .putInt(KEY_FAST_INTERVAL_SEC, fastSeconds)
            .putInt(KEY_NORMAL_INTERVAL_SEC, normalSeconds)
            .apply()
        _settings.value = _settings.value.copy(
            fastIntervalSeconds = fastSeconds,
            normalIntervalSeconds = normalSeconds
        )
    }

    fun toggleVoiceCoach(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(isVoiceEnabled = enabled)
    }

    fun toggleChime(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CHIME_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(isChimeEnabled = enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(isVibrationEnabled = enabled)
    }

    fun toggleAudioDucking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_DUCKING, enabled).apply()
        _settings.value = _settings.value.copy(audioDuckingEnabled = enabled)
    }

    fun togglePeriodicDistance(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERIODIC_DISTANCE, enabled).apply()
        _settings.value = _settings.value.copy(periodicDistanceUpdates = enabled)
    }

    fun startWorkout() {
        val context = getApplication<Application>()
        try {
            WorkoutForegroundService.startWorkout(
                context = context,
                fastSeconds = _settings.value.fastIntervalSeconds,
                normalSeconds = _settings.value.normalIntervalSeconds,
                goal = _configuredGoal.value
            )
            bindToServiceIfRunning()
        } catch (e: Exception) {
            android.util.Log.e("WorkoutViewModel", "Error starting workout service", e)
        }
    }

    fun pauseWorkout() {
        val context = getApplication<Application>()
        WorkoutForegroundService.pauseWorkout(context)
    }

    fun resumeWorkout() {
        val context = getApplication<Application>()
        WorkoutForegroundService.resumeWorkout(context)
    }

    fun stopWorkout() {
        workoutService?.let { service ->
            val summary = service.stopWorkoutSession()
            _summarySession.value = summary
        } ?: run {
            val context = getApplication<Application>()
            WorkoutForegroundService.stopWorkout(context)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore if unbind fails
            }
            isBound = false
        }
    }
}
