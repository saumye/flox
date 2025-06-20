package ai.flox.storage.news

import ai.flox.storage.BaseDAO
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NewsDAO : BaseDAO {
    @Query("SELECT * FROM article WHERE category = :category ORDER BY publishedAt DESC")
    suspend fun getByCategory(category: String): List<NewsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(vararg news: NewsArticleEntity)
}
