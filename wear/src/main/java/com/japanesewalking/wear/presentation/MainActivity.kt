package com.japanesewalking.wear.presentation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.japanesewalking.wear.datalayer.WearWorkoutManager
import com.japanesewalking.wear.datalayer.WearWorkoutState

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WearMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during active workout so walker can glance at wrist
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MaterialTheme {
                WearWorkoutScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkActivePhoneState()
    }

    private fun checkActivePhoneState() {
        try {
            Wearable.getDataClient(this).getDataItems(
                Uri.parse("wear://*/japanese_walking/state")
            ).addOnSuccessListener { buffer ->
                try {
                    for (item in buffer) {
                        if (item.uri.path == "/japanese_walking/state") {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            val status = dataMap.getString("status", "IDLE")
                            if (status == "RUNNING" || status == "PAUSED") {
                                val state = WearWorkoutState(
                                    status = status,
                                    intervalType = dataMap.getString("intervalType", "FAST"),
                                    remainingSeconds = dataMap.getInt("remainingSeconds", 180),
                                    intervalTotalSeconds = dataMap.getInt("intervalTotalSeconds", 180),
                                    totalElapsedSeconds = dataMap.getInt("totalElapsedSeconds", 0),
                                    completedSets = dataMap.getInt("completedSets", 0),
                                    distanceMeters = dataMap.getDouble("distanceMeters", 0.0),
                                    speedKmh = dataMap.getDouble("speedKmh", 0.0)
                                )
                                WearWorkoutManager.onPhoneStateReceived(this, state)
                            }
                        }
                    }
                } finally {
                    buffer.release()
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed checking active phone state", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkActivePhoneState", e)
        }
    }
}
