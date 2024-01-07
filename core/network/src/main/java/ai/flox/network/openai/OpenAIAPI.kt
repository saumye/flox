package ai.flox.network.openai

import ai.flox.network.openai.models.OpenAIRequest
import ai.flox.network.openai.models.CompletionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIAPI {
    @POST("completions")
    suspend fun completions(@Body request: OpenAIRequest) : Response<CompletionsResponse>

    companion object {
        private const val HOST = "api.openai.com/v1/chat/"
        const val ENDPOINT = "https://$HOST"
    }
}