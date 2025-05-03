package ai.flox.asr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

// --- Interface Definition ---
interface IWhisper {
    suspend fun loadModel(modelPath: String, vocabPath: String, isMultilingual: Boolean): Boolean
    suspend fun loadModel(modelFile: File, vocabFile: File, isMultilingual: Boolean): Boolean
    fun unloadModel()
    fun setAction(action: Whisper.Companion.Action)
    fun start()
    fun stop()
    fun isInProgress(): Boolean
    suspend fun transcribeBuffer(samples: FloatArray): String?
    fun release()
}
// -------------------------

// Implement the Kotlin interface
class Whisper(
    private val context: Context,
    // Inject the engine for testability
    private val whisperEngine: WhisperEngine = WhisperEngineJava(context) // Default implementation
) : IWhisper, CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job // Use Default for CPU-bound tasks

    companion object {
        private const val TAG = "WhisperKotlin"
        const val MSG_PROCESSING = "Processing..."
        const val MSG_PROCESSING_DONE = "Processing done...!"
        const val MSG_FILE_NOT_FOUND = "Input file doesn't exist..!"

        // Use enum class for Actions
        enum class Action { TRANSLATE, TRANSCRIBE }
    }

    private val isInProgressState = MutableStateFlow(false)
    // private val audioBufferChannel = Channel<FloatArray>(Channel.UNLIMITED) // Channel for buffer processing

    private var action: Action = Action.TRANSCRIBE // Default action

    // File processing task - simplified using launch directly
    private var fileProcessingJob: Job? = null
    // Buffer processing task
    private var bufferProcessingJob: Job? = null

    init {
        Log.d(TAG, "Whisper Kotlin wrapper initialized.")
        // No background threads started here, engine initialization is separate
    }

    // override fun setListener(listener: WhisperListener?) { // Removed
    //     this.listener = listener
    // }

    // Make loadModel suspend as engine init is suspend
    override suspend fun loadModel(modelPath: String, vocabPath: String, isMultilingual: Boolean): Boolean {
        Log.d(TAG, "Loading model: $modelPath, Vocab: $vocabPath")
        return whisperEngine.initialize(modelPath, vocabPath, isMultilingual)
    }

    // Overload for File paths
    override suspend fun loadModel(modelFile: File, vocabFile: File, isMultilingual: Boolean): Boolean {
        return loadModel(modelFile.absolutePath, vocabFile.absolutePath, isMultilingual)
    }

    override fun unloadModel() {
        Log.d(TAG, "Unloading model.")
        whisperEngine.deinitialize()
    }

    override fun setAction(action: Action) {
        this.action = action
    }

    override fun start() {
        if (!isInProgressState.compareAndSet(expect = false, update = true)) {
            Log.w(TAG, "Transcription already in progress...")
            return
        }
        Log.e(TAG, "Start called without setting a file path.")
        // Or should start() always mean buffer transcription?
        // Clarify the intended use of start()
        isInProgressState.value = false // Reset if start is invalid
    }

    override fun stop() {
        Log.d(TAG, "Stop requested.")
        fileProcessingJob?.cancel("Stop called")
        // bufferProcessingJob?.cancel("Stop called") // Cancel buffer job if it exists
        // audioBufferChannel.close() // Close channel to stop buffer loop
        isInProgressState.value = false
    }

    override fun isInProgress(): Boolean {
        return isInProgressState.value
    }

    // This method now directly calls the engine's suspend function and returns the result
    // The old queue/loop mechanism is removed as S2TAudioRecorder controls the flow
    override suspend fun transcribeBuffer(samples: FloatArray) : String? { // Changed return type
         if (!whisperEngine.isInitialized()) {
             Log.e(TAG, "Cannot transcribe buffer, engine not initialized.")
             return null // Changed return
         }
        // Call the suspend function in the engine
         Log.v(TAG, "Passing ${samples.size} samples to engine's transcribeBuffer")
        val result = whisperEngine.transcribeBuffer(samples)
        Log.d(TAG,"Returned result from transcribeBuffer: $result")
         if (result != null) {
             // sendResult(result) // Removed listener call
             return result // Return result directly
         } else {
             Log.e(TAG, "Engine transcribeBuffer returned null.")
             return null // Changed return
         }
    }

    // Release engine resources when Whisper wrapper is no longer needed
    override fun release() {
        Log.d(TAG, "Releasing Whisper wrapper and engine.")
        stop() // Ensure jobs are stopped
        unloadModel() // Calls engine deinitialize
        job.cancel() // Cancel the CoroutineScope
        // listener = null // Removed
    }

    // --- Listener Calls --- (Removed)
    // private fun sendUpdate(message: String?) { ... }
    // private fun sendResult(result: String?) { ... }

    // --- Listener Interface --- (Removed)
    // interface WhisperListener { ... }
} 