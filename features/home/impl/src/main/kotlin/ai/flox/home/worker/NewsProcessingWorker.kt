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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.icu.util.Calendar
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
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
import java.util.Random

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

        fun calculateRandomDelayUntilMorningCalendar(): Long {
            val random = Random()

            // Define the time window
            val startHourBoundary = 0 // 12 AM
            val endHourBoundary = 6   // Up to 6 AM (exclusive for hour generation)

            // Generate random hour, minute, second
            val randomHour = random.nextInt(endHourBoundary - startHourBoundary) // 0 to 5
            val randomMinute = random.nextInt(60) // 0 to 59
            val randomSecond = random.nextInt(60) // 0 to 59

            Log.v(TAG, "Generated random time: ${String.format("%02d:%02d:%02d", randomHour, randomMinute, randomSecond)}")

            // Get current time in the default timezone
            val nowCalendar = Calendar.getInstance()
            Log.v(TAG, "Current time: ${nowCalendar.time}")

            // Set up a Calendar object for the random time today
            val randomTimeCalendar = Calendar.getInstance()
            randomTimeCalendar.timeInMillis = nowCalendar.timeInMillis // Start with current date

            randomTimeCalendar.set(Calendar.HOUR_OF_DAY, randomHour)
            randomTimeCalendar.set(Calendar.MINUTE, randomMinute)
            randomTimeCalendar.set(Calendar.SECOND, randomSecond)
            randomTimeCalendar.set(Calendar.MILLISECOND, 0)

            // If the random time today has already passed, set it for tomorrow
            if (randomTimeCalendar.before(nowCalendar)) {
                randomTimeCalendar.add(Calendar.DAY_OF_YEAR, 1)
                Log.v(TAG, "Random time was in the past for today, moved to tomorrow: ${randomTimeCalendar.time}")
            } else {
                Log.v(TAG, "Random time is for today: ${randomTimeCalendar.time}")
            }

            val delayMillis = randomTimeCalendar.timeInMillis - nowCalendar.timeInMillis
            Log.v(TAG, "Calculated delay (ms): $delayMillis")
            return delayMillis
        }

//        fun calculateRandomDelayUntilMorning(): Duration {
//            val random = Random()
//
//            // 1. Define the time window (12 AM to 6 AM) using LocalTime
//            val startTimeWindow = LocalTime.MIDNIGHT // 00:00:00.000
//            val endTimeWindow = LocalTime(6, 0, 0)   // 06:00:00.000
//
//            // 2. Generate a random hour, minute, and second within this window
//            // Hour: 0 to 5 (inclusive)
//            // Joda-Time's LocalTime.getHourOfDay() is 0-23
//            val randomHour = startTimeWindow.hourOfDay + random.nextInt(endTimeWindow.hourOfDay - startTimeWindow.hourOfDay)
//            val randomMinute = random.nextInt(60) // 0 to 59
//            val randomSecond = random.nextInt(60) // 0 to 59
//
//            val randomTime = LocalTime(randomHour, randomMinute, randomSecond)
//            Log.v(TAG, "Generated random local time: $randomTime")
//
//            // 3. Get the current date and time in the system's default timezone
//            // DateTime.now() by default uses DateTimeZone.getDefault()
//            val now = DateTime.now(DateTimeZone.forTimeZone(TimeZone.getDefault()))
//            // You can be explicit if preferred: val now = DateTime.now(DateTimeZone.getDefault())
//            Log.v(TAG, "Current DateTime (default timezone ${now.zone}): $now")
//
//
//            // 4. Create a DateTime object for the random time on today's date,
//            //    respecting the timezone of 'now'
//            var randomDateTime = now.withTime(randomTime)
//
//            // 5. If the random time today has already passed (in the current timezone),
//            //    set it for tomorrow.
//            if (randomDateTime.isBefore(now)) {
//                randomDateTime = randomDateTime.plusDays(1)
//                Log.v(TAG, "Random DateTime was in the past for today, moved to tomorrow: $randomDateTime")
//            } else {
//                Log.v(TAG, "Random DateTime is for today: $randomDateTime")
//            }
//
//            // 6. Calculate the duration (delay)
//            val delay = Duration(now, randomDateTime)
//            Log.v(TAG, "Calculated Joda Duration: ${delay.standardSeconds} seconds")
//            return delay
//        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            setForeground(createForegroundInfo())
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

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        val id = TAG
        val title = "SmartScoop"
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "News Processing"
            val channel = NotificationChannel(id, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentText("Syncing News")
            .setOngoing(true)
            .build()

        return ForegroundInfo(0, notification)
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
