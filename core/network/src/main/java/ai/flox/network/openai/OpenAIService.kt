package ai.flox.network.openai

import ai.flox.network.ApiService
import ai.flox.network.NetworkException
import ai.flox.network.NetworkResource
import ai.flox.network.openai.models.OpenAIRequest
import ai.flox.network.openai.models.CompletionsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OpenAIService @Inject constructor(
    private val openAIAPI: OpenAIAPI
) : ApiService() {
    //TODO: Create BaseService with BaseFunctionality

    suspend fun completions(request: OpenAIRequest): NetworkResource<out CompletionsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val res = openAIAPI.completions(request)
                NetworkResource.Success(res.body(), null)
            }catch (ex: Exception) {
                // TODO: Handle all kinds of failures
                NetworkResource.Failure(NetworkException.fromException(ex))
            }
        }
    }
}
