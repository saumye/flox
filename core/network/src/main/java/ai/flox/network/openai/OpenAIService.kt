package ai.flox.network.openai

import ai.flox.network.ApiService
import ai.flox.network.NetworkException
import ai.flox.network.NetworkResource
import ai.flox.network.openai.models.OpenAIRequest
import ai.flox.network.openai.models.CompletionsResponse
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

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

@Module
@InstallIn(SingletonComponent::class)
object OpenAIModule {

    @Singleton
    @Provides
    internal fun provideOpenAIAPI(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): OpenAIAPI {
        val retrofit =
            Retrofit.Builder()
                .baseUrl(OpenAIAPI.ENDPOINT)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        return retrofit.create(OpenAIAPI::class.java)
    }
}
