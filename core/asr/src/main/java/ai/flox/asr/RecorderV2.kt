package ai.flox.asr

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "RecorderV2"

class RecorderV2 {
    private var audioStream: AudioStreamThread? = null

    fun startStreaming(onDataReceived: AudioDataReceivedListener, onError: (Exception) -> Unit) {
        if (audioStream == null) {
            audioStream = AudioStreamThread(onDataReceived, onError)
            audioStream?.start()
        } else {
            Log.i(TAG, "AudioStreamThread is already running")
        }
    }

    fun stopRecording() {
        audioStream?.stopRecording()
        runBlocking {
            audioStream?.join()
            audioStream = null
        }
    }

    interface AudioDataReceivedListener {
        fun onAudioDataReceived(data: FloatArray)
    }


    private class AudioStreamThread(
        private val onDataReceived: AudioDataReceivedListener,
        private val onError: (Exception) -> Unit
    ) : Thread("AudioStreamer") {
        private val quit = AtomicBoolean(false)

        @SuppressLint("MissingPermission")
        override fun run() {
            var bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT) * 4
            val floatBuffer = FloatArray(bufferSize / 2)
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufferSize)

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

//            listenerTimerInterval = encoder.getFrameSizeInMillis()
//            numFramesInASecond = 1000 / listenerTimerInterval
//
//            framePeriod = sampleRate * listenerTimerInterval / 1000
//
//            bufferSize = framePeriod * bSamples * nChannels / 8
//
//            if (splitFlag) {
//                splitAudioDurationInMillis = S2TConstants.BREAK_INTERVAL
//            } else {
//                splitAudioDurationInMillis = timerInterval * 1000
//            }
//
//            val fullBufferSize: Int =
//                bufferSize * ((timerInterval + 2) * 1000 / listenerTimerInterval + 1)
//            fileBuffer = ByteBuffer.allocate(fullBufferSize)

            try {
//                audioRecord.setRecordPositionUpdateListener(updateListener)
//                audioRecord.setPositionNotificationPeriod(framePeriod)
                audioRecord.startRecording()
                while (!quit.get()) {
                    val readResult = audioRecord.read(floatBuffer, 0, floatBuffer.size, AudioRecord.READ_BLOCKING)
                    Log.i(TAG, "readResult: $readResult")
                    if (readResult > 0) {
                        Log.i(TAG, "READING FROM THE floatBuffer")

                        onDataReceived.onAudioDataReceived(floatBuffer.copyOf(readResult))
                    } else if (readResult < 0) {
                        throw RuntimeException("AudioRecord.read error: $readResult")
                    }
                }
            } catch (e: Exception) {
                onError(e)
            } finally {
                audioRecord.stop()
                audioRecord.release()
            }
        }

//        private val updateListener: OnRecordPositionUpdateListener =
//            object : OnRecordPositionUpdateListener {
//                override fun onPeriodicNotification(audioRecord: AudioRecord) {
//                    val readResult = audioRecord.read(floatBuffer, 0, floatBuffer.size, AudioRecord.READ_BLOCKING)
//                    audioRecord.setRecordPositionUpdateListener()
//                    Log.i(TAG, "readResult: $readResult")
//                    if (readResult > 0) {
//                        Log.i(TAG, "READING FROM THE floatBuffer")
//
//                        onDataReceived.onAudioDataReceived(floatBuffer.copyOf(readResult))
//                    } else if (readResult < 0) {
//                        throw RuntimeException("AudioRecord.read error: $readResult")
//                    }
//                }
//
//                override fun onMarkerReached(recorder: AudioRecord) {
//                    // NOT USED
//                }
//            }

        fun stopRecording() {
            quit.set(true)
        }
    }
}
