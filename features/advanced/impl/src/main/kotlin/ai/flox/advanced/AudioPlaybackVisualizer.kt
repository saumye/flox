package ai.flox.advanced

import ai.flox.advanced.ui.SpeechWavesView
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Arrays
class AudioPlaybackVisualizer(
    private val audioData: FloatArray,
    private val sampleRate: Int,
    private val speechWavesView: SpeechWavesView,
    private val updateIntervalMs: Long = 32,
    private val bufferSize: Int = 1024
) {
    val TAG = "AudioPlaybackVisualizer"
    private var playbackPosition = 0
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastTimestamp = System.currentTimeMillis()
    private var visualizationPosition = 0 // New variable for tracking visualization position

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                updateVisualization()
                handler.postDelayed(this, updateIntervalMs)
            }
        }
    }

    fun start(audioTrack: AudioTrack) {
        isPlaying = true
        playbackPosition = 0
        visualizationPosition = 0
        lastTimestamp = System.currentTimeMillis()
        handler.post(updateRunnable)

        // Set up periodic notifications
        val framesPerNotification = (sampleRate * updateIntervalMs / 1000).toInt()
        audioTrack.positionNotificationPeriod = framesPerNotification

        Log.d(TAG,"framesPerNotification:$framesPerNotification")
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) {
                stop()
                audioTrack.release()
                audioTrack.setPlaybackPositionUpdateListener(null)
                speechWavesView.update(ByteArray(bufferSize))
            }

            override fun onPeriodicNotification(track: AudioTrack) {
                if(audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    // Get the raw position from AudioTrack
                    playbackPosition = track.playbackHeadPosition

                    // Calculate elapsed time-based position instead of using AudioTrack's position directly
                    calculateTimeBasedPosition()

                    // Update visualization
                    updateVisualization()
                }
            }
        })
    }

    private fun calculateTimeBasedPosition() {
        val currentTime = System.currentTimeMillis()
        val elapsedMs = currentTime - lastTimestamp

        // Calculate how many samples to advance based on elapsed time and sample rate
        val samplesToAdvance = (elapsedMs * sampleRate / 1000).toInt()

        // Update our visualization position
        visualizationPosition += samplesToAdvance

        // Ensure we don't exceed the array bounds
        if (visualizationPosition >= audioData.size) {
            visualizationPosition = audioData.size - 1
        }

        // Update timestamp for next calculation
        lastTimestamp = currentTime
    }

    fun stop() {
        isPlaying = false
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateVisualization() {
        // Use our time-based position instead of AudioTrack's position
        val startSample = visualizationPosition

        // Ensure we don't exceed array bounds
        if (startSample >= audioData.size) {
            Log.d("AudioPlaybackVisualizer", "Stopping visualization - reached end of audio data")
            stop()
            return
        }

        // Create a buffer for visualization
        val buffer = ByteArray(bufferSize)

        // Find the maximum amplitude in the current window for normalization
        var maxAmplitude = 0.0f
        val samplesToProcess = minOf(bufferSize, audioData.size - startSample)

        for (i in 0 until samplesToProcess) {
            val amplitude = Math.abs(audioData[startSample + i])
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude
            }
        }

        // Ensure we get visualization even for quiet audio
        val amplificationFactor = if (maxAmplitude < 1.0e-5f) {
            1.0e7f
        } else {
            1.0f / maxAmplitude
        }

        // Fill buffer with amplified audio data
        for (i in 0 until samplesToProcess) {
            val amplifiedSample = audioData[startSample + i] * amplificationFactor
            buffer[i] = (amplifiedSample * 127).toInt().coerceIn(-128, 127).toByte()
        }

        // Fill remaining buffer with zeros
        for (i in samplesToProcess until bufferSize) {
            buffer[i] = 0
        }

        // Update the visualization
        speechWavesView.update(buffer)
    }
}