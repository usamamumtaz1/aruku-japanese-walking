package com.japanesewalking.app.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Maintains an active zero-volume digital silence audio track during active workouts.
 *
 * Why this is essential for background pocket tracking:
 * 1. Linux Kernel Power Subsystem: Android's native `audioserver` holds an internal hardware
 *    kernel wake lock whenever an audio session is active.
 * 2. Process Freezer Immunity: The Linux `cgroups v2 freezer` strictly exempts processes with
 *    active media playback from being suspended when the screen display turns off.
 * 3. Bluetooth Warmth: Keeps the Bluetooth A2DP/SCO link alive so TTS voice prompts are heard
 *    immediately without cutting off the first syllable.
 * 4. Zero CPU Overhead: Uses MODE_STATIC with a tiny 1KB buffer of pure zeros (0% CPU impact).
 */
class SilentAudioKeeper {

    private var audioTrack: AudioTrack? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        try {
            val sampleRate = 8000
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(1024)

            val silenceBuffer = ByteArray(minBufferSize) // All zeros = absolute pure digital silence

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack(
                attributes,
                format,
                silenceBuffer.size,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(silenceBuffer, 0, silenceBuffer.size)
            track.setLoopPoints(0, silenceBuffer.size / 2, -1) // Infinite loop
            track.play()

            audioTrack = track
            isRunning = true
            Log.d(TAG, "SilentAudioKeeper started - background process immunity engaged")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SilentAudioKeeper", e)
        }
    }

    fun stop() {
        if (!isRunning) return
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop SilentAudioKeeper", e)
        }
        audioTrack = null
        isRunning = false
        Log.d(TAG, "SilentAudioKeeper stopped")
    }

    companion object {
        private const val TAG = "SilentAudioKeeper"
    }
}
