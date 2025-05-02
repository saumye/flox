package ai.flox.advanced

import ai.flox.asr.Whisper
import ai.flox.asr.Whisper.WhisperListener
import ai.liv.s2tlibrary.AudioRecorderConfig
import ai.liv.s2tlibrary.RecorderState
import ai.liv.s2tlibrary.S2TAudioRecorder
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.k2fsa.sherpa.onnx.getVadModelConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class S2T(private val context: Context) {

    private val TAG: String = "S2T"

    // whisper-tiny.tflite and whisper-base-nooptim.en.tflite works well
    private val DEFAULT_MODEL_TO_USE: String = "whisper-tiny.tflite"
    // English only model ends with extension ".en.tflite"
    private val ENGLISH_ONLY_MODEL_EXTENSION: String = ".en.tflite"
    private val ENGLISH_ONLY_VOCAB_FILE: String = "filters_vocab_en.bin"
    private val MULTILINGUAL_VOCAB_FILE: String = "filters_vocab_multilingual.bin"
    private val EXTENSIONS_TO_COPY: Array<String> = arrayOf("tflite", "bin", "wav", "pcm")
    private var s2tRecorder: S2TAudioRecorder? = null
    private var mWhisper: Whisper? = null
    private var mVad: Vad? = null

    // Expose the listener audio data flow
    val listenerAudioData: SharedFlow<FloatArray>?
        get() = s2tRecorder?.listenerAudioData

    // Expose the recorder state flow
    val recorderStateFlow: StateFlow<RecorderState>?
        get() = s2tRecorder?.state

    // Expose the VAD state flow
    val isSpeechDetectedFlow: StateFlow<Boolean>?
        get() = s2tRecorder?.isSpeechDetected

    private var sdcardDataFolder: File? = null

    // Coroutine scope for managing audio collection
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioCollectionJob: Job? = null
    private var selectedTfliteFile: File? = null

    init {
        // Call the method to copy specific file types from assets to data folder
        sdcardDataFolder = context.getExternalFilesDir(null)
        copyAssetsToSdcard(context, sdcardDataFolder, EXTENSIONS_TO_COPY)

        // Initialize default model to use
        selectedTfliteFile = File(sdcardDataFolder, DEFAULT_MODEL_TO_USE)

        val isMultilingualModel = true//!(selectedTfliteFile!!.name.endsWith(ENGLISH_ONLY_MODEL_EXTENSION))
        val vocabFileName =
            if (isMultilingualModel) MULTILINGUAL_VOCAB_FILE else ENGLISH_ONLY_VOCAB_FILE
        val vocabFile = File(sdcardDataFolder, vocabFileName)

        try {
            mWhisper = Whisper(context)
            // Launch model loading in the scope
            scope.launch {
                val sf = selectedTfliteFile
                val loaded = sf?.let { mWhisper?.loadModel(it, vocabFile, isMultilingualModel) }
                if (loaded == true) {
                    Log.i(TAG, "Whisper model loaded successfully.")
                } else {
                    Log.e(TAG, "Whisper model failed to load.")
                }
            }

            // VAD and Recorder init can proceed, but recorder usage might fail if model load fails
            val vadConfig = getVadModelConfig(0)
            if (vadConfig != null) {
                mVad = Vad(context.assets, vadConfig)
                Log.d(TAG, "VAD initialized successfully.")
            } else {
                Log.e(TAG, "Failed to get VAD model config!")
            }

            s2tRecorder = S2TAudioRecorder(context = context, vad = mVad)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing S2T components", e)
            mVad?.release()
            s2tRecorder?.release()
            mWhisper?.release() // Use release now
        }
    }

    fun startStreaming(listener: WhisperListener? = null) {
        scope.launch {
            val recorder = s2tRecorder ?: run {
                Log.e(TAG, "Recorder not initialized!")
                return@launch
            }

            if (recorder.state.value == RecorderState.Recording) {
                Log.w(TAG, "Streaming already active.")
                return@launch
            }

            if (recorder.state.value != RecorderState.Ready) {
                Log.d(TAG, "Preparing recorder...")
                recorder.prepare()
                try {
                    recorder.state
                        .filterIsInstance<RecorderState.Ready>()
                        .take(1)
                        .first()
                    Log.d(TAG, "Recorder is Ready.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error waiting for recorder to become ready", e)
                    recorder.state
                        .filterIsInstance<RecorderState.Error>()
                        .take(1)
                        .firstOrNull()?.let { errorState ->
                            Log.e(TAG, "Recorder preparation failed: ${errorState.exception}")
                        }
                    return@launch
                }
            }

            Log.d(TAG, "Starting streaming...")

            listener?.let { mWhisper?.setListener(it) }

            audioCollectionJob?.cancel()

            audioCollectionJob = scope.launch {
                Log.d(TAG, "Starting audio data collection for Whisper...")
                try {
                    recorder.whisperAudioData.collect { audioChunk ->
                        mWhisper?.transcribeBuffer(audioChunk)
                    }
                } catch (e: CancellationException) {
                    Log.i(TAG, "Whisper audio collection job cancelled.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting Whisper audio data", e)
                } finally {
                    Log.d(TAG, "Whisper audio collection finished.")
                }
            }

            recorder.start()

            recorder.state
                .filterIsInstance<RecorderState.Recording>()
                .take(1)
                .firstOrNull()?.let {
                    Log.i(TAG, "Recorder state confirmed: Recording")
                }
        }
    }

    fun stopStreaming() {
        val recorder = s2tRecorder ?: return
        if (recorder.state.value != RecorderState.Recording && recorder.state.value != RecorderState.Stopping) {
            Log.w(TAG, "Stop called but not in Recording state (${recorder.state.value}).")
        } else {
            Log.d(TAG, "Stopping streaming...")
            recorder.stop()
            audioCollectionJob?.cancel()
            audioCollectionJob = null
        }
    }

    fun release() {
        Log.d(TAG, "Releasing S2T resources...")
        stopStreaming()
        scope.cancel("S2T Released")
        s2tRecorder?.release()
        mWhisper?.release() // Use release instead of stop
        s2tRecorder = null
        mWhisper = null
        mVad = null
        Log.d(TAG, "S2T resources released.")
    }

    private fun copyAssetsToSdcard(context: Context, destFolder: File?, extensions: Array<String>) {
        val assetManager = context.assets

        try {
            val assetFiles = assetManager.list("") ?: return

            for (assetFileName in assetFiles) {
                for (extension in extensions) {
                    if (assetFileName.endsWith(".$extension")) {
                        val outFile = File(destFolder, assetFileName)

                        if (outFile.exists()) break

                        assetManager.open(assetFileName).use { inputStream ->
                            FileOutputStream(outFile).use { outputStream ->
                                val buffer = ByteArray(1024)
                                var bytesRead: Int
                                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                        break
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}