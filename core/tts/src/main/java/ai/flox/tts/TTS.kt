package ai.flox.tts

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// --- Interface Definition ---
interface ITts {
    val generatedAudio: SharedFlow<FloatArray>
    fun generate(text: String, sid: Int = 0, speed: Float = 1.0f)
    fun stop()
    fun release()
    // Add other necessary public methods if any
}
// -------------------------

class TTS(private val context: Context) : ITts {
    private val TAG: String = "TTS"
    private var tts: OfflineTts? = null
    private val ttsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var generationJob: Job? = null
    private val mutex = Mutex()
    private var stopped: Boolean = false

    private val _generatedAudio = MutableSharedFlow<FloatArray>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val generatedAudio: SharedFlow<FloatArray> = _generatedAudio.asSharedFlow()

    init {
        initTts()
    }

    private fun initTts() {
        ttsScope.launch {
            mutex.withLock {
                if (tts == null) {
                    var modelDir: String?
                    var modelName: String?
                    var acousticModelName: String?
                    var vocoder: String?
                    var voices: String?
                    var ruleFsts: String?
                    var ruleFars: String? = null
                    var lexicon: String?
                    var dataDir: String?
                    var dictDir: String?
                    var assets: AssetManager? = context.assets
                    acousticModelName = null
                    vocoder = null

                    modelDir = "kokoro-multi-lang-v1_0"
                    modelName = "kokoro.onnx"
                    voices = "voices.bin"
                    dataDir = "kokoro-multi-lang-v1_0/espeak-ng-data"
                    dictDir = "kokoro-multi-lang-v1_0/dict"
                    lexicon = "kokoro-multi-lang-v1_0/lexicon-us-en.txt,kokoro-multi-lang-v1_0/lexicon-zh.txt"
                    ruleFsts = "$modelDir/phone-zh.fst,$modelDir/date-zh.fst,$modelDir/number-zh.fst"

                    if (dataDir != null) {
                        val newDir = copyDataDir(dataDir!!)
                        dataDir = "$newDir/$dataDir"
                    }

                    if (dictDir != null) {
                        val newDir = copyDataDir(dictDir!!)
                        dictDir = "$newDir/$dictDir"
                        if (ruleFsts == null) {
                            ruleFsts = "$modelDir/phone.fst,$modelDir/date.fst,$modelDir/number.fst"
                        }
                    }

                    val config = getOfflineTtsConfig(
                        modelDir = modelDir!!,
                        modelName = modelName ?: "",
                        acousticModelName = acousticModelName ?: "",
                        vocoder = vocoder ?: "",
                        voices = voices ?: "",
                        lexicon = lexicon ?: "",
                        dataDir = dataDir ?: "",
                        dictDir = dictDir ?: "",
                        ruleFsts = ruleFsts ?: "",
                        ruleFars = ruleFars ?: "",
                    )!!

                    tts = OfflineTts(assetManager = assets, config = config)
                }
            }
        }
    }

    private fun callback(samples: FloatArray): Int {
        Log.d(TAG,"callback called with ${samples.size}")
        CoroutineScope(Dispatchers.IO).launch {
            _generatedAudio.emit(samples)
        }
        return 1
    }

    override fun stop() {
        generationJob?.cancel()
    }

    override fun generate(text: String, sid: Int, speed: Float) {
        generationJob = ttsScope.launch {
            mutex.withLock {
                val currentTts = tts
                if (currentTts == null) {
                    Log.e(TAG, "TTS not initialized, cannot generate.")
                    initTts()
                    return@launch
                }

                if (text.isBlank()) {
                    Log.w(TAG, "Skipping generation for blank text.")
                    return@launch
                }

                Log.d(TAG, "Starting TTS generation for: '$text'")
                try {
                    currentTts.generateWithCallback(
                        text = text,
                        sid = sid,
                        speed = speed,
                        callback = this@TTS::callback
                    )
                    Log.d(TAG, "Finished TTS generation call for: '$text'")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during TTS generation for '$text'", e)
                }
            }
        }
    }

    override fun release() {
        ttsScope.launch {
            mutex.withLock {
                Log.i(TAG, "Releasing TTS resources.")
                stop()
                tts?.release()
                tts = null
            }
        }
    }

    private fun copyDataDir(dataDir: String): String {
        Log.i(TAG, "data dir is $dataDir")
        copyAssets(dataDir)

        val newDataDir = context.getExternalFilesDir(null)!!.absolutePath
        Log.i(TAG, "newDataDir: $newDataDir")
        return newDataDir
    }

    private fun copyAssets(path: String) {
        val assets: Array<String>?
        try {
            assets = context.assets.list(path)
            if (assets!!.isEmpty()) {
                copyFile(path)
            } else {
                val fullPath = "${context.getExternalFilesDir(null)}/$path"
                val dir = File(fullPath)
                dir.mkdirs()
                for (asset in assets.iterator()) {
                    val p: String = if (path == "") "" else path + "/"
                    copyAssets(p + asset)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy $path. $ex")
        }
    }

    private fun copyFile(filename: String) {
        try {
            val istream = context.assets.open(filename)
            val newFilename = context.getExternalFilesDir(null).toString() + "/" + filename
            val ostream = FileOutputStream(newFilename)
            val buffer = ByteArray(1024)
            var read = 0
            while (read != -1) {
                ostream.write(buffer, 0, read)
                read = istream.read(buffer)
            }
            istream.close()
            ostream.flush()
            ostream.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to copy $filename, $ex")
        }
    }
}