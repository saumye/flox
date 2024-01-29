package ai.flox.storage.news

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "article", primaryKeys = ["id", "url"])
data class NewsArticleEntity(
    @ColumnInfo(name ="title")
    val title: String,

    @ColumnInfo(name ="description")
    val description: String? = null,

    @ColumnInfo(name ="url")
    val url: String,

    @ColumnInfo(name ="urlToImage")
    val urlToImage: String? = null,

    @ColumnInfo(name ="source")
    val source: String? = null,

    @ColumnInfo(name = "publishedAt")
    val publishedAt: Long? = null,

    @ColumnInfo(name = "id")
    val id: String,
)