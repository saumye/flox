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
    fun setListener(listener: Whisper.WhisperListener?)
    suspend fun loadModel(modelPath: String, vocabPath: String, isMultilingual: Boolean): Boolean
    suspend fun loadModel(modelFile: File, vocabFile: File, isMultilingual: Boolean): Boolean
    fun unloadModel()
    fun setAction(action: Whisper.Companion.Action)
    fun setFilePath(wavFile: String)
    fun start()
    fun stop()
    fun isInProgress(): Boolean
    suspend fun transcribeBuffer(samples: FloatArray): Boolean
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
    private var wavFilePath: String? = null
    private var listener: WhisperListener? = null

    // File processing task - simplified using launch directly
    private var fileProcessingJob: Job? = null
    // Buffer processing task
    private var bufferProcessingJob: Job? = null

    init {
        Log.d(TAG, "Whisper Kotlin wrapper initialized.")
        // No background threads started here, engine initialization is separate
    }

    override fun setListener(listener: WhisperListener?) {
        this.listener = listener
    }

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

    override fun setFilePath(wavFile: String) {
        this.wavFilePath = wavFile
    }

    override fun start() {
        if (!isInProgressState.compareAndSet(expect = false, update = true)) {
            Log.w(TAG, "Transcription already in progress...")
            return
        }
        // Use the appropriate processing based on action/state?
        // Assuming file path means file transcription for now.
        if (wavFilePath != null) {
            startFileTranscription()
        } else {
             Log.e(TAG, "Start called without setting a file path.")
            // Or should start() always mean buffer transcription?
             // Clarify the intended use of start()
            isInProgressState.value = false // Reset if start is invalid
        }
    }

    // Internal function to launch file transcription job
    private fun startFileTranscription() {
        fileProcessingJob?.cancel() // Cancel previous job
        fileProcessingJob = launch { // Launch in the class's CoroutineScope
            sendUpdate(MSG_PROCESSING)
            val filePath = wavFilePath
            if (filePath == null || !File(filePath).exists()) {
                Log.e(TAG, "File path is null or file does not exist: $filePath")
                sendUpdate(MSG_FILE_NOT_FOUND)
                isInProgressState.value = false
                return@launch
            }

            val startTime = System.currentTimeMillis()
            val result: String? = try {
                when (action) {
                    Action.TRANSCRIBE -> whisperEngine.transcribeFile(filePath)
                    Action.TRANSLATE -> { Log.w(TAG,"Translate not implemented in engine"); null } // Handle translation if engine supports it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during file transcription", e)
                sendUpdate("Transcription failed: ${e.message}")
                null
            }
            val timeTaken = System.currentTimeMillis() - startTime
            Log.d(TAG, "File transcription time: ${timeTaken}ms")

            sendResult(result)
            sendUpdate(MSG_PROCESSING_DONE)
            isInProgressState.value = false // Mark as finished
        }
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

    // This method now directly calls the engine's suspend function
    // The old queue/loop mechanism is removed as S2TAudioRecorder controls the flow
    override suspend fun transcribeBuffer(samples: FloatArray) : Boolean {
         if (!whisperEngine.isInitialized()) {
             Log.e(TAG, "Cannot transcribe buffer, engine not initialized.")
             return false
         }
        // Call the suspend function in the engine
        // The listener callback will be triggered by the engine itself if needed,
        // or we rely on the result returned here.
         Log.v(TAG, "Passing ${samples.size} samples to engine's transcribeBuffer")
        val result = whisperEngine.transcribeBuffer(samples)
         if (result != null) {
             sendResult(result) // Send result back via listener
             return true
         } else {
             Log.e(TAG, "Engine transcribeBuffer returned null.")
             return false
         }
    }

    // Release engine resources when Whisper wrapper is no longer needed
    override fun release() {
        Log.d(TAG, "Releasing Whisper wrapper and engine.")
        stop() // Ensure jobs are stopped
        unloadModel() // Calls engine deinitialize
        job.cancel() // Cancel the CoroutineScope
        listener = null
    }

    // --- Listener Calls --- 
    private fun sendUpdate(message: String?) {
        message ?: return
        // Ensure listener calls happen on the main thread if they update UI
        launch(Dispatchers.Main) {
            listener?.onUpdateReceived(message)
        }
    }

    private fun sendResult(result: String?) {
         result ?: return
         launch(Dispatchers.Main) {
             listener?.onResultReceived(result)
         }
    }

    // --- Listener Interface --- (Keep as is)
    interface WhisperListener {
        fun onUpdateReceived(message: String)
        fun onResultReceived(result: String)
    }
} 