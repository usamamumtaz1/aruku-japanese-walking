package com.japanesewalking.wear.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object WearCommandSender {
    private const val TAG = "WearCommandSender"
    private const val COMMAND_PATH = "/japanese_walking/command"

    fun sendCommand(context: Context, command: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes: List<Node> = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected phone nodes found to send $command")
                    return@launch
                }

                val messageClient = Wearable.getMessageClient(context)
                val payload = command.toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, COMMAND_PATH, payload).await()
                    Log.d(TAG, "Sent $command to phone: ${node.displayName} (${node.id})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send $command to phone", e)
            }
        }
    }
}
