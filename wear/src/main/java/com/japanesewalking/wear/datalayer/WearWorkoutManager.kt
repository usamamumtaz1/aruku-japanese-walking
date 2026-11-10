package com.japanesewalking.wear.datalayer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object WearWorkoutManager {
    private const val TAG = "WearWorkoutManager"

    private val _workoutState = MutableStateFlow(WearWorkoutState())
    val workoutState: StateFlow<WearWorkoutState> = _workoutState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    fun startWorkout(context: Context) {
        timerJob?.cancel()

        // 1. Instantly transition to active FAST WALK
        val initialState = WearWorkoutState(
            status = "RUNNING",
            intervalType = "FAST",
            remainingSeconds = 180,
            intervalTotalSeconds = 180,
            totalElapsedSeconds = 0,
            completedSets = 0,
            distanceMeters = 0.0,
            speedKmh = 6.0
        )
        _workoutState.value = initialState

        // 2. Trigger double-pulse vibration on wrist
        triggerWristHaptics(context, isFast = true)

        // 3. Command phone to start
        WearCommandSender.sendCommand(context, "START")

        // 4. Start standalone local ticker
        startLocalTicker(context)
    }

    fun pauseWorkout(context: Context) {
        timerJob?.cancel()
        _workoutState.value = _workoutState.value.copy(status = "PAUSED")
        WearCommandSender.sendCommand(context, "PAUSE")
    }

    fun resumeWorkout(context: Context) {
        _workoutState.value = _workoutState.value.copy(status = "RUNNING")
        WearCommandSender.sendCommand(context, "RESUME")
        startLocalTicker(context)
    }

    fun skipInterval(context: Context) {
        val curr = _workoutState.value
        if (curr.status != "RUNNING" && curr.status != "PAUSED") return

        val isNowFast = curr.intervalType != "FAST"
        val nextType = if (isNowFast) "FAST" else "NORMAL"
        val newSets = if (isNowFast) curr.completedSets + 1 else curr.completedSets

        _workoutState.value = curr.copy(
            intervalType = nextType,
            remainingSeconds = 180,
            intervalTotalSeconds = 180,
            completedSets = newSets,
            speedKmh = if (isNowFast) 6.0 else 4.0
        )

        triggerWristHaptics(context, isFast = isNowFast)
        WearCommandSender.sendCommand(context, "SKIP")
    }

    fun stopWorkout(context: Context) {
        timerJob?.cancel()
        _workoutState.value = WearWorkoutState(status = "IDLE")
        WearCommandSender.sendCommand(context, "STOP")
    }

    fun onPhoneStateReceived(context: Context, phoneState: WearWorkoutState) {
        val oldState = _workoutState.value

        // Wrist haptics if phone triggered an interval transition
        if (phoneState.status == "RUNNING" && oldState.intervalType != phoneState.intervalType) {
            triggerWristHaptics(context, isFast = phoneState.intervalType == "FAST")
        }

        _workoutState.value = phoneState

        // Keep local ticker synchronized if phone is active
        if (phoneState.status == "RUNNING" && timerJob?.isActive != true) {
            startLocalTicker(context)
        } else if (phoneState.status != "RUNNING") {
            timerJob?.cancel()
        }
    }

    private fun startLocalTicker(context: Context) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val curr = _workoutState.value
                if (curr.status != "RUNNING") break

                val newRemaining = curr.remainingSeconds - 1
                val newTotal = curr.totalElapsedSeconds + 1
                val metersThisSec = if (curr.intervalType == "FAST") (6.0 / 3.6) else (4.0 / 3.6)
                val newDistance = curr.distanceMeters + metersThisSec

                if (newRemaining <= 0) {
                    // Interval Complete! Flip FAST <-> NORMAL
                    val isNextFast = curr.intervalType != "FAST"
                    val nextType = if (isNextFast) "FAST" else "NORMAL"
                    val newCompletedSets = if (isNextFast) curr.completedSets + 1 else curr.completedSets

                    _workoutState.value = curr.copy(
                        intervalType = nextType,
                        remainingSeconds = 180,
                        intervalTotalSeconds = 180,
                        totalElapsedSeconds = newTotal,
                        completedSets = newCompletedSets,
                        distanceMeters = newDistance,
                        speedKmh = if (isNextFast) 6.0 else 4.0
                    )
                    triggerWristHaptics(context, isFast = isNextFast)
                } else {
                    _workoutState.value = curr.copy(
                        remainingSeconds = newRemaining,
                        totalElapsedSeconds = newTotal,
                        distanceMeters = newDistance
                    )
                }
            }
        }
    }

    fun triggerWristHaptics(context: Context, isFast: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isFast) {
                    // Double energetic buzz for FAST walk sprint!
                    val pattern = longArrayOf(0, 180, 80, 240)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    // Gentle soothing single pulse for RECOVERY walk
                    vibrator.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isFast) 300 else 150)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wrist vibration error", e)
        }
    }
}
