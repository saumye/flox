package ai.flox.network.newsapi

import ai.flox.network.newsapi.models.TopHeadlinesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsAPI {
    @GET("top-headlines")
    suspend fun headlines(
        @Query("apiKey") apiKey: String,
        @Query("country") country: String,
        @Query("q") phrase: String,
        @Query("category") category: String
    ): Response<TopHeadlinesResponse>

    companion object {
        private const val HOST = "newsapi.org/v2/"
        const val ENDPOINT = "https://$HOST"
    }
}