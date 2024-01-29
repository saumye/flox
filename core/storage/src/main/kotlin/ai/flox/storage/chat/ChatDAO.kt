package ai.flox.storage.chat

import ai.flox.storage.BaseDAO
import ai.flox.storage.chat.model.ChatMessageEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDAO : BaseDAO {
    @Query("SELECT * FROM chatmessage")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(vararg message: ChatMessageEntity)
}