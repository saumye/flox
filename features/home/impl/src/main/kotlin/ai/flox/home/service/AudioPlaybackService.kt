package ai.flox.home.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.IOException
import ai.flox.home.util.StorageUtils

class AudioPlaybackService : Service() {

    private var storyPlayer: MediaPlayer? = null
    private var backgroundPlayer: MediaPlayer? = null
    private val binder = LocalBinder()

    companion object {
        private const val TAG = "AudioPlaybackService"
        const val ACTION_PLAY_STORY = "ai.flox.home.service.action.PLAY_STORY"
        const val ACTION_STOP_STORY = "ai.flox.home.service.action.STOP_STORY"
        const val ACTION_START_BACKGROUND = "ai.flox.home.service.action.START_BACKGROUND"
        const val ACTION_STOP_BACKGROUND = "ai.flox.home.service.action.STOP_BACKGROUND"
        const val EXTRA_NEWS_ID = "ai.flox.home.service.extra.NEWS_ID"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_STORY -> {
                val newsId = intent.getStringExtra(EXTRA_NEWS_ID)
                if (newsId != null) {
                    playStoryAudio(newsId)
                }
            }
            ACTION_STOP_STORY -> stopStoryAudio()
            ACTION_START_BACKGROUND -> startBackgroundMusic()
            ACTION_STOP_BACKGROUND -> stopBackgroundMusic()
        }
        return START_STICKY
    }

    private fun startBackgroundMusic() {
        if (backgroundPlayer == null) {
            try {
                //val assetFileDescriptor = assets.openFd("breaking_news_1.mp3")
                val assetFileDescriptor = assets.openFd("news_bg_reduced.mp3")
                backgroundPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                    isLooping = true
                    setVolume(0.5f, 0.5f) // Lower volume for background
                    prepareAsync()
                    setOnPreparedListener {
                        Log.d(TAG, "Background music prepared, starting.")
                        it.start()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Background music error: what: $what, extra: $extra")
                        true
                    }
                }
                assetFileDescriptor.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to load background music", e)
            }
        } else {
            if (!backgroundPlayer!!.isPlaying) {
                backgroundPlayer?.start()
            }
        }
    }

    private fun playStoryAudio(newsId: String) {
        backgroundPlayer?.setVolume(0.5f, 0.5f) // Duck background audio

        val audioFile = StorageUtils.getWavAudioFile(this, newsId)
        if (audioFile == null || !audioFile.exists()) {
            Log.e(TAG, "Audio file not found for newsId: $newsId at path: ${audioFile?.absolutePath}")
            resumeBackgroundMusic()
            return
        }

        stopStoryAudio() // Stop any currently playing story

        storyPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(audioFile.absolutePath)
                setVolume(0.9f, 0.9f)
                prepareAsync()
                setOnPreparedListener {
                    Log.d(TAG, "Story audio prepared, starting for newsId: $newsId")
                    it.start()
                }
                setOnCompletionListener {
                    Log.d(TAG, "Story audio completed for newsId: $newsId")
                    stopStoryAudio()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error playing story audio for $newsId: what $what, extra $extra")
                    stopStoryAudio()
                    true
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to set data source for story audio: $newsId", e)
                stopStoryAudio()
            }
        }
    }

    private fun stopStoryAudio() {
        storyPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        storyPlayer = null
        resumeBackgroundMusic()
    }

    private fun resumeBackgroundMusic() {
        backgroundPlayer?.setVolume(0.1f, 0.1f)
    }

    private fun stopBackgroundMusic() {
        backgroundPlayer?.stop()
        backgroundPlayer?.release()
        backgroundPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStoryAudio()
        stopBackgroundMusic()
        Log.d(TAG, "AudioPlaybackService destroyed")
    }
} 