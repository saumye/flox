package ai.flox.chat.data

import ai.flox.chat.model.ChatMessage
import ai.flox.network.NetworkResource
import ai.flox.network.openai.models.Choice
import ai.flox.network.openai.models.CompletionsResponse
import ai.flox.network.openai.models.Message
import ai.flox.network.openai.models.OpenAIRequest
import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.hilt.android.qualifiers.ApplicationContext
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A basic offline LLM implementation that generates simple responses
 * for use when the app is in offline mode.
 */
@Singleton
class LocalLlm @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "LocalLLM"
    private val modelFileName = "model.gguf"
    private val minP = 0.05f
    private val temperature = 1.0f
    private val smolLM = SmolLM()
    private lateinit var modelPath: String

    // Define sentence delimiters
    private val sentenceDelimiters = listOf('.', '!', '?')

    // Maximum number of sentences to collect
    private val maxSentences = 3

    init {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Create model file in app's internal storage
                val modelFile = File(context.filesDir, modelFileName)

                // If the model doesn't exist in internal storage, copy it from sdcard
                if (!modelFile.exists()) {
                    val sdcardModelFile = File("/sdcard/$modelFileName")
                    if (sdcardModelFile.exists()) {
                        copyModelToInternalStorage(sdcardModelFile, modelFile)
                        Log.d(
                            TAG,
                            "Model copied from SDCard to internal storage: ${modelFile.absolutePath}"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Model file not found in SDCard: ${sdcardModelFile.absolutePath}"
                        )
                    }
                }

                modelPath = modelFile.absolutePath

                // Initialize the model
                smolLM.close()
                smolLM.create(modelPath, minP, temperature, storeChats = true, contextSize = 0)
                Log.d(TAG, "Model initialized from: $modelPath")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing model", e)
            }
        }
    }

    private fun copyModelToInternalStorage(source: File, destination: File) {
        try {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy model file", e)
            throw e
        }
    }

    suspend fun completions(request: ChatMessage): NetworkResource<CompletionsResponse> {
        Log.d(TAG, "Received request: $request in LocalLlm")

        val response = try {
            // Use StringBuilder to accumulate tokens
            val responseBuilder = StringBuilder()
            var sentenceCount = 0

            smolLM.getResponse(request.message)
                .takeWhile { token ->
                    // Accumulate the token
                    responseBuilder.append(token)

                    // Get current text
                    val text = responseBuilder.toString()
                    text.replace('*',' ')
                    // Count sentences by checking delimiters
                    val currentSentenceCount = countSentences(text)

                    // Update sentence count
                    sentenceCount = currentSentenceCount

                    // Continue collecting if we haven't reached max sentences
                    sentenceCount < maxSentences
                }
                .collect { token ->
                    Log.d(TAG, "Token: $token")
                }

            // Get the text with up to maxSentences sentences
            val fullText = responseBuilder.toString().trim()
            fullText.replace('*',' ')
            val limitedText = if (sentenceCount > 0) {
                // Extract up to maxSentences sentences
                extractSentences(fullText, maxSentences)
            } else {
                // Use the complete text if no full sentence was found
                fullText
            }

            Log.d(TAG, "Generated response: $limitedText")
            limitedText
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            "I encountered an error while processing your request in offline mode."
        }

        // Create a response that mimics the OpenAI API response format
        val completionResponse = CompletionsResponse(
            id = "offline-${System.currentTimeMillis()}",
            model = "local",
            choices = listOf(
                Choice(
                    message = Message(response, ChatMessage.USER_ID_AI)
                )
            ),
            created = System.currentTimeMillis().toInt()
        )

        return NetworkResource.Success(completionResponse, null)
    }

    /**
     * Counts the number of complete sentences in the text.
     * A sentence is considered complete if it ends with one of the sentence delimiters.
     */
    private fun countSentences(text: String): Int {
        var count = 0
        var startIndex = 0

        while (startIndex < text.length) {
            // Find the next delimiter
            val nextDelimiterIndices = sentenceDelimiters.map {
                text.indexOf(it, startIndex)
            }.filter { it >= 0 }

            if (nextDelimiterIndices.isEmpty()) {
                // No more delimiters found
                break
            }

            // Move to the position after the delimiter
            startIndex = nextDelimiterIndices.minOrNull()!! + 1
            count++
        }

        return count
    }

    /**
     * Extracts up to the specified number of sentences from the text.
     */
    private fun extractSentences(text: String, maxCount: Int): String {
        var count = 0
        var lastIndex = 0

        for (i in text.indices) {
            if (i < text.length && sentenceDelimiters.contains(text[i])) {
                count++
                if (count >= maxCount) {
                    lastIndex = i + 1
                    break
                }
            }
        }

        return if (lastIndex > 0) {
            text.substring(0, lastIndex).trim()
        } else {
            text.trim()
        }
    }
} 