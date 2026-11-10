package com.japanesewalking.app.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.japanesewalking.app.JapaneseWalkingApp
import com.japanesewalking.app.MainActivity
import com.japanesewalking.app.R
import com.japanesewalking.app.data.model.GoalType
import com.japanesewalking.app.data.model.IntervalType
import com.japanesewalking.app.data.model.LiveWorkoutState
import com.japanesewalking.app.data.model.SessionStatus
import com.japanesewalking.app.data.model.WorkoutGoal
import com.japanesewalking.app.data.model.WorkoutSession
import com.japanesewalking.app.data.model.WorkoutSettings
import com.japanesewalking.app.widget.WorkoutAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class WorkoutForegroundService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var scheduledExecutor: ScheduledExecutorService? = null

    private lateinit var audioCoach: AudioCoachManager
    private lateinit var vibrationManager: VibrationManager
    private lateinit var locationTracker: LocationTracker
    private val silentAudioKeeper = SilentAudioKeeper()

    private var settings = WorkoutSettings()
    private var goal = WorkoutGoal()

    private val _state = MutableStateFlow(LiveWorkoutState())
    val state: StateFlow<LiveWorkoutState> = _state.asStateFlow()

    private var lastAnnouncedKm = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var cadenceDistanceMeters = 0.0

    // Monotonic real-time tracking for pocket & lock-screen immunity
    private var sessionStartRealtimeMillis: Long = 0L
    private var totalPausedMillis: Long = 0L
    private var pauseStartRealtimeMillis: Long = 0L
    private var intervalStartTotalSeconds: Long = 0L
    private var hasAnnouncedHalfTime: Boolean = false
    private var hasAnnouncedCountdown: Boolean = false
    private var tickCounter: Long = 0L

    inner class LocalBinder : Binder() {
        fun getService(): WorkoutForegroundService = this@WorkoutForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        loadSettingsFromPrefs()
        audioCoach = AudioCoachManager(this)
        vibrationManager = VibrationManager(this)
        locationTracker = LocationTracker(this)

        serviceScope.launch {
            locationTracker.distanceFlow.collect { dist ->
                updateDistanceAndMetrics(dist)
            }
        }

        serviceScope.launch {
            locationTracker.speedKmhFlow.collect { spd ->
                _state.value = _state.value.copy(currentSpeedKmh = spd)
            }
        }
    }

    private fun loadSettingsFromPrefs() {
        try {
            val prefs = getSharedPreferences("japanese_walking_prefs", Context.MODE_PRIVATE)
            settings = settings.copy(
                fastIntervalSeconds = prefs.getInt("key_fast_interval_sec", settings.fastIntervalSeconds),
                normalIntervalSeconds = prefs.getInt("key_normal_interval_sec", settings.normalIntervalSeconds),
                isVoiceEnabled = prefs.getBoolean("key_voice_enabled", settings.isVoiceEnabled),
                isChimeEnabled = prefs.getBoolean("key_chime_enabled", settings.isChimeEnabled),
                isVibrationEnabled = prefs.getBoolean("key_vibration_enabled", settings.isVibrationEnabled),
                audioDuckingEnabled = prefs.getBoolean("key_audio_ducking", settings.audioDuckingEnabled),
                periodicDistanceUpdates = prefs.getBoolean("key_periodic_distance", settings.periodicDistanceUpdates),
                periodicDistanceIntervalMeters = prefs.getFloat("key_periodic_distance_meters", settings.periodicDistanceIntervalMeters.toFloat()).toDouble()
            )
        } catch (e: Exception) {
            Log.w("WorkoutService", "Failed loading settings from prefs", e)
        }
    }

    private fun speakAnnouncement(text: String, playChimeBefore: Boolean = true) {
        if (!settings.isVoiceEnabled) return
        audioCoach.speak(
            text = text,
            playChimeBefore = playChimeBefore && settings.isChimeEnabled,
            useDucking = settings.audioDuckingEnabled
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                loadSettingsFromPrefs()
                val fastSec = intent.getIntExtra(EXTRA_FAST_SEC, settings.fastIntervalSeconds)
                val normalSec = intent.getIntExtra(EXTRA_NORMAL_SEC, settings.normalIntervalSeconds)
                val goalTypeName = intent.getStringExtra(EXTRA_GOAL_TYPE) ?: GoalType.TIME_BOUND.name
                val goalTargetSec = intent.getIntExtra(EXTRA_TARGET_SEC, 30 * 60)
                val goalTargetMeters = intent.getDoubleExtra(EXTRA_TARGET_METERS, 3000.0)

                settings = settings.copy(
                    fastIntervalSeconds = fastSec,
                    normalIntervalSeconds = normalSec
                )
                goal = WorkoutGoal(
                    type = GoalType.valueOf(goalTypeName),
                    targetSeconds = goalTargetSec,
                    targetMeters = goalTargetMeters
                )
                startWorkoutSession()
            }
            ACTION_PAUSE -> pauseWorkoutSession()
            ACTION_RESUME -> resumeWorkoutSession()
            ACTION_SKIP -> skipInterval()
            ACTION_STOP -> stopWorkoutSession()
            ACTION_WAKEUP_ALARM -> handleWakeupAlarm()
        }
        return START_STICKY
    }

    private fun startWorkoutSession() {
        lastAnnouncedKm = 0
        cadenceDistanceMeters = 0.0
        sessionStartRealtimeMillis = SystemClock.elapsedRealtime()
        totalPausedMillis = 0L
        pauseStartRealtimeMillis = 0L
        intervalStartTotalSeconds = 0L
        hasAnnouncedHalfTime = false
        hasAnnouncedCountdown = false
        tickCounter = 0L

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JapaneseWalking:WorkoutWakeLock").apply {
                setReferenceCounted(false)
                acquire(3 * 60 * 60 * 1000L) // 3 hours timeout safety for background pocket tracking
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to acquire WakeLock", e)
        }

        locationTracker.reset()
        locationTracker.startTracking()

        _state.value = LiveWorkoutState(
            status = SessionStatus.RUNNING,
            currentInterval = IntervalType.FAST,
            intervalRemainingSeconds = settings.fastIntervalSeconds,
            intervalTotalSeconds = settings.fastIntervalSeconds,
            intervalElapsedSeconds = 0,
            totalElapsedSeconds = 0,
            fastElapsedSeconds = 0,
            normalElapsedSeconds = 0,
            distanceMeters = 0.0,
            currentSpeedKmh = 6.0,
            completedSets = 0,
            caloriesKcal = 0,
            goal = goal
        )

        startForegroundWithNotification()

        try {
            WearableSyncManager.broadcastState(this, _state.value)
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to broadcast initial state to watch", e)
        }

        if (settings.isVibrationEnabled) {
            vibrationManager.vibrateFastInterval()
        }
        speakAnnouncement("Starting Japanese Interval Walking. Fast walk interval starts now. Speed up your pace!")

        startTicker()
        silentAudioKeeper.start()
        scheduleNextMilestoneAlarm()
    }

    private fun startTicker() {
        stopTicker()
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "JapaneseWalking-TickerThread").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        scheduledExecutor?.scheduleAtFixedRate({
            try {
                onTick()
            } catch (e: Exception) {
                Log.e("WorkoutService", "Exception in background onTick", e)
            }
        }, 1000L, 1000L, TimeUnit.MILLISECONDS)
    }

    private fun stopTicker() {
        try {
            scheduledExecutor?.shutdownNow()
        } catch (e: Exception) {
            Log.e("WorkoutService", "Error shutting down scheduledExecutor", e)
        }
        scheduledExecutor = null
    }

    private fun handleWakeupAlarm() {
        if (_state.value.status != SessionStatus.RUNNING) return

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val tempLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JapaneseWalking:AlarmTTSLock")
            tempLock.acquire(15_000L) // 15 seconds to ensure audio coach finishes speech
        } catch (e: Exception) {
            Log.w("WorkoutService", "Failed acquiring temp lock", e)
        }

        onTick()
        scheduleNextMilestoneAlarm()
    }

    private fun scheduleNextMilestoneAlarm() {
        if (_state.value.status != SessionStatus.RUNNING) return

        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val now = SystemClock.elapsedRealtime()
            val totalElapsedSec = ((now - sessionStartRealtimeMillis - totalPausedMillis) / 1000L).coerceAtLeast(0L)
            val currentInterval = _state.value.currentInterval
            val intervalDuration = if (currentInterval == IntervalType.FAST) settings.fastIntervalSeconds else settings.normalIntervalSeconds
            val intervalElapsed = (totalElapsedSec - intervalStartTotalSeconds).toInt().coerceAtLeast(0)
            val halfTimeSec = intervalDuration / 2

            val nextTargetSec = when {
                !hasAnnouncedHalfTime && intervalElapsed < halfTimeSec -> halfTimeSec
                !hasAnnouncedCountdown && intervalElapsed < (intervalDuration - 5) -> (intervalDuration - 5)
                else -> intervalDuration
            }

            val targetTotalSec = intervalStartTotalSeconds + nextTargetSec
            val targetRealtimeMillis = sessionStartRealtimeMillis + totalPausedMillis + (targetTotalSec * 1000L)
            val triggerAtMillis = maxOf(targetRealtimeMillis, now + 1000L)

            val alarmIntent = Intent(this, WorkoutForegroundService::class.java).apply {
                action = ACTION_WAKEUP_ALARM
            }
            val pendingIntent = PendingIntent.getService(
                this,
                999,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (se: SecurityException) {
                // If permission not granted, setAndAllowWhileIdle works without any special permission!
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to schedule exact wakeup alarm", e)
        }
    }

    private fun cancelMilestoneAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val alarmIntent = Intent(this, WorkoutForegroundService::class.java).apply {
                action = ACTION_WAKEUP_ALARM
            }
            val pendingIntent = PendingIntent.getService(
                this,
                999,
                alarmIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to cancel milestone alarm", e)
        }
    }

    private fun onTick() {
        val curr = _state.value
        if (curr.status != SessionStatus.RUNNING) return

        // Ensure WakeLock remains firmly held while running in pocket
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w("WorkoutService", "Failed verifying wakeLock state", e)
        }

        tickCounter++

        // Wall-clock monotonic elapsed seconds (immune to CPU sleeping/Doze delays)
        val now = SystemClock.elapsedRealtime()
        val totalElapsedSec = ((now - sessionStartRealtimeMillis - totalPausedMillis) / 1000L).coerceAtLeast(0L)

        var currentInterval = curr.currentInterval
        var completedSets = curr.completedSets
        var fastElapsed = curr.fastElapsedSeconds
        var normalElapsed = curr.normalElapsedSeconds

        var intervalDuration = if (currentInterval == IntervalType.FAST) settings.fastIntervalSeconds else settings.normalIntervalSeconds
        var intervalElapsed = (totalElapsedSec - intervalStartTotalSeconds).toInt().coerceAtLeast(0)
        var intervalRemaining = (intervalDuration - intervalElapsed).coerceAtLeast(0)

        // Interval transition check
        if (intervalRemaining <= 0) {
            if (currentInterval == IntervalType.FAST) {
                // Transition: FAST -> NORMAL
                currentInterval = IntervalType.NORMAL
                intervalDuration = settings.normalIntervalSeconds
                intervalStartTotalSeconds = totalElapsedSec
                intervalElapsed = 0
                intervalRemaining = intervalDuration
                hasAnnouncedHalfTime = false
                hasAnnouncedCountdown = false

                if (settings.isVibrationEnabled) vibrationManager.vibrateNormalInterval()
                speakAnnouncement("Fast interval complete! Slow down to a moderate recovery pace.")
                scheduleNextMilestoneAlarm()
            } else {
                // Transition: NORMAL -> FAST (Completed 1 full set!)
                completedSets++
                currentInterval = IntervalType.FAST
                intervalDuration = settings.fastIntervalSeconds
                intervalStartTotalSeconds = totalElapsedSec
                intervalElapsed = 0
                intervalRemaining = intervalDuration
                hasAnnouncedHalfTime = false
                hasAnnouncedCountdown = false

                if (settings.isVibrationEnabled) vibrationManager.vibrateFastInterval()
                speakAnnouncement("Set $completedSets completed! Speed up to brisk pace.")
                scheduleNextMilestoneAlarm()
            }
        } else {
            // Half-Time Announcement:
            // Checks if interval has reached exact halfway point (e.g. 1 min in a 2 min cycle)
            val halfTimeSec = intervalDuration / 2
            if (!hasAnnouncedHalfTime && intervalElapsed >= halfTimeSec && intervalRemaining > 5) {
                hasAnnouncedHalfTime = true
                announceHalfTime(halfTimeSec, currentInterval)
                scheduleNextMilestoneAlarm()
            } else if (!hasAnnouncedCountdown && intervalRemaining in 1..5) {
                hasAnnouncedCountdown = true
                speakAnnouncement("Change pace in 5 seconds", playChimeBefore = false)
                scheduleNextMilestoneAlarm()
            }
        }

        if (currentInterval == IntervalType.FAST) {
            fastElapsed++
        } else {
            normalElapsed++
        }

        // Update Cadence-based distance if GPS is indoors or stationary
        val metersThisSecond = if (currentInterval == IntervalType.FAST) (6.0 / 3.6) else (4.0 / 3.6)
        cadenceDistanceMeters += metersThisSecond
        val activeDistance = maxOf(curr.distanceMeters, cadenceDistanceMeters)

        // Speed: GPS speed if moving, else Japanese protocol cadence speed
        val activeSpeed = if (curr.currentSpeedKmh > 0.5) {
            curr.currentSpeedKmh
        } else {
            if (currentInterval == IntervalType.FAST) 6.0 else 4.0
        }

        // Calories estimate (MET standard: Fast ~5.0, Normal ~3.3 for 70kg user)
        val fastHours = fastElapsed / 3600.0
        val normalHours = normalElapsed / 3600.0
        val calories = ((5.0 * 70.0 * fastHours) + (3.3 * 70.0 * normalHours)).toInt().coerceAtLeast(1)

        // Average Speed
        val hours = totalElapsedSec / 3600.0
        val avgSpeed = if (hours > 0) (activeDistance / 1000.0) / hours else activeSpeed

        // Heart rate estimation based on intensity & speed
        val estHr = if (currentInterval == IntervalType.FAST) {
            (115 + (activeSpeed * 3)).toInt().coerceIn(110, 145)
        } else {
            (85 + (activeSpeed * 2)).toInt().coerceIn(80, 105)
        }

        // Goal reached check
        var isGoalReached = curr.isGoalReached
        if (!isGoalReached) {
            val reachedByTime = (goal.type == GoalType.TIME_BOUND && totalElapsedSec >= goal.targetSeconds)
            val reachedByDist = (goal.type == GoalType.DISTANCE_BOUND && activeDistance >= goal.targetMeters)

            if (reachedByTime || reachedByDist) {
                isGoalReached = true
                if (settings.isVibrationEnabled) vibrationManager.vibrateGoalCompleted()
                speakAnnouncement("Target goal achieved! Fantastic work. You can finish or continue your walk.")
            }
        }

        _state.value = curr.copy(
            intervalRemainingSeconds = intervalRemaining,
            intervalTotalSeconds = intervalDuration,
            intervalElapsedSeconds = intervalElapsed,
            currentInterval = currentInterval,
            totalElapsedSeconds = totalElapsedSec,
            fastElapsedSeconds = fastElapsed,
            normalElapsedSeconds = normalElapsed,
            distanceMeters = activeDistance,
            currentSpeedKmh = activeSpeed,
            completedSets = completedSets,
            caloriesKcal = calories,
            averageSpeedKmh = avgSpeed,
            estimatedHeartRateBpm = estHr,
            isGoalReached = isGoalReached
        )

        updateNotification()
    }

    private fun announceHalfTime(halfTimeSec: Int, intervalType: IntervalType) {
        val durationText = formatDurationSpoken(halfTimeSec)
        val encouragement = if (intervalType == IntervalType.FAST) {
            "Keep up the brisk pace!"
        } else {
            "Maintain your recovery pace."
        }
        val announcement = "Halfway mark: $durationText passed, $durationText to go. $encouragement"
        speakAnnouncement(announcement, playChimeBefore = true)
    }

    private fun formatDurationSpoken(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins > 0 && secs > 0 -> {
                val minText = if (mins == 1) "1 minute" else "$mins minutes"
                val secText = if (secs == 1) "1 second" else "$secs seconds"
                "$minText $secText"
            }
            mins > 0 -> {
                if (mins == 1) "1 minute" else "$mins minutes"
            }
            else -> {
                if (secs == 1) "1 second" else "$secs seconds"
            }
        }
    }

    private fun updateDistanceAndMetrics(distanceMeters: Double) {
        val curr = _state.value
        _state.value = curr.copy(distanceMeters = distanceMeters)

        // Periodic km announcement (e.g. 1 km, 2 km)
        if (settings.periodicDistanceUpdates) {
            val currentKm = (distanceMeters / 1000.0).toInt()
            if (currentKm > lastAnnouncedKm && currentKm > 0) {
                lastAnnouncedKm = currentKm
                val paceMinKm = if (distanceMeters > 0) {
                    ((curr.totalElapsedSeconds / 60.0) / (distanceMeters / 1000.0)).toInt()
                } else 0
                speakAnnouncement("$currentKm kilometer completed. Average pace: $paceMinKm minutes per kilometer.")
            }
        }
    }

    private fun pauseWorkoutSession() {
        stopTicker()
        silentAudioKeeper.stop()
        cancelMilestoneAlarm()
        pauseStartRealtimeMillis = SystemClock.elapsedRealtime()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed releasing wakeLock", e)
        }
        _state.value = _state.value.copy(status = SessionStatus.PAUSED)
        locationTracker.stopTracking()
        updateNotification()
        try {
            WorkoutAppWidgetProvider.notifyStateChanged(this, _state.value)
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed updating widget on pause", e)
        }
        speakAnnouncement("Workout paused.")
    }

    private fun resumeWorkoutSession() {
        _state.value = _state.value.copy(status = SessionStatus.RUNNING)
        if (pauseStartRealtimeMillis > 0L) {
            totalPausedMillis += (SystemClock.elapsedRealtime() - pauseStartRealtimeMillis)
            pauseStartRealtimeMillis = 0L
        }
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed acquiring wakeLock on resume", e)
        }
        locationTracker.startTracking()
        startTicker()
        silentAudioKeeper.start()
        scheduleNextMilestoneAlarm()
        updateNotification()
        try {
            WorkoutAppWidgetProvider.notifyStateChanged(this, _state.value)
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed updating widget on resume", e)
        }
        speakAnnouncement("Resuming Japanese walking. Pick up where you left off.")
    }

    fun skipInterval() {
        val curr = _state.value
        if (curr.status != SessionStatus.RUNNING && curr.status != SessionStatus.PAUSED) return
        val totalElapsedSec = curr.totalElapsedSeconds
        if (curr.currentInterval == IntervalType.FAST) {
            intervalStartTotalSeconds = totalElapsedSec - settings.fastIntervalSeconds
        } else {
            intervalStartTotalSeconds = totalElapsedSec - settings.normalIntervalSeconds
        }
        hasAnnouncedHalfTime = false
        hasAnnouncedCountdown = false
        onTick()
        updateNotification()
    }

    fun stopWorkoutSession(): WorkoutSession {
        stopTicker()
        silentAudioKeeper.stop()
        cancelMilestoneAlarm()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed releasing wakeLock on stop", e)
        }
        locationTracker.stopTracking()

        val s = _state.value
        _state.value = s.copy(status = SessionStatus.COMPLETED)

        speakAnnouncement("Workout complete! Great job completing your Japanese walking session.")

        val sessionRecord = WorkoutSession(
            durationSeconds = s.totalElapsedSeconds,
            distanceMeters = s.distanceMeters,
            fastDurationSeconds = s.fastElapsedSeconds,
            normalDurationSeconds = s.normalElapsedSeconds,
            completedSets = s.completedSets,
            caloriesKcal = s.caloriesKcal,
            averageSpeedKmh = s.averageSpeedKmh,
            goalType = goal.type.name,
            targetDurationSeconds = goal.targetSeconds,
            targetDistanceMeters = goal.targetMeters
        )

        // Persist session to local Room DB
        serviceScope.launch(Dispatchers.IO) {
            JapaneseWalkingApp.instance.repository.saveSession(sessionRecord)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return sessionRecord
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                startForeground(JapaneseWalkingApp.NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(JapaneseWalkingApp.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to startForeground with type, fallback to basic", e)
            try {
                startForeground(JapaneseWalkingApp.NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e("WorkoutService", "Fatal fallback failed", e2)
            }
        }
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(JapaneseWalkingApp.NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to update notification", e)
        }

        // Broadcast real-time state to connected Wear OS watch
        try {
            WearableSyncManager.broadcastState(this, _state.value)
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed broadcasting to watch", e)
        }

        // Throttle widget update to avoid excessive Binder IPC in pocket
        if (tickCounter % 5L == 0L || _state.value.intervalElapsedSeconds == 0) {
            try {
                WorkoutAppWidgetProvider.notifyStateChanged(this, _state.value)
            } catch (e: Exception) {
                Log.e("WorkoutService", "Failed to update widget", e)
            }
        }
    }

    private fun buildNotification(): Notification {
        val curr = _state.value
        val intervalTitle = if (curr.currentInterval == IntervalType.FAST) {
            "⚡ Fast Walk: ${formatTime(curr.intervalRemainingSeconds)}"
        } else {
            "🌿 Recovery Walk: ${formatTime(curr.intervalRemainingSeconds)}"
        }

        val contentText = "Dist: ${String.format("%.2f", curr.distanceMeters / 1000.0)} km | Sets: ${curr.completedSets} | Total: ${formatTime(curr.totalElapsedSeconds.toInt())}"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = if (curr.status == SessionStatus.RUNNING) ACTION_PAUSE else ACTION_RESUME
        }
        val pendingPauseResume = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = ACTION_SKIP
        }
        val pendingSkip = PendingIntent.getService(
            this, 3, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeActionTitle = if (curr.status == SessionStatus.RUNNING) "Pause" else "Resume"

        return NotificationCompat.Builder(this, JapaneseWalkingApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(intervalTitle)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(intervalTitle)
                    .bigText("$contentText\nCadence: ${String.format("%.1f", curr.currentSpeedKmh)} km/h | Burned: ${curr.caloriesKcal} kcal")
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpenApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, pauseResumeActionTitle, pendingPauseResume)
            .addAction(android.R.drawable.ic_media_next, "Skip", pendingSkip)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Finish", pendingStop)
            .build()
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed releasing wakeLock on destroy", e)
        }
        wakeLock = null
        stopTicker()
        silentAudioKeeper.stop()
        cancelMilestoneAlarm()
        locationTracker.stopTracking()
        audioCoach.release()
        try {
            WorkoutAppWidgetProvider.pushUpdateToAll(this, null)
        } catch (e: Exception) {
            Log.e("WorkoutService", "Failed to clear widget on destroy", e)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_WAKEUP_ALARM = "com.japanesewalking.app.ACTION_WAKEUP_ALARM"

        const val EXTRA_FAST_SEC = "EXTRA_FAST_SEC"
        const val EXTRA_NORMAL_SEC = "EXTRA_NORMAL_SEC"
        const val EXTRA_GOAL_TYPE = "EXTRA_GOAL_TYPE"
        const val EXTRA_TARGET_SEC = "EXTRA_TARGET_SEC"
        const val EXTRA_TARGET_METERS = "EXTRA_TARGET_METERS"

        fun startWorkout(
            context: Context,
            fastSeconds: Int = 180,
            normalSeconds: Int = 180,
            goal: WorkoutGoal = WorkoutGoal()
        ) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FAST_SEC, fastSeconds)
                putExtra(EXTRA_NORMAL_SEC, normalSeconds)
                putExtra(EXTRA_GOAL_TYPE, goal.type.name)
                putExtra(EXTRA_TARGET_SEC, goal.targetSeconds)
                putExtra(EXTRA_TARGET_METERS, goal.targetMeters)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseWorkout(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeWorkout(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopWorkout(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
