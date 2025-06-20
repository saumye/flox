package ai.flox.home.worker

import ai.flox.home.data.NewsRepository
import ai.flox.home.model.HomeAction
import ai.flox.home.model.NewsCategory
import ai.flox.home.model.NewsItem
import ai.flox.home.util.StorageUtils
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.flox.state.Resource
import ai.flox.tts.TTS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

@HiltWorker
class NewsProcessingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val tts: TTS
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NewsProcessingWorker"
        const val WORK_NAME = "tts_audio_generation"
        private const val SAMPLE_RATE = 22050 // Sherpa-ONNX TTS models often use 22050Hz
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            for (category in NewsCategory.entries) {
                newsRepository.refreshNewsByCategory(category).first()
                Log.d(TAG, "Starting TTS audio generation work")

                if (!StorageUtils.isExternalStorageWritable()) {
                    Log.e(TAG, "External storage is not writable")
                    return@withContext Result.failure()
                }

                val newsItemsAction = newsRepository.getNewsByCategory(category).firstOrNull()

                if (newsItemsAction == null) {
                    Log.w(TAG, "No news items action received from repository. Worker finished.")
                    return@withContext Result.success()
                }

                if (newsItemsAction is HomeAction.LoadCategoryArticles && newsItemsAction.resource is Resource.Success<*>) {
                    val items = (newsItemsAction.resource as Resource.Success<List<NewsItem>>).data
                    if (items.isEmpty()) {
                        Log.d(TAG, "No news articles to process. Worker finished.")
                        return@withContext Result.success()
                    }

                    Log.d(TAG, "Retrieved ${items.size} news items")
                    items.forEach { newsItem ->
                        val audioFile = StorageUtils.getWavAudioFile(context, newsItem.id)
                        if (audioFile == null) {
                            Log.e(TAG, "Failed to get audio file for newsId: ${newsItem.id}")
                            return@forEach
                        }

                        if (audioFile.exists()) {
                            Log.d(
                                TAG,
                                "Audio file already exists for newsId: ${newsItem.id}, skipping"
                            )
                            return@forEach
                        }

                        Log.d(TAG, "Generating audio for newsId: ${newsItem.id}")
                        try {
                            val audioData =
                                tts.generate(newsItem.description ?: "", sid = 21, speed = 0.9f)
                            if (audioData != null && audioData.isNotEmpty()) {
                                Log.d(
                                    TAG,
                                    "Received audio data, size: ${audioData.size}. min: ${audioData.minOrNull()}, max: ${audioData.maxOrNull()}"
                                )
                                writeWavFile(audioData, audioFile)
                                Log.d(
                                    TAG,
                                    "Successfully generated and saved WAV audio for newsId: ${newsItem.id}"
                                )
                            } else {
                                Log.w(TAG, "Received no audio data for newsId: ${newsItem.id}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error generating audio for newsId: ${newsItem.id}", e)
                        }
                    }
                    Log.d(TAG, "TTS audio generation work completed successfully")
                } else {
                    Log.e(TAG, "Failed to load news items or action was not LoadArticles.")
                    return@withContext Result.failure()
                }
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in TTS audio generation work", e)
            return@withContext Result.failure()
        }
    }

    @Throws(IOException::class)
    private fun writeWavFile(audioData: FloatArray, file: File) {
        val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (sample in audioData) {
            val pcm = (sample * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            byteBuffer.putShort(pcm.toShort())
        }
        val pcmData = byteBuffer.array()

        FileOutputStream(file).use { out ->
            val totalDataLen = pcmData.size + 36
            val sampleRate = SAMPLE_RATE.toLong()
            val channels = CHANNELS
            val byteRate = (BITS_PER_SAMPLE * SAMPLE_RATE * channels / 8).toLong()

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] =
            'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte(); header[5] =
            (totalDataLen shr 8 and 0xff).toByte()
            header[6] = (totalDataLen shr 16 and 0xff).toByte(); header[7] =
            (totalDataLen shr 24 and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] =
            'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] =
            't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] =
            0 // Sub-chunk size (16 for PCM)
            header[20] = 1; header[21] = 0 // Audio format (1 for PCM)
            header[22] = channels.toByte(); header[23] = 0
            header[24] = (sampleRate and 0xff).toByte(); header[25] =
            (sampleRate shr 8 and 0xff).toByte()
            header[26] = (sampleRate shr 16 and 0xff).toByte(); header[27] =
            (sampleRate shr 24 and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte(); header[29] =
            (byteRate shr 8 and 0xff).toByte()
            header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] =
            (byteRate shr 24 and 0xff).toByte()
            header[32] = (channels * BITS_PER_SAMPLE / 8).toByte(); header[33] = 0 // Block align
            header[34] = BITS_PER_SAMPLE.toByte(); header[35] = 0
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] =
            't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (pcmData.size and 0xff).toByte(); header[41] =
            (pcmData.size shr 8 and 0xff).toByte()
            header[42] = (pcmData.size shr 16 and 0xff).toByte(); header[43] =
            (pcmData.size shr 24 and 0xff).toByte()

            out.write(header, 0, 44)
            out.write(pcmData)
        }
    }
}
