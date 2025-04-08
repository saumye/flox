package ai.flox.chat.data

import ai.flox.asr.Recorder
import ai.flox.asr.WaveUtil
import ai.flox.asr.Whisper
import ai.flox.asr.Whisper.WhisperListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    private var mWhisper: Whisper? = null

    private var sdcardDataFolder: File? = null
    private var selectedWaveFile: File? = null
    private var selectedTfliteFile: File? = null

    private var startTime: Long = 0
    private val loopTesting = false
    private val transcriptionSync = SharedResource()

    init {
        // Call the method to copy specific file types from assets to data folder
        sdcardDataFolder = context.getExternalFilesDir(null)
        copyAssetsToSdcard(context, sdcardDataFolder, EXTENSIONS_TO_COPY)

        // Initialize default model to use
        selectedTfliteFile = File(sdcardDataFolder, DEFAULT_MODEL_TO_USE)
        selectedWaveFile = File(sdcardDataFolder, "MicInput.wav")
        // Audio recording functionality
        mRecorder = Recorder(context)
        mRecorder!!.setListener(object : Recorder.RecorderListener {
            override fun onUpdateReceived(message: String) {
                Log.d(TAG, "Update is received, Message: $message")

                if (message == Recorder.MSG_RECORDING) {

                } else if (message == Recorder.MSG_RECORDING_DONE) {

                }
            }

            override fun onDataReceived(samples: FloatArray?) {
            //                mWhisper.writeBuffer(samples);
            }
        })

        val isMultilingualModel = !(selectedTfliteFile!!.name.endsWith(ENGLISH_ONLY_MODEL_EXTENSION))
        val vocabFileName =
            if (isMultilingualModel) MULTILINGUAL_VOCAB_FILE else ENGLISH_ONLY_VOCAB_FILE
        val vocabFile = File(sdcardDataFolder, vocabFileName)

        mWhisper = Whisper(context)
        mWhisper!!.loadModel(selectedTfliteFile, vocabFile, isMultilingualModel)
    }

    fun isRecording() =  mRecorder?.isInProgress

    fun setListener(listener: WhisperListener?) {
        mWhisper?.setListener(listener)
    }

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