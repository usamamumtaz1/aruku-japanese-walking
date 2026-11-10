package com.japanesewalking.app.service

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.japanesewalking.app.MainActivity

class PhoneDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "PhoneDataListener"
        private const val COMMAND_PATH = "/japanese_walking/command"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == COMMAND_PATH) {
            val command = String(messageEvent.data, Charsets.UTF_8)
            Log.d(TAG, "Received command from watch: $command")

            when (command) {
                "START" -> {
                    try {
                        val intent = Intent(this, WorkoutForegroundService::class.java).apply {
                            action = WorkoutForegroundService.ACTION_START
                            putExtra(WorkoutForegroundService.EXTRA_FAST_SEC, 180)
                            putExtra(WorkoutForegroundService.EXTRA_NORMAL_SEC, 180)
                            putExtra(WorkoutForegroundService.EXTRA_TARGET_SEC, 30 * 60)
                        }
                        ContextCompat.startForegroundService(this, intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Direct FGS start restricted by Android 14+, launching via Activity", e)
                        val launchIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            action = "com.japanesewalking.app.AUTO_START_WORKOUT"
                        }
                        startActivity(launchIntent)
                    }
                }
                "PAUSE" -> {
                    val intent = Intent(this, WorkoutForegroundService::class.java).apply {
                        action = WorkoutForegroundService.ACTION_PAUSE
                    }
                    startService(intent)
                }
                "RESUME" -> {
                    val intent = Intent(this, WorkoutForegroundService::class.java).apply {
                        action = WorkoutForegroundService.ACTION_RESUME
                    }
                    startService(intent)
                }
                "SKIP" -> {
                    val intent = Intent(this, WorkoutForegroundService::class.java).apply {
                        action = WorkoutForegroundService.ACTION_SKIP
                    }
                    startService(intent)
                }
                "STOP" -> {
                    val intent = Intent(this, WorkoutForegroundService::class.java).apply {
                        action = WorkoutForegroundService.ACTION_STOP
                    }
                    startService(intent)
                }
            }
        }
    }
}
