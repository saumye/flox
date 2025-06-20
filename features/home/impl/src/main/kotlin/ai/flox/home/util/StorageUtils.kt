package ai.flox.home.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object StorageUtils {
    private const val TAG = "StorageUtils"
    private const val AUDIO_DIR_NAME = "FloxAudio"

    fun getAudioDirectory(context: Context): File? {
        return try {
            // Get the external files directory for audio
            val audioDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                AUDIO_DIR_NAME
            )
            
            if (!audioDir.exists()) {
                if (!audioDir.mkdirs()) {
                    Log.e(TAG, "Failed to create audio directory")
                    return null
                }
            }
            
            audioDir
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio directory", e)
            null
        }
    }

    fun getAudioFile(context: Context, newsId: String): File? {
        return getAudioDirectory(context)?.let { dir ->
            File(dir, "${newsId}_description.mp3")
        }
    }

    fun getWavAudioFile(context: Context, newsId: String): File? {
        return getAudioDirectory(context)?.let { dir ->
            File(dir, "${newsId}_description.wav")
        }
    }

    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
} 