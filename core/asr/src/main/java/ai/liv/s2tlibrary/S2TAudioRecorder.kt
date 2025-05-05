package ai.liv.s2tlibrary

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.core.app.ActivityCompat
import com.k2fsa.sherpa.onnx.Vad
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import kotlin.math.ceil
import java.util.concurrent.atomic.AtomicBoolean

// Configuration data class
data class AudioRecorderConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_FLOAT,
    // Base read buffer size - determines the smallest chunk read from AudioRecord.
    // Listener interval should ideally be >= this value.
    val readBufferSizeMs: Int = 50, // e.g., read 50ms chunks minimum
    // Target intervals for different consumers
    val listenerTimerIntervalMs: Int = 50, // Target interval for listener updates
    val vadTimerIntervalMs: Int = 500,    // Target interval for VAD updates
    val whisperTimerIntervalMs: Int = 2500 // Target interval for Whisper updates
)

// Sealed class for representing recorder state
sealed class RecorderState {
    object Idle : RecorderState()
    object Preparing : RecorderState()
    object Ready : RecorderState()
    object Recording : RecorderState()
    object Stopping : RecorderState()
    data class Error(val exception: Exception) : RecorderState()
}

class S2TAudioRecorder(
    private val context: Context,
    private val config: AudioRecorderConfig = AudioRecorderConfig(),
    private val vad: Vad? = null // Accept optional Vad instance
) {
    private val TAG = "S2TAudioRecorder"

    // Coroutine Dispatchers
    private val recordingDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val processingDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    // Main scope
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var audioRecord: AudioRecord? = null
    private var processingJob: Job? = null
    
    // For managing state transitions
    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    // StateFlow for VAD status
    private val _isSpeechDetected = MutableStateFlow<Boolean>(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    // --- Shared flows for different data consumers ---
    private val _listenerAudioData = MutableSharedFlow<FloatArray>(replay = 0, extraBufferCapacity = 64)
    val listenerAudioData: SharedFlow<FloatArray> = _listenerAudioData.asSharedFlow()
    private val _whisperAudioData = MutableSharedFlow<FloatArray>(replay = 0, extraBufferCapacity = 8)
    val whisperAudioData: SharedFlow<FloatArray> = _whisperAudioData.asSharedFlow()

    // --- Buffer size calculations ---
    // Frame period = number of samples in one notification period
    private val bytesPerFloat = 4 // For ENCODING_PCM_FLOAT
    private val framePeriod = (config.sampleRate * config.listenerTimerIntervalMs / 1000).toInt()
    private val bufferSize = framePeriod * bytesPerFloat
    private val readBuffer = FloatArray(framePeriod)
    
    // Buffers to accumulate data for different intervals
    private val listenerBuffer = mutableListOf<FloatArray>()
    private val whisperBuffer = mutableListOf<FloatArray>()
    
    // Counters for tracking periodic data emission
    private var readDataCount = 0
    private var vadReadDataCount = 0
    private var speechSilenceCounter = 0
    
    // Flag to prevent multiple simultaneous read operations
    private var isReading = AtomicBoolean(false)
    
    // Flag to indicate if we should stop
    private var shouldStop = AtomicBoolean(false)
    
    // Constants for speech detection debouncing
    private val SPEECH_SILENCE_DEBOUNCE_FRAMES = 2
    
    init {
        Log.d(TAG, "VAD integration enabled: ${vad != null}")
        Log.d(TAG, "Configured Frame Period: $framePeriod samples")
        Log.d(TAG, "Configured Buffer Size: $bufferSize bytes")
        Log.d(TAG, "Intervals: Listener=${config.listenerTimerIntervalMs}ms, VAD=${config.vadTimerIntervalMs}ms, Whisper=${config.whisperTimerIntervalMs}ms")
    }

    /**
     * Update listener that is triggered at each notification period
     */
    private val updateListener = object : AudioRecord.OnRecordPositionUpdateListener {
        override fun onPeriodicNotification(recorder: AudioRecord) {
            readDataFromBuffer()
        }

        override fun onMarkerReached(recorder: AudioRecord) {
            // Not used
        }
    }
    
    /**
     * Reads data from AudioRecord buffer and processes it
     */
    private fun readDataFromBuffer() {
        if (isReading.getAndSet(true)) {
            return  // Another read is in progress
        }
        
        try {
            val recorder = audioRecord ?: return
            
            // Read from AudioRecord - this should always return exactly framePeriod samples
            // because the notification is precisely timed
            val readResult = recorder.read(readBuffer, 0, readBuffer.size, AudioRecord.READ_NON_BLOCKING)
            
            if (readResult < 0) {
                Log.e(TAG, "AudioRecord read error: $readResult")
                _state.value = RecorderState.Error(RuntimeException("AudioRecord read error: $readResult"))
                return
            }
            
            if (readResult > 0) {
                // Create a copy of the buffer to avoid mutation issues
                val currentChunk = readBuffer.copyOf()
                
                // --- VAD Processing ---
                vadReadDataCount++
                if (vadReadDataCount >= config.vadTimerIntervalMs / config.listenerTimerIntervalMs) {
                    vad?.let {
                        val prevSpeechDetected = _isSpeechDetected.value
                        
                        // Run VAD on the current audio chunk
                        it.acceptWaveform(currentChunk)
                        val currentlySpeaking = it.isSpeechDetected()
                        
                        // Apply debouncing for false negatives
                        // If we were previously speaking, require multiple consecutive negative detections
                        if (prevSpeechDetected && !currentlySpeaking) {
                            // Count consecutive non-speech frames before changing state
                            speechSilenceCounter++
                            
                            // Only change state after enough silent frames (debouncing)
                            if (speechSilenceCounter >= SPEECH_SILENCE_DEBOUNCE_FRAMES) {
                                Log.d(TAG, "VAD state changed to silent after debounce: $currentlySpeaking")
                                _isSpeechDetected.value = false
                                speechSilenceCounter = 0
                            } else {
                                // Still in debounce period, maintain speaking state
                                Log.v(TAG, "VAD debouncing silence: $speechSilenceCounter/$SPEECH_SILENCE_DEBOUNCE_FRAMES")
                            }
                        } else if (!prevSpeechDetected && currentlySpeaking) {
                            // For transition to speaking, respond immediately
                            Log.d(TAG, "VAD state changed to speaking: $currentlySpeaking")
                            _isSpeechDetected.value = true
                            speechSilenceCounter = 0
                        } else if (currentlySpeaking) {
                            speechSilenceCounter = 0
                        } else {

                        }
                    }
                    vadReadDataCount = 0
                }
                
                // --- Data Accumulation ---
                // Send immediate update to listener flow
                recorderScope.launch {
                    _listenerAudioData.emit(currentChunk)
                }
                
                // Add to whisper buffer for larger chunks
                whisperBuffer.add(currentChunk)
                readDataCount++
                
                // Emit Whisper Buffer when enough data is collected
                if (readDataCount >= config.whisperTimerIntervalMs / config.listenerTimerIntervalMs || shouldStop.get()) {
                    if (whisperBuffer.isNotEmpty()) {
                        val combined = combineChunks(whisperBuffer)
                        recorderScope.launch {
                            _whisperAudioData.emit(combined)
                            Log.d(TAG, "Emitted whisper buffer: ${combined.size} samples")
                        }
                        whisperBuffer.clear()
                    }
                    readDataCount = 0
                    
                    if (shouldStop.get()) {
                        stop()
                    }
                }
            } else {
                Log.w(TAG, "Read 0 samples from AudioRecord")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from AudioRecord", e)
            _state.value = RecorderState.Error(e)
        } finally {
            isReading.set(false)
        }
    }

    suspend fun prepare() {
        if (_state.value !is RecorderState.Idle && _state.value !is RecorderState.Error) {
            Log.w(TAG, "Prepare called on invalid state: ${_state.value}")
            return
        }
        
        _state.value = RecorderState.Preparing
        _isSpeechDetected.value = false
        vad?.reset()
        
        withContext(Dispatchers.Unconfined) {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    throw SecurityException("RECORD_AUDIO permission not granted")
                }
                
                val minBufferSize = AudioRecord.getMinBufferSize(
                    config.sampleRate,
                    config.channelConfig,
                    config.audioFormat
                )
                
                // Calculate a good internal buffer size (at least 1 second of audio)
                val internalBufferSize = maxOf(
                    minBufferSize,
                    config.sampleRate * bytesPerFloat * 1 // 1 second buffer
                )
                
                Log.d(TAG, "Min buffer size: $minBufferSize bytes")
                Log.d(TAG, "Internal buffer size: $internalBufferSize bytes")
                
                audioRecord?.release()
                
                @SuppressLint("MissingPermission")
                val recorder = AudioRecord(
                    config.audioSource,
                    config.sampleRate,
                    config.channelConfig,
                    config.audioFormat,
                    internalBufferSize
                )
                
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord initialization failed: ${recorder.state}")
                }
                
                audioRecord = recorder
                
                // Reset counters and buffers
                readDataCount = 0
                vadReadDataCount = 0
                speechSilenceCounter = 0
                whisperBuffer.clear()
                shouldStop.set(false)
                
                _state.value = RecorderState.Ready
                Log.i(TAG, "AudioRecord prepared successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing AudioRecord", e)
                audioRecord?.release()
                audioRecord = null
                _state.value = RecorderState.Error(e)
            }
        }
    }

    fun start() {
        if (_state.value != RecorderState.Ready) {
            Log.e(TAG, "Start called on invalid state: ${_state.value}")
            if (_state.value != RecorderState.Recording) {
                _state.value = RecorderState.Error(IllegalStateException("Start called on invalid state: ${_state.value}"))
            }
            return
        }
        
        val recorder = audioRecord
        if (recorder == null) {
            _state.value = RecorderState.Error(IllegalStateException("AudioRecord not initialized. Call prepare() first."))
            return
        }
        
        try {
            // Set the notification period
            recorder.setRecordPositionUpdateListener(updateListener)
            recorder.positionNotificationPeriod = framePeriod
            
            // Initialize state
            readDataCount = 0
            vadReadDataCount = 0
            whisperBuffer.clear()
            shouldStop.set(false)
            
            // Start recording
            recorder.startRecording()
            _state.value = RecorderState.Recording
            Log.i(TAG, "Recording started with position notification every $framePeriod samples")
            
            // Trigger first read to start the process
            readDataFromBuffer()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord", e)
            _state.value = RecorderState.Error(e)
        }
    }

    fun stop() {
        if (_state.value != RecorderState.Recording && _state.value != RecorderState.Stopping) {
            Log.w(TAG, "Stop called on non-recording/stopping state: ${_state.value}")
            if (_state.value == RecorderState.Idle || _state.value == RecorderState.Ready) return
        }
        
        if (_state.value == RecorderState.Stopping) {
            Log.d(TAG, "Stop called while already stopping.")
            return
        }
        
        _state.value = RecorderState.Stopping
        Log.i(TAG, "Stopping recording...")
        
        try {
            val recorder = audioRecord ?: return
            
            // Signal for stopping
            shouldStop.set(true)
            
            // Remove listener
            recorder.setRecordPositionUpdateListener(null)
            
            // Stop recording
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
            
            // Emit any remaining data
            if (whisperBuffer.isNotEmpty()) {
                val combined = combineChunks(whisperBuffer)
                recorderScope.launch {
                    _whisperAudioData.emit(combined)
                    Log.d(TAG, "Emitted final whisper buffer: ${combined.size} samples")
                    
                    // Update state after final emission
                    _state.value = RecorderState.Ready
                    _isSpeechDetected.value = false
                }
                whisperBuffer.clear()
            } else {
                // Update state immediately if no data to emit
                _state.value = RecorderState.Ready
                _isSpeechDetected.value = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
            _state.value = RecorderState.Error(e)
        }
    }

    fun release() {
        Log.i(TAG, "Releasing S2TAudioRecorder resources.")
        
        // Ensure recording is stopped
        if (_state.value == RecorderState.Recording || _state.value == RecorderState.Stopping) {
            stop()
        }
        
        // Cancel the scope
        // recorderScope.cancel("Releasing S2TAudioRecorder")
        
        // Release the AudioRecord
        try {
            audioRecord?.release()
            Log.d(TAG, "AudioRecord released.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AudioRecord release", e)
        }
        
        audioRecord = null
        _state.value = RecorderState.Idle
        
        // Release VAD
        vad?.release()
        
        Log.i(TAG, "S2TAudioRecorder released.")
    }

    // Helper function to combine list of FloatArrays
    private fun combineChunks(chunks: List<FloatArray>): FloatArray {
        if (chunks.isEmpty()) return FloatArray(0)
        val totalSize = chunks.sumOf { it.size }
        val result = FloatArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.size)
            offset += chunk.size
        }
        return result
    }
}