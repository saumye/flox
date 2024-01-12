package ai.flox.storage.conversation

import ai.flox.storage.BaseDAO
import ai.flox.storage.conversation.model.ConversationEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface ConversationDAO : BaseDAO {
    @Query("SELECT * FROM conversation")
    suspend fun getAll(): List<ConversationEntity>

    @Insert
    suspend fun insertAll(vararg conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)
}