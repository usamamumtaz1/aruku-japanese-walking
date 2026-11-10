package com.japanesewalking.wear.datalayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.japanesewalking.wear.R
import com.japanesewalking.wear.presentation.MainActivity
import org.json.JSONObject

class WearDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataService"
        private const val STATE_PATH = "/japanese_walking/state"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "wear_workout_channel"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == STATE_PATH) {
                try {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val newState = WearWorkoutState(
                        status = dataMap.getString("status", "IDLE"),
                        intervalType = dataMap.getString("intervalType", "FAST"),
                        remainingSeconds = dataMap.getInt("remainingSeconds", 180),
                        intervalTotalSeconds = dataMap.getInt("intervalTotalSeconds", 180),
                        totalElapsedSeconds = dataMap.getInt("totalElapsedSeconds", 0),
                        completedSets = dataMap.getInt("completedSets", 0),
                        distanceMeters = dataMap.getDouble("distanceMeters", 0.0),
                        speedKmh = dataMap.getDouble("speedKmh", 0.0)
                    )

                    Log.d(TAG, "onDataChanged received: status=${newState.status}, interval=${newState.intervalType}")
                    handleStateUpdate(newState)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onDataChanged", e)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == STATE_PATH) {
            try {
                val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                val json = JSONObject(jsonStr)

                val newState = WearWorkoutState(
                    status = json.optString("status", "IDLE"),
                    intervalType = json.optString("intervalType", "FAST"),
                    remainingSeconds = json.optInt("remainingSeconds", 180),
                    intervalTotalSeconds = json.optInt("intervalTotalSeconds", 180),
                    totalElapsedSeconds = json.optInt("totalElapsedSeconds", 0),
                    completedSets = json.optInt("completedSets", 0),
                    distanceMeters = json.optDouble("distanceMeters", 0.0),
                    speedKmh = json.optDouble("speedKmh", 0.0)
                )

                Log.d(TAG, "onMessageReceived: status=${newState.status}, interval=${newState.intervalType}")
                handleStateUpdate(newState)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing state from phone message", e)
            }
        }
    }

    private fun handleStateUpdate(newState: WearWorkoutState) {
        WearWorkoutManager.onPhoneStateReceived(this, newState)

        when (newState.status) {
            "RUNNING" -> {
                showWorkoutNotification(newState)
                autoLaunchWatchUI()
            }
            "PAUSED" -> {
                showWorkoutNotification(newState)
            }
            "IDLE", "COMPLETED" -> {
                dismissWorkoutNotification()
            }
        }
    }

    private fun showWorkoutNotification(state: WearWorkoutState) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Japanese Walking",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Active Japanese walking workout tracker"
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isFast = state.intervalType == "FAST"
            val phaseName = if (isFast) "⚡ Fast Walk" else "🌿 Recovery Walk"
            val remainingMin = state.remainingSeconds / 60
            val remainingSec = state.remainingSeconds % 60
            val timeFormatted = String.format("%d:%02d", remainingMin, remainingSec)
            val title = if (state.status == "PAUSED") "⏸ Walk Paused" else phaseName
            val content = "$timeFormatted remaining • Set ${state.completedSets + 1}"

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed showing workout notification on watch", e)
        }
    }

    private fun dismissWorkoutNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed dismissing workout notification", e)
        }
    }

    private fun autoLaunchWatchUI() {
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed auto-launching watch UI (caught, fullScreenIntent notification handles wakeup)", e)
        }
    }
}
