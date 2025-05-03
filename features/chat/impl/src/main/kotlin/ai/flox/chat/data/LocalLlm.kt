package ai.flox.chat.data

import ai.flox.chat.model.ChatMessage
import ai.flox.network.NetworkResource
import ai.flox.network.openai.models.Choice
import ai.flox.network.openai.models.CompletionsResponse
import ai.flox.network.openai.models.Message
import ai.flox.network.openai.models.OpenAIRequest
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A basic offline LLM implementation that generates simple responses
 * for use when the app is in offline mode.
 */
@Singleton
class LocalLlm @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "LocalLLM"

    suspend fun completions(request: ChatMessage): NetworkResource<CompletionsResponse> {
        // Simulate network delay
        delay(800)
        Log.d(TAG, "Received request: $request in LocalLlm")
        
        val prompt = request.message.lowercase()
        val response = when {
            prompt.contains("hello") || prompt.contains("hi") -> 
                "Hello! I'm in offline mode right now. I have limited capabilities."
            
            prompt.contains("help") -> 
                "I'm running in offline mode with basic functionality. For full features, please switch to online mode."
            
            prompt.contains("weather") || prompt.contains("forecast") -> 
                "I can't check the weather while in offline mode."
            
            prompt.contains("time") || prompt.contains("date") -> 
                "It's currently ${Date(System.currentTimeMillis())}. Note that I'm running in offline mode."
            
            prompt.contains("offline") || prompt.contains("mode") -> 
                "Yes, I'm currently running in offline mode with limited capabilities."
            
            prompt.contains("how are you") || prompt.contains("feeling") -> 
                "I'm operating in offline mode, but I'm functioning well within my limitations. How are you?"
                
            prompt.length < 10 ->
                "I received your short message. I'm currently in offline mode with limited response capabilities."
                
            else -> 
                "I'm currently in offline mode with limited understanding capabilities. For more advanced conversations, please switch to online mode."
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
} 