package ai.flox.home.data

import ai.flox.home.model.NewsItem
import ai.flox.network.newsapi.models.Article
import ai.flox.storage.news.NewsArticleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val dateConvertor = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)

fun Article.toDomain() = NewsItem(
    title = this.title, description = this.description, source = this.source.name,
    url = this.url, urlToImage = this.urlToImage, publishedAt = dateConvertor.parse(publishedAt) ?: Date(System.currentTimeMillis())
)

fun NewsArticleEntity.toDomain() = NewsItem(
    title = this.title, description = this.description, source = this.source,
    url = this.url, urlToImage = this.urlToImage, publishedAt = Date(publishedAt)
)

fun NewsItem.toLocal() = NewsArticleEntity(
    title = this.title, description = this.description, source = this.source, publishedAt = publishedAt.time,
    url = this.url, urlToImage = this.urlToImage, id = UUID.randomUUID().toString()
)