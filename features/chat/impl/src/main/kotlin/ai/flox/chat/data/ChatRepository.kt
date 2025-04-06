package ai.flox.chat.data

import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.SyncStatus
import ai.flox.conversation.model.Conversation
import ai.flox.network.NetworkResource
import ai.flox.network.openai.OpenAIService
import ai.flox.network.openai.models.OpenAIRequest
import ai.flox.state.Resource
import ai.flox.storage.chat.ChatDAO
import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val openAIService: OpenAIService,
    private val chatDAO: ChatDAO,
    @ApplicationContext private val application: Context
) {
    private val TAG: String = "ChatRepository"
    private lateinit var tts: OfflineTts
    private lateinit var track: AudioTrack
    private var stopped: Boolean = false
    private var mediaPlayer: MediaPlayer? = null

    init {
        initTts()
        initAudioTrack()
    }

    fun getChatMessages(conversation: Conversation): Flow<ChatAction> {
        return flow {
            emit(
                ChatAction.LoadMessages(
                    Resource.Success(chatDAO.getAll().map { it.toDomain(conversation) })
                )
            )
        }.flowOn(Dispatchers.IO).catch {
            emit(ChatAction.LoadMessages(Resource.Failure(Exception(it))))
        }
    }

    fun sendMessage(message: ChatMessage): Flow<ChatAction> {
        return flow {
            chatDAO.insertOrUpdate(message.toLocal(message.conversation))
            emit(
                ChatAction.CreateOrUpdateMessages(Resource.Success(message))
            )
            val response = openAIService.completions(OpenAIRequest.fromDomain(message.message))
            if (response is NetworkResource.Success) {
                response.data?.let {
                    onClickGenerate(it.toDomain(Date(System.currentTimeMillis()), message.conversation).message)
                    chatDAO.insertOrUpdate(it.toDomain(Date(System.currentTimeMillis()), message.conversation).toLocal(message.conversation))
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                message.copy(
                                    messageState = SyncStatus.COMPLETED
                                )
                            )
                        )
                    )
                    emit(
                        ChatAction.CreateOrUpdateMessages(
                            Resource.Success(
                                it.toDomain(Date(System.currentTimeMillis()), message.conversation)
                                    .copy(messageState = SyncStatus.COMPLETED)
                            )
                        )
                    )
                }
            } else if (response is NetworkResource.Failure) {
                emit(
                    ChatAction.CreateOrUpdateMessages(
                        Resource.Failure(
                            error = response.error,
                            data = message.copy(messageState = SyncStatus.FAILED_PERMANENTLY)
                        )
                    )
                )
            }
        }.flowOn(Dispatchers.IO).catch {
            emit(
                ChatAction.CreateOrUpdateMessages(
                    Resource.Failure(
                        Exception(it),
                        message.copy(messageState = SyncStatus.FAILED_PERMANENTLY)
                    )
                )
            )
        }
    }

    private fun initTts() {
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
        var assets: AssetManager? = application.assets
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

    // this function is called from C++
    private fun callback(samples: FloatArray): Int {
        if (!stopped) {
            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            return 1
        } else {
            track.stop()
            return 0
        }
    }

    private fun onClickGenerate(textStr: String) {
        track.pause()
        track.flush()
        track.play()
        stopped = false
        Thread {
            val audio = tts.generateWithCallback(
                text = textStr,
                sid = 0,
                speed = 1.0f,
                callback = this::callback
            )

            val filename = application.filesDir.absolutePath + "/generated.wav"
            val ok = audio.samples.size > 0 && audio.save(filename)
            if (ok) {
                MainScope().launch {
                    track.stop()
                }
            }
        }.start()
    }

    private fun copyDataDir(dataDir: String): String {
        Log.i(TAG, "data dir is $dataDir")
        copyAssets(dataDir)

        val newDataDir = application.getExternalFilesDir(null)!!.absolutePath
        Log.i(TAG, "newDataDir: $newDataDir")
        return newDataDir
    }

    private fun copyAssets(path: String) {
        val assets: Array<String>?
        try {
            assets = application.assets.list(path)
            if (assets!!.isEmpty()) {
                copyFile(path)
            } else {
                val fullPath = "${application.getExternalFilesDir(null)}/$path"
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
            val istream = application.assets.open(filename)
            val newFilename = application.getExternalFilesDir(null).toString() + "/" + filename
            val ostream = FileOutputStream(newFilename)
            // Log.i(TAG, "Copying $filename to $newFilename")
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

    private fun initAudioTrack() {
        val sampleRate = tts.sampleRate()
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        Log.i(TAG, "sampleRate: $sampleRate, buffLength: $bufLength")

        val attr = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        track = AudioTrack(
            attr, format, bufLength, AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.play()
    }

    private fun onClickPlay() {
        val filename = application.filesDir.absolutePath + "/generated.wav"
        mediaPlayer?.stop()
        mediaPlayer = MediaPlayer.create(
            application,
            Uri.fromFile(File(filename))
        )
        mediaPlayer?.start()
    }

    private fun onClickStop() {
        stopped = true
        track.pause()
        track.flush()
        mediaPlayer?.stop()
        mediaPlayer = null
    }
}