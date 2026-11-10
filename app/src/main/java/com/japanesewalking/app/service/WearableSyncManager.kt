package com.japanesewalking.app.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.japanesewalking.app.data.model.LiveWorkoutState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object WearableSyncManager {
    private const val TAG = "WearableSyncManager"
    private const val STATE_PATH = "/japanese_walking/state"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun broadcastState(context: Context, state: LiveWorkoutState) {
        scope.launch {
            try {
                // 1. DataClient: High-priority urgent persistent state sync
                val putDataMapRequest = PutDataMapRequest.create(STATE_PATH).apply {
                    dataMap.putString("status", state.status.name)
                    dataMap.putString("intervalType", state.currentInterval.name)
                    dataMap.putInt("remainingSeconds", state.intervalRemainingSeconds)
                    dataMap.putInt("intervalTotalSeconds", state.intervalTotalSeconds)
                    dataMap.putInt("totalElapsedSeconds", state.totalElapsedSeconds.toInt())
                    dataMap.putInt("completedSets", state.completedSets)
                    dataMap.putDouble("distanceMeters", state.distanceMeters)
                    dataMap.putDouble("speedKmh", state.currentSpeedKmh)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val request = putDataMapRequest.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(request)

                // 2. MessageClient: Direct real-time RPC push to all connected watch nodes
                val nodeClient = Wearable.getNodeClient(context)
                val nodes: List<Node> = nodeClient.connectedNodes.await()
                if (nodes.isNotEmpty()) {
                    val json = JSONObject().apply {
                        put("status", state.status.name)
                        put("intervalType", state.currentInterval.name)
                        put("remainingSeconds", state.intervalRemainingSeconds)
                        put("intervalTotalSeconds", state.intervalTotalSeconds)
                        put("totalElapsedSeconds", state.totalElapsedSeconds.toInt())
                        put("completedSets", state.completedSets)
                        put("distanceMeters", state.distanceMeters)
                        put("speedKmh", state.currentSpeedKmh)
                    }
                    val payload = json.toString().toByteArray(Charsets.UTF_8)
                    val messageClient = Wearable.getMessageClient(context)
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, STATE_PATH, payload)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast state to Wear OS", e)
            }
        }
    }
}
