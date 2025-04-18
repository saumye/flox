package ai.flox.asr

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "MainScreenViewModel" //logging tag


class MainScreenViewModel(application: Application,
) : AndroidViewModel(application) {

//    private val _canTranscribe = MutableLiveData(false)
//    val canTranscribe: LiveData<Boolean> = _canTranscribe
//
//    private val _dataLog = MutableLiveData("")
//    val dataLog: LiveData<String> = _dataLog
//
//    private val _isRecording = MutableLiveData(false)
//    val isRecording: LiveData<Boolean> = _isRecording
//
//    private val _isStreaming = MutableLiveData(false)
//    val isStreaming: LiveData<Boolean> = _isStreaming
//
//    private val _processingTimeMessage = MutableLiveData("")
//    val processingTimeMessage: LiveData<String> = _processingTimeMessage
//
//
//    private val modelsPath = File(application.filesDir, "models")
//    private val samplesPath = File(application.filesDir, "samples")
//    private val recorder: RecorderV2 = RecorderV2()
//    private var whisperContext: WhisperContext? = null
//    private var mediaPlayer: MediaPlayer? = null
//
//    private var lastProcessedTimestamp: Long = 0 // Keep track of the last processed audio timestamp
//    private val audioState = AudioState()
//    private var MAX_AUDIO_SEC = 30
//    private var SAMPLE_RATE = 16000
//    private var streamingStartTime: Long = 0
//    private var totalProcessingTime: Long = 0
//    //16*1024 * seconds you want for a chunk
//    private val chunkSize = 16*1024*5
//
//    data class AudioState(
//        var isCapturing: Boolean = false,
//        var isTranscribing: Boolean = false,
//        var nSamples: Int = 0,
//        var audioBufferF32: MutableList<Float> = mutableListOf()
//    )
//
//    private val _transcriptionText = MutableLiveData<String>("")
//    val transcriptionText: LiveData<String> = _transcriptionText
//
//    // Function to process transcription - maybe put this into LibWhisper.kt???
//    @SuppressLint("RestrictedApi")
//
//    private fun startStreaming() {
//        if (_isStreaming.value != true) {
//            Log.d(TAG, "Starting streaming 2 electric boogaloo...")
//            _isStreaming.value = true
//
////            audioBuffer.clear()
//            audioState.isCapturing = true
//            audioState.audioBufferF32.clear()
//            audioState.nSamples = 0
//
//            lastProcessedTimestamp = System.currentTimeMillis() // Resetting the timestamp
//            streamingStartTime = System.currentTimeMillis()
//            // onDataReceived to handle buffering and processing audio data
//            val onDataReceived = object : RecorderV2.AudioDataReceivedListener {
//                override fun onAudioDataReceived(data: FloatArray) {
//                    // Add incoming data to the buffer
////                    audioBuffer.addAll(data.toList())
//                    if (!audioState.isCapturing) {
//                        Log.d(TAG, "Not capturing, ignoring audio")
//                        return
//                    }
//                    if (audioState.nSamples + data.size > MAX_AUDIO_SEC * SAMPLE_RATE) {
//                        Log.d(TAG, "Too much audio data, ignoring")
////                        toggleStream()
//                        //empty the buffer
//                        audioState.audioBufferF32.clear()
//                        audioState.nSamples = 0
//                        return
//                    }
//                    audioState.audioBufferF32.addAll(data.toList())
//                    audioState.nSamples += data.size
//                    // Process the buffer in chunks
//                    processBufferedAudioChunks()
//                }
//            }
//
//            // Start streaming with the onDataReceived listener
//            recorder.startStreaming(onDataReceived) { e ->
//                Log.e(TAG, "Error during streaming: ${e.localizedMessage}", e)
//                _isStreaming.postValue(false)
//            }
//        } else {
//            Log.i(TAG, "Streaming is already active.")
//        }
//    }
//    private fun processBufferedAudioChunks() {
//        if (audioState.isTranscribing) {
//            return
//        }
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                audioState.isTranscribing = true
//                while (audioState.audioBufferF32.size >= chunkSize) {
//                    val processingStartTime = System.currentTimeMillis()
//                    val chunkToProcess = audioState.audioBufferF32.take(chunkSize).toFloatArray()
//
//
//                    val textChunk = whisperContext?.streamTranscribeData(chunkToProcess) ?: ""
//                    Log.i(TAG, "Decoded Audio Chunk Text = $textChunk")
//                    val processingEndTime = System.currentTimeMillis()
//                    totalProcessingTime += (processingEndTime - processingStartTime)
//
//                    val recordingTime = (System.currentTimeMillis() - streamingStartTime) / 1000.0
//                    val processingTime = (processingEndTime - processingStartTime) / 1000.0
//                    withContext(Dispatchers.Main) {
//                        val currentText = _transcriptionText.value ?: ""
//                        _transcriptionText.value = currentText + textChunk
//                        val recordingTime = (System.currentTimeMillis() - streamingStartTime) / 1000.0
//                        val cumulativeProcessingTime = totalProcessingTime / 1000.0
//                        val realTimeFactor = cumulativeProcessingTime / recordingTime
//                        val timeInfo = "Recording time: ${"%.3f".format(recordingTime)} s, " +
//                                "Processing time: ${"%.3f".format(cumulativeProcessingTime)} s, " +
//                                "Real-time factor: ${"%.3f".format(realTimeFactor)}"
//                        Log.i(TAG,"$timeInfo")
//                        _processingTimeMessage.value = timeInfo
//                        Log.i(TAG, "Final Text: ${_transcriptionText.value}")
//                    }
//                    audioState.audioBufferF32 = audioState.audioBufferF32.drop(chunkSize).toMutableList()
////                        lastProcessedTimestamp = currentTimestamp // Update the last processed timestamp
//
////                    audioBuffer = audioBuffer.drop(chunkSize).toMutableList()
//                }
//                audioState.isTranscribing = false
//            } catch (e: Exception) {
//                Log.e(TAG, "Error during buffer processing: ${e.localizedMessage}", e)
//            }
//        }
//    }
//
//    fun toggleStream() = viewModelScope.launch {
//        if (_isStreaming.value == true) {
//            Log.d(TAG, "Stopping streaming...")
//            recorder.stopRecording()
//            _isStreaming.value = false
//            Log.d(TAG, "Streaming stopped")
//        } else {
//            Log.d(TAG, "Starting streaming...")
//            startStreaming()
//        }
//    }
//
//    override fun onCleared() {
//        runBlocking {
//            whisperContext?.release()
//            whisperContext = null
//        }
//    }
//    companion object {
//        fun factory() = viewModelFactory {
//            initializer {
//                val application =
//                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
//                MainScreenViewModel(application)
//            }
//        }
//    }

}


private suspend fun Context.copyData(
    assetDirName: String,
    destDir: File,
    printMessage: suspend (String) -> Unit
) = withContext(Dispatchers.IO) {
    assets.list(assetDirName)?.forEach { name ->
        val assetPath = "$assetDirName/$name"
        Log.v(TAG, "Processing $assetPath...")
        val destination = File(destDir, name)
        Log.v(TAG, "Copying $assetPath to $destination...")
        printMessage("Copying $name...\n")
        assets.open(assetPath).use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.v(TAG, "Copied $assetPath to $destination")
    }
}