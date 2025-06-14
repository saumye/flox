package ai.flox.network.newsapi.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopHeadlinesResponse(
    @field:Json(name = "articles")
    val articles: List<Article>,

    @field:Json(name = "totalArticles")
    val totalArticles: Int
)

@JsonClass(generateAdapter = true)
data class Article(
    @field:Json(name = "title")
    val title: String,

    @field:Json(name = "description")
    val description: String? = null,

    @field:Json(name = "content")
    val content: String? = null,

    @field:Json(name = "url")
    val url: String,

    @field:Json(name = "image")
    val image: String? = null,

    @field:Json(name = "source")
    val source: Source,

    @field:Json(name = "publishedAt")
    val publishedAt: String
)

@JsonClass(generateAdapter = true)
data class Source(
    @field:Json(name = "name")
    val name: String,

    @field:Json(name = "url")
    val url: String
)