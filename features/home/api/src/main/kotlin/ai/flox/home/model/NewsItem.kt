package ai.flox.home.model

import java.util.Date
import java.util.UUID

data class NewsItem(
    val title: String,
    val description: String? = null,
    val source: String? = null,
    val url: String,
    val urlToImage: String? = null,
    val publishedAt: Date? = null,
    val id: String = UUID.randomUUID().toString(),
)