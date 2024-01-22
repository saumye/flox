package ai.flox.home.model

import java.util.Date
import java.util.UUID

data class NewsItem(
    val title: String,
    val description: String,
    val source: String,
    val url: String,
    val urlToImage: String,
    val publishedAt: Date,
    val id: String = UUID.randomUUID().toString(),
)