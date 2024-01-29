package ai.flox.network.newsapi

import ai.flox.network.ApiService
import ai.flox.network.BuildConfig
import ai.flox.network.NetworkException
import ai.flox.network.NetworkResource
import ai.flox.network.newsapi.models.TopHeadlinesResponse
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

class NewsService @Inject constructor(
    private val newsAPI: NewsAPI
) : ApiService() {
    //TODO: Create BaseService with BaseFunctionality

    suspend fun headlines(): NetworkResource<out TopHeadlinesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val res = newsAPI.headlines(
                    apiKey = BuildConfig.NEWSAPI_KEY,
                    country ="us",
                    phrase = "AI",
                    category ="technology"
                )
                NetworkResource.Success(res.body(), null)
            } catch (ex: Exception) {
                // TODO: Handle all kinds of failures
                NetworkResource.Failure(NetworkException.fromException(ex))
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NewsAPIModule {

    @Singleton
    @Provides
    internal fun provideNewsAPI(
        okHttpClient: OkHttpClient
    ): NewsAPI {
        val retrofit =
            Retrofit.Builder()
                .baseUrl(NewsAPI.ENDPOINT)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
                .build()
        return retrofit.create(NewsAPI::class.java)
    }
}
