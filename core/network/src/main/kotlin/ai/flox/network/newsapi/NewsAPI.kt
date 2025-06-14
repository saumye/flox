package ai.flox.network.newsapi

import ai.flox.network.newsapi.models.TopHeadlinesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsAPI {
    @GET("top-headlines")
    suspend fun headlines(
        @Query("apikey") apiKey: String,
        @Query("country") country: String = "us",
        @Query("category") category: String = "general",
        @Query("max") max: Int = 10,
        @Query("lang") lang: String = "en"
    ): Response<TopHeadlinesResponse>

    @GET("search")
    suspend fun search(
        @Query("apikey") apiKey: String,
        @Query("q") query: String,
        @Query("country") country: String = "us",
        @Query("max") max: Int = 10,
        @Query("lang") lang: String = "en",
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<TopHeadlinesResponse>

    companion object {
        private const val HOST = "gnews.io/api/v4/"
        const val ENDPOINT = "https://$HOST"
    }
}