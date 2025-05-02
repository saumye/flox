package ai.flox.asr

import java.io.IOException

interface WhisperEngine {
    fun isInitialized(): Boolean
    // Consider making initialize suspend if it could be long-running
    @Throws(IOException::class)
    suspend fun initialize(modelPath: String, vocabPath: String, multilingual: Boolean): Boolean 
    fun deinitialize()
    suspend fun transcribeBuffer(samples: FloatArray): String?
} 