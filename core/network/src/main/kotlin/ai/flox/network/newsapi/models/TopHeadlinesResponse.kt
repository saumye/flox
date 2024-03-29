package ai.flox.network.newsapi.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopHeadlinesResponse(
    @field:Json(name = "articles")
    val articles: List<Article>,

    @field:Json(name = "status")
    val status: String,

    @field:Json(name = "totalResults")
    val results: Int
)

@JsonClass(generateAdapter = true)
data class Article(
    @field:Json(name = "title")
    val title: String,

    @field:Json(name = "description")
    val description: String? = null,

    @field:Json(name = "url")
    val url: String,

    @field:Json(name = "urlToImage")
    val urlToImage: String? = null,

    @field:Json(name = "source")
    val source: Source? = null,

    @field:Json(name = "publishedAt")
    val publishedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Source(
    @field:Json(name = "id")
    val id: String? = null,

    @field:Json(name = "name")
    val name: String? = null
)