package ai.flox.storage.news

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "article")
data class NewsArticleEntity(
    @ColumnInfo(name ="title")
    val title: String,

    @ColumnInfo(name ="description")
    val description: String,

    @ColumnInfo(name ="url")
    val url: String,

    @ColumnInfo(name ="urlToImage")
    val urlToImage: String,

    @ColumnInfo(name ="source")
    val source: String,

    @ColumnInfo(name = "publishedAt")
    val publishedAt: Long,

    @PrimaryKey @ColumnInfo(name = "id")
    val id: String,
)