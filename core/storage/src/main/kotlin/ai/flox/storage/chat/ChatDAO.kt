package ai.flox.storage.chat

import ai.flox.storage.BaseDAO
import ai.flox.storage.chat.model.ChatMessageEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDAO : BaseDAO {
    @Query("SELECT * FROM chatmessage")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert
    suspend fun insertAll(vararg message: ChatMessageEntity)

    @Delete
    suspend fun delete(message: ChatMessageEntity)
}