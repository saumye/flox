package ai.flox.advanced

import ai.flox.asr.Recorder
import ai.flox.asr.WaveUtil
import ai.flox.asr.Whisper
import ai.flox.asr.Whisper.WhisperListener
import ai.liv.s2tlibrary.S2TAudioRecorder
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class S2T(context: Context) {

    private val TAG: String = "S2T"

    // whisper-tiny.tflite and whisper-base-nooptim.en.tflite works well
    private val DEFAULT_MODEL_TO_USE: String = "whisper-tiny.tflite"
    // English only model ends with extension ".en.tflite"
    private val ENGLISH_ONLY_MODEL_EXTENSION: String = ".en.tflite"
    private val ENGLISH_ONLY_VOCAB_FILE: String = "filters_vocab_en.bin"
    private val MULTILINGUAL_VOCAB_FILE: String = "filters_vocab_multilingual.bin"
    private val EXTENSIONS_TO_COPY: Array<String> = arrayOf("tflite", "bin", "wav", "pcm")

    private var mRecorder: Recorder? = null
    var s2tRecorder: S2TAudioRecorder? = null
    private var mWhisper: Whisper? = null

    private var sdcardDataFolder: File? = null
    private var selectedWaveFile: File? = null
    private var selectedTfliteFile: File? = null

    private var startTime: Long = 0
    private val loopTesting = false
    private val transcriptionSync = SharedResource()


    private val _canTranscribe = MutableLiveData(false)
    val canTranscribe: LiveData<Boolean> = _canTranscribe

    private val _dataLog = MutableLiveData("")
    val dataLog: LiveData<String> = _dataLog

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _isStreaming = MutableLiveData(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    private val _processingTimeMessage = MutableLiveData("")
    val processingTimeMessage: LiveData<String> = _processingTimeMessage

    private val _transcriptionText = MutableLiveData("")
    val transcriptionText: LiveData<String> = _transcriptionText

    private var lastProcessedTimestamp: Long = 0 // Keep track of the last processed audio timestamp
    private val audioState = AudioState()
    private var MAX_AUDIO_SEC = 30
    private var SAMPLE_RATE = 16000
    private var streamingStartTime: Long = 0
    private var totalProcessingTime: Long = 0
    //16*1024 * seconds you want for a chunk
    private val chunkSize = 16*1024/2

    data class AudioState(
        var isCapturing: Boolean = false,
        var isTranscribing: Boolean = false,
        var nSamples: Int = 0,
        var audioBufferF32: MutableList<Float> = mutableListOf()
    )



    init {
        // Call the method to copy specific file types from assets to data folder
        sdcardDataFolder = context.getExternalFilesDir(null)
        copyAssetsToSdcard(context, sdcardDataFolder, EXTENSIONS_TO_COPY)

        // Initialize default model to use
        selectedTfliteFile = File(sdcardDataFolder, DEFAULT_MODEL_TO_USE)
        selectedWaveFile = File(sdcardDataFolder, "MicInput.wav")
        // Audio recording functionality
//        mRecorder = Recorder(context)
//        mRecorder!!.setListener(object : Recorder.RecorderListener {
//            override fun onUpdateReceived(message: String) {
//                Log.d(TAG, "Update is received, Message: $message")
//
//                if (message == Recorder.MSG_RECORDING) {
//
//                } else if (message == Recorder.MSG_RECORDING_DONE) {
//
//                }
//            }
//
//            override fun onDataReceived(samples: FloatArray?) {
//            //                mWhisper.writeBuffer(samples);
//            }
//        })

        val isMultilingualModel = !(selectedTfliteFile!!.name.endsWith(ENGLISH_ONLY_MODEL_EXTENSION))
        val vocabFileName =
            if (isMultilingualModel) MULTILINGUAL_VOCAB_FILE else ENGLISH_ONLY_VOCAB_FILE
        val vocabFile = File(sdcardDataFolder, vocabFileName)

        mWhisper = Whisper(context)
        mWhisper!!.loadModel(selectedTfliteFile, vocabFile, isMultilingualModel)
        mWhisper?.setListener(object: WhisperListener {
            override fun onUpdateReceived(message: String?) {
                Log.d(TAG, "WhisperListener.onUpdateReceived : $message")
            }

            override fun onResultReceived(result: String?) {
                Log.d(TAG, "WhisperListener.onResultReceived : $result")
            }

        })

        s2tRecorder = S2TAudioRecorder.getInstance(context, mWhisper)
    }

    fun isRecording() =  mRecorder?.isInProgress


    fun transcribe() {
        if (mRecorder != null && mRecorder!!.isInProgress) {
            Log.d(TAG, "Recording is in progress... stopping...")
            mRecorder!!.stop()
        }
        if (!mWhisper!!.isInProgress) {
            Log.d(TAG, "Start transcription...")
            mWhisper!!.setFilePath(selectedWaveFile!!.absolutePath)
            mWhisper!!.setAction(Whisper.ACTION_TRANSCRIBE)
            mWhisper!!.start()

            // only for loop testing
            if (loopTesting) {
                Thread {
                    for (i in 0..999) {
                        if (!mWhisper!!.isInProgress){
                            mWhisper!!.setFilePath(selectedWaveFile!!.absolutePath)
                            mWhisper!!.setAction(Whisper.ACTION_TRANSCRIBE)
                            mWhisper!!.start()
                        }
                        else Log.d(TAG, "Whisper is already in progress...!")

                        val wasNotified = transcriptionSync.waitForSignalWithTimeout(15000)
                        Log.d(
                            TAG,
                            if (wasNotified) "Transcription Notified...!" else "Transcription Timeout...!"
                        )
                    }
                }.start()
            }
        } else {
            Log.d(TAG, "Whisper is already in progress...!")
            mWhisper!!.stop()
        }
    }

    // Recording calls
    fun startRecording() {
        val waveFile = File(sdcardDataFolder, WaveUtil.RECORDING_FILE)
        mRecorder!!.setFilePath(waveFile.absolutePath)
        mRecorder!!.start()
    }

    fun startStreaming(listener: WhisperListener?) {
        if (_isStreaming.value != true) {
            Log.d(TAG, "Starting streaming 2 electric boogaloo...")
            _isStreaming.value = true

//            audioBuffer.clear()
            audioState.isCapturing = true
            audioState.audioBufferF32.clear()
            audioState.nSamples = 0

            lastProcessedTimestamp = System.currentTimeMillis() // Resetting the timestamp
            streamingStartTime = System.currentTimeMillis()
            // onDataReceived to handle buffering and processing audio data
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
//                        _isStreaming.postValue(false)
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

            mWhisper?.setListener(listener)

            s2tRecorder?.prepare()
            s2tRecorder?.start()
            // Start streaming with the onDataReceived listener
//            recorder.startStreaming(onDataReceived) { e ->
//                Log.e(TAG, "Error during streaming: ${e.localizedMessage}", e)
//                _isStreaming.postValue(false)
//            }
        } else {
            _isStreaming.postValue(false)
            //recorder.stopRecording()
            s2tRecorder?.stop()
            s2tRecorder?.reset(false)
            Log.i(TAG, "Streaming is already active.")
        }
    }

//    private fun processBufferedAudioChunks() {
//        if (audioState.isTranscribing) {
//            return
//        }
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                audioState.isTranscribing = true
//                while (audioState.audioBufferF32.size >= chunkSize) {
//                    val processingStartTime = System.currentTimeMillis()
//                    val chunkToProcess = audioState.audioBufferF32.take(chunkSize).toFloatArray()
//
//                    val textChunk = mWhisper?.transcribeBuffer(chunkToProcess) ?: ""
//                    Log.i(TAG, "Audio Chunk = ${chunkToProcess.toList()}")
//                    val processingEndTime = System.currentTimeMillis()
//                    totalProcessingTime += (processingEndTime - processingStartTime)
//
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



    // Copy assets with specified extensions to destination folder
    private fun copyAssetsToSdcard(context: Context, destFolder: File?, extensions: Array<String>) {
        val assetManager = context.assets

        try {
            // List all files in the assets folder once
            val assetFiles = assetManager.list("") ?: return

            for (assetFileName in assetFiles) {
                // Check if file matches any of the provided extensions
                for (extension in extensions) {
                    if (assetFileName.endsWith(".$extension")) {
                        val outFile = File(destFolder, assetFileName)

                        // Skip if file already exists
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
                        break // No need to check further extensions
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class SharedResource {
    // Synchronized method for Thread 1 to wait for a signal with a timeout
    @Synchronized
    fun waitForSignalWithTimeout(timeoutMillis: Long): Boolean {
        val startTime = System.currentTimeMillis()

        try {
            (this as Object).wait(timeoutMillis) // Wait for the given timeout
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // Restore interrupt status
            return false // Thread interruption as timeout
        }

        val elapsedTime = System.currentTimeMillis() - startTime

        // Check if wait returned due to notify or timeout
        return if (elapsedTime < timeoutMillis) {
            true // Returned due to notify
        } else {
            false // Returned due to timeout
        }
    }

    // Synchronized method for Thread 2 to send a signal
    @Synchronized
    fun sendSignal() {
        (this as Object).notify() // Notifies the waiting thread
    }
}