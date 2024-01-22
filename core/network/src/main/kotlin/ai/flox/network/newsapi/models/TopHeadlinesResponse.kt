package ai.flox.network.newsapi.models

import com.google.gson.annotations.SerializedName

data class TopHeadlinesResponse(
    @SerializedName("articles")
    val articles: List<Article>,

    @SerializedName("status")
    val status: String,

    @SerializedName("totalResults")
    val results: Int
)

data class Article(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("urlToImage")
    val urlToImage: String,

    @SerializedName("source")
    val source: Source,

    @SerializedName("publishedAt")
    val publishedAt: String
)

data class Source(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String
)