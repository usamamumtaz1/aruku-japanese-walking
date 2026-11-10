package com.japanesewalking.wear.datalayer

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.japanesewalking.wear.presentation.MainActivity
import org.json.JSONObject

class WearDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataService"
        private const val STATE_PATH = "/japanese_walking/state"
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
                    WearWorkoutManager.onPhoneStateReceived(this, newState)

                    if (newState.status == "RUNNING") {
                        autoLaunchWatchUI()
                    }
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
                WearWorkoutManager.onPhoneStateReceived(this, newState)

                if (newState.status == "RUNNING") {
                    autoLaunchWatchUI()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing state from phone message", e)
            }
        }
    }

    private fun autoLaunchWatchUI() {
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed auto-launching watch UI", e)
        }
    }
}
